package com.lib.media.controller;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.lib.media.ijkplayer.IMediaController;

/**
 * author: heshantao
 * data: 2017/3/30.
 */

public class VideoMediaController extends BaseVideoController implements IMediaController {

    public VideoMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public VideoMediaController(Context context) {
        super(context);
    }

    @Override
    public void showOnce(View view) {

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
