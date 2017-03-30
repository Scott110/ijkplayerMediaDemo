package com.lib.media.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;

import com.lib.media.controller.BaseVideoController;
import com.lib.media.controller.VideoMediaController;

/**
 * author: heshantao
 * data: 2017/2/16.
 * 屏幕方向控制工具类
 */

public class OrientationUtil {
    private static final String TAG = "OrientationUtil";
    private Activity mActivity;
    private VideoMediaController mController;
    private OrientationEventListener mOrientationEventListener;

    public OrientationUtil(Activity activity, VideoMediaController controller) {
        this.mActivity = activity;
        this.mController = controller;
        init();

    }

    public void init() {
        int orientation;
        if (mController.getIsOnlyFullScreen()) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        setScreenOrientation(orientation);

        //设置中禁止横竖屏切换监听
        mOrientationEventListener = new OrientationEventListener(mActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                setOritentionAutoRotate();
                if (orientation >= 0 && orientation <= 30 || orientation >= 330 || (orientation >= 150 && orientation <= 210)) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else if ((orientation >= 90 && orientation <= 120) || (orientation >= 240 && orientation <= 300)) {
                    mActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }

            }
        };

        mOrientationEventListener.enable();
    }


    /**
     * 获取界面方向
     */
    public static int getScreenOrientation(Activity mActivity) {
        int rotation = mActivity.getWindowManager().getDefaultDisplay().getRotation();
        DisplayMetrics dm = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        int orientation;
        // if the device's natural orientation is portrait:
        if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && height > width ||
                (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270) && width > height) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation =
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        } else { // if the device's natural orientation is landscape or if the device is square:
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                default:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
            }
        }

        return orientation;
    }


    //根据开启自动旋转设置是否可以自动旋转
    public void setOritentionAutoRotate() {
        if (mOrientationEventListener == null) return;
        boolean autoRotateOn = (android.provider.Settings.System.getInt(mActivity.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 1);
        if (autoRotateOn) {
            mOrientationEventListener.enable();
        } else {
            mOrientationEventListener.disable();
        }
    }


    //设置屏幕方向
    public void setScreenOrientation(int orientation) {
        Log.e(TAG, "setScreenOrientation: 方向" + orientation);
        mActivity.setRequestedOrientation(orientation);
    }

}
