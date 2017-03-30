package com.lib.media.listener;

import android.graphics.Bitmap;

/**
 * author: heshantao
 * data: 2017/3/21.
 * 媒体控制界面操作
 */

public interface IMediaViewController {
    //滑动改变改变音量
    public void slideVolume(float deltaY);

    //滑动改变亮度
    public void slideLight(float deltaY);

    //滑动改变进度
    public void slideProgress(float deltaX);

}
