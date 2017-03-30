package com.lib.media.util;

import android.app.Activity;
import android.provider.Settings;

import com.lib.media.controller.BaseVideoController;
import com.lib.media.controller.VideoMediaController;

/**
 * author: heshantao
 * data: 2017/3/29.
 * 亮度工具
 */

public class LightUtil {

    private Activity mActivity;
    private VideoMediaController mController;

    public LightUtil(Activity activity, VideoMediaController controller) {
        this.mActivity = activity;
        this.mController = controller;
    }

    //初始化屏幕亮度
    public void initLight() {
        try {
            int e = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            float progress = 1.0F * (float) e / 255.0F;
            android.view.WindowManager.LayoutParams layout = this.mActivity.getWindow().getAttributes();
            layout.screenBrightness = progress;
            mActivity.getWindow().setAttributes(layout);
        } catch (Settings.SettingNotFoundException var7) {
            var7.printStackTrace();
        }
    }

    public void setControllerLight() {
        android.view.WindowManager.LayoutParams layout = this.mActivity.getWindow().getAttributes();
        float mBrightness = layout.screenBrightness;
        if (mBrightness <= 0.00f) {
            mBrightness = 0.50f;
        } else if (mBrightness < 0.01f) {
            mBrightness = 0.01f;
        }
        mController.setLight(mBrightness);
    }

    public void setLight(float mBrightness) {
        android.view.WindowManager.LayoutParams layout = this.mActivity.getWindow().getAttributes();
        layout.screenBrightness = mBrightness;
        this.mActivity.getWindow().setAttributes(layout);

    }


}
