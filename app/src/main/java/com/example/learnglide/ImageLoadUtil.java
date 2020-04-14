package com.example.learnglide;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class ImageLoadUtil {


    /**
     * 加载图片
     * @param context
     * @param key
     * @param width
     * @param height
     * @param simpleSize 缩放比例
     * @return
     */
    public static Bitmap loadImage(Context context, String key, int width, int height, int simpleSize) {
        Bitmap bitmap = MyImageCache.getInstance(context).getBitmapFromMemory(key);// 从内存中找
        if (null == bitmap) {
            Bitmap reuseable = MyImageCache.getInstance(context).getSrcFromReuseableBimtmapPool(width, height, simpleSize);
            bitmap = MyImageCache.getInstance(context).getSrcFromDiskCache(key, reuseable);
            if (null == bitmap) {  //如果磁盘中也没缓存,就从网络下载,这里使用本地图片模拟网络图片
                Log.e("Img_Test", "从网络加载: ");
                bitmap = resizeBitmap(context, R.drawable.test, 80, 80, false, reuseable);
                MyImageCache.getInstance(context).putBitmap2DiskCache(key, bitmap);
                MyImageCache.getInstance(context).putSrc2MemoryCache(key, bitmap);
            }else {
                Log.e("Img_Test", "从磁盘加载: ");
            }
        }else {
            Log.e("Img_Test", "从内存加载: ");
        }
        return bitmap;
    }


    /**
     * 缩放bitmap
     *
     * @param context
     * @param id
     * @param maxW
     * @param maxH
     * @return
     */
    public static Bitmap resizeBitmap(Context context, int id, int maxW, int maxH, boolean hasAlpha, Bitmap reusable) {
        Resources resources = context.getResources();
        BitmapFactory.Options options = new BitmapFactory.Options();
        // 只解码出 outxxx参数 比如 宽、高
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, id, options);
        //根据宽、高进行缩放
        int w = options.outWidth;
        int h = options.outHeight;
        //设置缩放系数
        options.inSampleSize = calcuteInSampleSize(w, h, maxW, maxH);
        if (!hasAlpha) {
            options.inPreferredConfig = Bitmap.Config.RGB_565;
        }
        options.inJustDecodeBounds = false;
        //设置成能复用
        options.inMutable = true;
        options.inBitmap = reusable;
        return BitmapFactory.decodeResource(resources, id, options);
    }

    /**
     * 计算缩放系数
     *
     * @param w
     * @param h
     * @param maxW
     * @param maxH
     * @return 缩放的系数
     */
    private static int calcuteInSampleSize(int w, int h, int maxW, int maxH) {
        int inSampleSize = 1;
        if (w > maxW && h > maxH) {
            inSampleSize = 2;
            //循环 使宽、高小于 最大的宽、高
            while (w / inSampleSize > maxW && h / inSampleSize > maxH) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
