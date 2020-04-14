package com.example.learnglide;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.LruCache;


import com.example.learnglide.disk.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * 创建者:qinyafei
 * 创建时间: 2019/6/20
 * 描述:
 * 版本信息：1.0.0
 **/
public class MyImageCache implements IImgaeCache {


    private static final int CACHE_SIZE = 10 * 1024 * 1024;

    private static MyImageCache instance;
    private Context context;
    private LruCache<String, Bitmap> memoryCache;
    private DiskLruCache diskLruCache;
    private BitmapFactory.Options options = new BitmapFactory.Options();


    public static Set<WeakReference<Bitmap>> reuseablePool;// 复用池
    ReferenceQueue referenceQueue;
    Thread gcThread;// 手动回收线程
    boolean shutDown;

    private MyImageCache(Context context) {
        this.context = context.getApplicationContext();// 为避免内存泄漏
    }

    public static MyImageCache getInstance(Context context) {

        if (null == instance) {
            synchronized (MyImageCache.class) {
                if (null == instance) {
                    instance = new MyImageCache(context);
                }
            }
        }

        return instance;
    }

    @Override
    public void init(String dir) {

        // 构建一个线程安全的复用池，考虑到图片加载多在子线程中进行
        reuseablePool = Collections.synchronizedSet(new HashSet<WeakReference<Bitmap>>());
        intiReferenceQueue();
        memoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
            /**
             * @return 图片占用内存大小
             */
            @Override
            protected int sizeOf(String key, Bitmap value) {
                //19之前必需同等大小，才能复用  inSampleSize=1
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    return value.getAllocationByteCount();
                }
                return value.getByteCount();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue.isMutable()) {// 如果是可服用的内存

                    reuseablePool.add(new WeakReference<Bitmap>(oldValue, referenceQueue));
                } else {
                    oldValue.recycle();
                }
            }
        };
        try {
            diskLruCache = DiskLruCache.open(new File(dir), BuildConfig.VERSION_CODE, 1, 10 * 1024 * 1024);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 初始化引用队列 && 回收线程
     */
    private void intiReferenceQueue() {
        if (null == referenceQueue) {
            referenceQueue = new ReferenceQueue<Bitmap>();
            //开启一个回收线程，在检测到有可回收对象时，
            // 手动回收，避免了GC的等待时间，让内存处理更高效
            gcThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!shutDown) {
                        try {
                            //remove是阻塞式的
                            Reference<Bitmap> reference = referenceQueue.remove();
                            Bitmap bitmap = reference.get();
                            if (null != bitmap && !bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            gcThread.start();
        }
    }

    @Override
    public void putSrc2MemoryCache(String key, Bitmap bitmap) {
        memoryCache.put(key, bitmap);
    }

    @Override
    public Bitmap getBitmapFromMemory(String key) {
        return memoryCache.get(key);
    }

    @Override
    public Bitmap getSrcFromReuseableBimtmapPool(int width, int height, int simpleSize) {

        // api 11 之前是不存在Bitmap服用的
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            Bitmap reuseable = null;
            Iterator<WeakReference<Bitmap>> iterator = reuseablePool.iterator();
            while (iterator.hasNext()) {
                Bitmap bitmap = iterator.next().get();
                if (null != bitmap) {
                    if (isBitmapCanResueable(bitmap, width, height, simpleSize)) {
                        reuseable = bitmap;
                        iterator.remove();
                        break;
                    } else {
                        iterator.remove();
                    }
                }
            }
            return reuseable;
        }
        return null;
    }

    @Override
    public void putBitmap2DiskCache(String key, Bitmap bitmap) {
        DiskLruCache.Snapshot snapshot = null;
        OutputStream os = null;
        try {
            snapshot = diskLruCache.get(key);
            //如果缓存中已经有这个文件  不理他
            if (null == snapshot) {
                //如果没有这个文件，就生成这个文件
                DiskLruCache.Editor editor = diskLruCache.edit(key);
                if (null != editor) {
                    os = editor.newOutputStream(0);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, os);
                    editor.commit();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != snapshot) {
                snapshot.close();
            }
            if (null != os) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Bitmap getSrcFromDiskCache(String key, Bitmap reuseable) {
        DiskLruCache.Snapshot snapshot = null;
        Bitmap bitmap = null;
        try {
            snapshot = diskLruCache.get(key);
            if (null == snapshot) {
                return null;
            }
            InputStream is = snapshot.getInputStream(0);
            options.inMutable = true;
            options.inBitmap = reuseable;
            bitmap = BitmapFactory.decodeStream(is, null, options);
            if (null != bitmap) {
                memoryCache.put(key, bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != snapshot) {
                snapshot.close();
            }
        }
        return bitmap;
    }

    @Override
    public void clearCache() {
        memoryCache.evictAll();
    }


    /**
     * 检查图片是否可以进行复用
     *
     * @param bitmap
     * @param w
     * @param h
     * @param inSampleSize
     * @return
     */
    private boolean isBitmapCanResueable(Bitmap bitmap, int w, int h, int inSampleSize) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return bitmap.getWidth() == w && bitmap.getHeight() == h && inSampleSize == 1;
        }
        if (inSampleSize >= 1) {
            w /= inSampleSize;
            h /= inSampleSize;
        }
        int byteCount = w * h * getPixelsCount(bitmap.getConfig());
        return byteCount <= bitmap.getAllocationByteCount();
    }

    /**
     * 获取每个像素的字节数目
     *
     * @param config
     * @return
     */
    private int getPixelsCount(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        }
        return 2;
    }
}
