package com.example.learnglide;

import android.graphics.Bitmap;

/**
 * 创建者:qinyafei
 * 创建时间: 2019/6/20
 * 描述: 图片缓存相关接口
 * 版本信息：1.0.0
 **/
public interface IImgaeCache {

    /**
     * 初始化方法，因为要做磁盘缓存，所以我们在这里传入文件路径
     *
     * @param dir
     */
    void init(String dir);

    /**
     * 将图片放入内存缓存
     *
     * @param key
     * @param bitmap
     */
    void putSrc2MemoryCache(String key, Bitmap bitmap);


    /**
     * 从内存缓存中取得图片
     *
     * @param key
     * @return
     */
    Bitmap getBitmapFromMemory(String key);


    /**
     * 从复用池中获取图片数据
     *
     * @param width      所需图片宽度
     * @param height     所需图片高度
     * @param simpleSize 缩放大小
     * @return
     */
    Bitmap getSrcFromReuseableBimtmapPool(int width, int height, int simpleSize);


    /**
     * 将图片数据放入磁盘缓存
     * @param key
     * @param bitmap
     */
    void putBitmap2DiskCache(String key, Bitmap bitmap);

    /**
     * 从磁盘中获取图片，要考虑复用问题
     * @param reuseable
     * @return
     */
    Bitmap getSrcFromDiskCache(String key,Bitmap reuseable);


    /**
     * 清除缓存
     */
    void clearCache();
}
