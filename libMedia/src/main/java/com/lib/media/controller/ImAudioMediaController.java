package com.lib.media.controller;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.View;

import com.lib.media.ijkplayer.IMediaController;

/**
 * author: heshantao
 * data: 2017/3/31.
 */

public class ImAudioMediaController extends BaseImAudioController implements IMediaController {
    public ImAudioMediaController(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ImAudioMediaController(Context context) {
        super(context);
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

    @Override
    public void isBuffering(boolean bol) {

    }


    @Override
    public void hide() {

    }

    @Override
    public void show(int timeout) {
        show();
    }
}
