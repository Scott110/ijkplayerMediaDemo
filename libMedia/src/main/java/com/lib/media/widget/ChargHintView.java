package com.lib.media.widget;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.lib.media.R;
import com.lib.media.ijkplayer.IjkVideoView;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

/**
 * author: heshantao
 * data: 2017/6/9.
 * 播放到达最大时长提示
 */

public class ChargHintView extends FrameLayout {
    private static final String TAG = "ChargHintView";
    private View mAnchor;
    private View root;
    private int videoHeight;
    private View ijkView;
    private LayoutParams hintFrameParams;

    public ChargHintView(@NonNull Context context) {
        this(context, null);
    }

    public ChargHintView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChargHintView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        root = inflater.inflate(R.layout.default_charge_hint_layout, null);
    }

    public void setAnchorView(View view) {
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }

        RelativeLayout.LayoutParams frameParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        addView(root, frameParams);
        if (mAnchor instanceof FrameLayout) {
            ((FrameLayout) mAnchor).addView(this);
        }
    }


    private final View.OnLayoutChangeListener mLayoutChangeListener =
            new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight,
                                           int oldBottom) {
                    updateHintLayoutParams();
                }
            };


    //更新提示view高度
    public void updateHintLayoutParams() {
        if (mAnchor == null) return;
        ijkView = ((ViewGroup) mAnchor).getChildAt(0);
        if (ijkView instanceof IjkVideoView) {
            videoHeight = ijkView.getHeight();
        }
        Log.d(TAG, "updateHintLayoutParams: +" + videoHeight);
        hintFrameParams = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, videoHeight);
        if (root != null) {
            root.setLayoutParams(hintFrameParams);
        }
    }


}
