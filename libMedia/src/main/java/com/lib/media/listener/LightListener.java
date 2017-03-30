package com.lib.media.listener;

/**
 * author: heshantao
 * data: 2017/3/29.
 */

public interface LightListener {
    public void initWindowLight();

    //获得当前屏幕亮度
    public void getWindowLight();

    public void setWindowLight(float light);
}
