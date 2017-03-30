package com.lib.media.controller;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.lib.media.ijkplayer.IMediaController;

/**
 * author: heshantao
 * data: 2017/3/30.
 */

public class VideoMediaController extends BaseVideoController implements IMediaController {
    public VideoMediaController(Activity mActivity, AttributeSet attrs) {
        super(mActivity, attrs);
    }

    public VideoMediaController(Activity mActivity) {
        super(mActivity);
    }

    @Override
    public void showOnce(View view) {

    }
}
