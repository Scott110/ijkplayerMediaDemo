package com.lib.media.controller;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import com.lib.media.ijkplayer.IMediaController;

/**
 * author: heshantao
 * data: 2017/3/30.
 */

public class AudioMediaController extends BaseAudioController implements IMediaController {

    public AudioMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AudioMediaController(Context context) {
        super(context);
    }

    @Override
    public void hide() {

    }

    @Override
    public void show(int timeout) {
        show();
    }

    @Override
    public void showOnce(View view) {

    }

    @Override
    public void onControllerTouchDown(float x, float y) {

    }

    @Override
    public void onControllerTouchMove(float x, float y) {

    }

    @Override
    public void onControllerTouchUp() {

    }

    @Override
    public void showVedioThumbnail(Bitmap bitmap) {

    }

    @Override
    public boolean isShowThumbnail() {
        return false;
    }


}
