/*
 * Copyright (C) 2015 Zhang Rui <bbcallen@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lib.media.ijkplayer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.Toast;

import com.lib.media.R;
import com.lib.media.constant.Constant;
import com.lib.media.controller.AudioMediaController;
import com.lib.media.controller.BaseVideoController;
import com.lib.media.controller.ImAudioMediaController;
import com.lib.media.controller.VideoMediaController;
import com.lib.media.listener.ICloseListener;
import com.lib.media.listener.ILightListener;
import com.lib.media.listener.IOrientationListener;
import com.lib.media.util.LightUtil;
import com.lib.media.util.NetworkUtils;
import com.lib.media.util.OrientationUtil;
import com.lib.media.widget.ChargHintView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import tv.danmaku.ijk.media.player.AndroidMediaPlayer;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;
import tv.danmaku.ijk.media.player.TextureMediaPlayer;
import tv.danmaku.ijk.media.player.misc.IMediaDataSource;

public class IjkVideoView extends FrameLayout implements MediaController.MediaPlayerControl, ILightListener, IOrientationListener {
    //-------------------------
    // Extend: Render
    //-------------------------
    public static final int RENDER_NONE = 0;
    public static final int RENDER_SURFACE_VIEW = 1;
    public static final int RENDER_TEXTURE_VIEW = 2;
    public static final int PV_PLAYER__AndroidMediaPlayer = 1;
    public static final int PV_PLAYER__IjkMediaPlayer = 2;
    public static final int PV_PLAYER__IjkExoMediaPlayer = 3;
    // 各种状态
    private static final int STATE_ERROR = -1;//错误
    private static final int STATE_IDLE = 0;//闲置
    private static final int STATE_PREPARING = 1;//准备中
    private static final int STATE_PREPARED = 2;//准备完成
    private static final int STATE_PLAYING = 3;//播放中
    private static final int STATE_PAUSED = 4;//暂停中
    private static final int STATE_PLAYBACK_COMPLETED = 5;//后台播放完成
    private static final int[] s_allAspectRatio = {
            IRenderView.AR_ASPECT_FIT_PARENT,
            IRenderView.AR_ASPECT_FILL_PARENT,
            IRenderView.AR_ASPECT_WRAP_CONTENT,
            IRenderView.AR_16_9_FIT_PARENT,
            IRenderView.AR_4_3_FIT_PARENT};
    Bitmap pauseCoverBmp;
    private String TAG = "IjkVideoView";
    //播放地址可以通过客户端设置
    private Uri mUri;
    //一些头设置
    private Map<String, String> mHeaders;
    /* mCurrentState is a VideoView object's current state.
    * mTargetState is the state that a method caller intends to reach.
    * For instance, regardless the VideoView object's current state,
    * calling pause() intends to bring the object to a target state
    * of STATE_PAUSED.
    * mCurrentState 是指视频View 当前状态
    * mTargetState 是目标状态(比如期望暂停)
    *
    * */
    private int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    // All the stuff we need for playing and showing a video(所有的东西我们需要播放和显示视频)
    private IRenderView.ISurfaceHolder mSurfaceHolder = null;
    //媒体播放器
    private IMediaPlayer mMediaPlayer = null;
    // 视频宽
    private int mVideoWidth;
    //视频高
    private int mVideoHeight;
    //窗口宽
    private int mSurfaceWidth;
    //窗口高
    private int mSurfaceHeight;
    //视频旋转角度
    private int mVideoRotationDegree;
    //媒体控制器
    private IMediaController mMediaController;
    //播放完成监听
    private IMediaPlayer.OnCompletionListener mOnCompletionListener;
    //播放准备监听
    private IMediaPlayer.OnPreparedListener mOnPreparedListener;
    //当前缓冲量
    private int mCurrentBufferPercentage;
    //播放错误监听
    private IMediaPlayer.OnErrorListener mOnErrorListener;
    //播放信息监听
    private IMediaPlayer.OnInfoListener mOnInfoListener;
    // recording the seek position while preparing(记录跳转的位置)
    private int mSeekWhenPrepared = 0;
    //可暂停
    private boolean mCanPause = true;
    //可回退
    private boolean mCanSeekBack = true;
    //可快进
    private boolean mCanSeekForward = true;
    private Context mAppContext;
    //渲染View
    private IRenderView mRenderView;
    private int mVideoSarNum;
    private int mVideoSarDen;
    IMediaPlayer.OnVideoSizeChangedListener mSizeChangedListener =
            new IMediaPlayer.OnVideoSizeChangedListener() {
                public void onVideoSizeChanged(IMediaPlayer mp, int width, int height, int sarNum, int sarDen) {
                    mVideoWidth = mp.getVideoWidth();
                    mVideoHeight = mp.getVideoHeight();
                    mVideoSarNum = mp.getVideoSarNum();
                    mVideoSarDen = mp.getVideoSarDen();
                    Log.d(TAG, "onVideoSizeChanged:------ " + mVideoHeight);
                    if (mVideoWidth != 0 && mVideoHeight != 0) {
                        if (mRenderView != null) {
                            mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                            mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                        }
                        // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                        requestLayout();
                    }
                }
            };
    //准备开始时间
    private long mPrepareStartTime = 0;
    //准备结束时间
    private long mPrepareEndTime = 0;
    //跳转开始时间
    private long mSeekStartTime = 0;
    //跳转结束时间
    private long mSeekEndTime = 0;
    /**
     * 使用编解码器硬编码还是软编码，true 硬编码 false 为软编码
     */
    private boolean usingMediaCodec = false;
    /**
     * 使用编解码是否自转
     */
    private boolean usingMediaCodecAutoRotate = false;
    //是否使用openSles
    private boolean usingOpenSLES = false;
    private boolean isUsingMediaCodecHandleResolutio = false;
    /**
     * Auto Select=,RGB 565=fcc-rv16,RGB 888X=fcc-rv32,YV12=fcc-yv12,默认为RGB 888X
     */
    private String pixelFormat = "";
    //是否可以后台播放
    private boolean mEnableBackgroundPlay = false;
    private boolean usingDetchSufaceTextrueView = false;
    //多媒体类型
    private String mediaType = Constant.MEDIA_TYPE_VIDEO;
    //音频时候的高度
    private int mInitVideoViewHeight;
    //亮度工具类
    private LightUtil mlightUtil;
    //监听方向工具类
    private OrientationUtil mOrientationUtil;
    //是否是付费的
    private boolean mIsCharge = false;
    IMediaPlayer.OnPreparedListener mPreparedListener = new IMediaPlayer.OnPreparedListener() {
        public void onPrepared(IMediaPlayer mp) {
            mPrepareEndTime = System.currentTimeMillis();
            mCurrentState = STATE_PREPARED;

            // Get the capabilities of the player for this stream
            // REMOVED: Metadata

            if (mOnPreparedListener != null) {
                mOnPreparedListener.onPrepared(mMediaPlayer);
            }
            if (mMediaController != null) {
                mMediaController.setEnabled(true);
            }
            mVideoWidth = mp.getVideoWidth();
            mVideoHeight = mp.getVideoHeight();

            int seekToPosition = mSeekWhenPrepared;  // mSeekWhenPrepared may be changed after seekTo() call
            if (seekToPosition != 0) {
                seekTo(seekToPosition);
            }

            Log.d(TAG, "onPrepared video size" + mVideoWidth + "/" + mVideoHeight);
            if (mVideoWidth != 0 && mVideoHeight != 0) {
                // REMOVED: getHolder().setFixedSize(mVideoWidth, mVideoHeight);
                if (mRenderView != null) {
                    mRenderView.setVideoSize(mVideoWidth, mVideoHeight);
                    mRenderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);
                    if (!mRenderView.shouldWaitForResize() || mSurfaceWidth == mVideoWidth && mSurfaceHeight == mVideoHeight) {
                        // We didn't actually change the size (it was already at the size
                        // we need), so we won't get a "surface changed" callback, so
                        // start the video here instead of in the callback.
                        if (mTargetState == STATE_PLAYING) {
                            start();
                            if (mMediaController != null) {
                                mMediaController.show();
                            }
                        } else if (!isPlaying() &&
                                (seekToPosition != 0 || getCurrentPosition() > 0)) {
                            if (mMediaController != null) {
                                // Show the media controls when we're paused into a video and make 'em stick.
                                mMediaController.show(0);
                            }
                        }
                    }
                }
            } else {
                // We don't know the video size yet, but should start anyway.
                // The video size might be reported to us later.

                if (mTargetState == STATE_PLAYING) {
                    start();
                }
            }
        }
    };
    //最大播放时长(分钟)
    private int mMaxPlayTime;
    //是否允许播放
    private boolean isAllowPlay = true;
    CountDownTimer timer = new CountDownTimer(Long.MAX_VALUE, 3000) {


        @Override
        public void onTick(long l) {
            boolean bol = (getCurrentPosition() > mMaxPlayTime);
            if (bol) {
                mMediaController.hide();
                release(true);
                isAllowPlay = false;
                attachChareHintView();
                timer.cancel();
            }
        }

        @Override
        public void onFinish() {

        }
    };
    private IMediaPlayer.OnCompletionListener mCompletionListener =
            new IMediaPlayer.OnCompletionListener() {
                public void onCompletion(IMediaPlayer mp) {
                    mCurrentState = STATE_PLAYBACK_COMPLETED;
                    mTargetState = STATE_PLAYBACK_COMPLETED;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }
                    clearScreenOn();
                    if (mOnCompletionListener != null) {
                        mOnCompletionListener.onCompletion(mMediaPlayer);
                    }
                }
            };
    private IMediaPlayer.OnInfoListener mInfoListener =
            new IMediaPlayer.OnInfoListener() {
                public boolean onInfo(IMediaPlayer mp, int arg1, int arg2) {
                    if (mOnInfoListener != null) {
                        mOnInfoListener.onInfo(mp, arg1, arg2);
                    }

                    switch (arg1) {
                        case IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_START://开始缓冲中
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_START:");
                            showBufferingStatus(true);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BUFFERING_END://结束缓冲中
                            Log.d(TAG, "MEDIA_INFO_BUFFERING_END:");
                            showBufferingStatus(false);
                            break;
                        case IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH:
                            Log.d(TAG, "MEDIA_INFO_NETWORK_BANDWIDTH: " + arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                            Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE:
                            Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_METADATA_UPDATE:
                            Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE:
                            Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                            Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:");
                            break;
                        case IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                            mVideoRotationDegree = arg2;
                            Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: " + arg2);
                            if (mRenderView != null)
                                mRenderView.setVideoRotation(arg2);
                            break;
                        case IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START:
                            Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:");
                            break;
                    }
                    return true;
                }
            };
    private IMediaPlayer.OnErrorListener mErrorListener =
            new IMediaPlayer.OnErrorListener() {
                public boolean onError(IMediaPlayer mp, int framework_err, int impl_err) {
                    Log.d(TAG, "Error: " + framework_err + "," + impl_err);
                    mCurrentState = STATE_ERROR;
                    mTargetState = STATE_ERROR;
                    if (mMediaController != null) {
                        mMediaController.hide();
                    }

                    /* If an error handler has been supplied, use it and finish. */
                    if (mOnErrorListener != null) {
                        if (mOnErrorListener.onError(mMediaPlayer, framework_err, impl_err)) {
                            return true;
                        }
                    }

                    /* Otherwise, pop up an error dialog so the user knows that
                     * something bad has happened. Only try and pop up the dialog
                     * if we're attached to a window. When we're going away and no
                     * longer have a window, don't bother showing the user an error.
                     */
                    if (getWindowToken() != null) {
                        Resources r = mAppContext.getResources();
                        String message = "Unknown error";

                        if (framework_err == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
                            message = "Invalid progressive playback";
                        }

                        new AlertDialog.Builder(getContext())
                                .setMessage(message)
                                .setPositiveButton("error",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int whichButton) {
                                            /* If we get here, there is no onError listener, so
                                             * at least inform them that the video is over.
                                             */
                                                if (mOnCompletionListener != null) {
                                                    mOnCompletionListener.onCompletion(mMediaPlayer);
                                                }
                                            }
                                        })
                                .setCancelable(false)
                                .show();
                    }
                    return true;
                }
            };
    private IMediaPlayer.OnBufferingUpdateListener mBufferingUpdateListener =
            new IMediaPlayer.OnBufferingUpdateListener() {
                public void onBufferingUpdate(IMediaPlayer mp, int percent) {
                    mCurrentBufferPercentage = percent;
                }
            };
    private IMediaPlayer.OnSeekCompleteListener mSeekCompleteListener = new IMediaPlayer.OnSeekCompleteListener() {

        @Override
        public void onSeekComplete(IMediaPlayer mp) {
            mSeekEndTime = System.currentTimeMillis();
        }
    };
    private int mCurrentAspectRatioIndex = 1;
    private int mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
    private List<Integer> mAllRenders = new ArrayList<Integer>();
    private int mCurrentRenderIndex = 0;
    private int mCurrentRender = RENDER_SURFACE_VIEW;
    //默认播放器类型为ijkMediaPlayer
    private int playerType = PV_PLAYER__IjkMediaPlayer;
    IRenderView.IRenderCallback mSHCallback = new IRenderView.IRenderCallback() {
        @Override
        public void onSurfaceChanged(@NonNull IRenderView.ISurfaceHolder holder, int format, int w, int h) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceChanged: unmatched render callback\n");
                return;
            }

            mSurfaceWidth = w;
            mSurfaceHeight = h;
            Log.d(TAG, "onSurfaceChanged: " + mSurfaceHeight);
            boolean isValidState = (mTargetState == STATE_PLAYING);
            boolean hasValidSize = !mRenderView.shouldWaitForResize() || (mVideoWidth == w && mVideoHeight == h);
            if (mMediaPlayer != null && isValidState && hasValidSize) {
                if (mSeekWhenPrepared != 0) {
                    seekTo(mSeekWhenPrepared);
                }
                start();
            }
        }

        @Override
        public void onSurfaceCreated(@NonNull IRenderView.ISurfaceHolder holder, int width, int height) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceCreated: unmatched render callback\n");
                return;
            }
            mSurfaceHolder = holder;

            if (mMediaPlayer != null) {
                bindSurfaceHolder(mMediaPlayer, holder);
            } else {
                openVideo();
            }


        }

        @Override
        public void onSurfaceDestroyed(@NonNull IRenderView.ISurfaceHolder holder) {
            if (holder.getRenderView() != mRenderView) {
                Log.e(TAG, "onSurfaceDestroyed: unmatched render callback\n");
                return;
            }
            // after we return from this we can't use the surface any more
            mSurfaceHolder = null;
            // REMOVED: if (mMediaController != null) mMediaController.hide();
            // REMOVED: release(true);

            releaseWithoutStop();
        }
    };


    public IjkVideoView(Context context) {
        super(context);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public IjkVideoView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }

    @NonNull
    public static String getRenderText(Context context, int render) {
        String text;
        switch (render) {
            case RENDER_NONE:
                text = "Render: Non";
                break;
            case RENDER_SURFACE_VIEW:
                text = "Render: SurfaceView";
                break;
            case RENDER_TEXTURE_VIEW:
                text = "Render: TextureView";
                break;
            default:
                text = "Render: Non";
                break;
        }
        return text;
    }

    @NonNull
    public static String getPlayerText(Context context, int player) {
        String text;
        switch (player) {
            case PV_PLAYER__AndroidMediaPlayer:
                text = "Player: AndroidMediaPlaye";
                break;
            case PV_PLAYER__IjkMediaPlayer:
                text = "Player: IjkMediaPlayer";
                break;
            case PV_PLAYER__IjkExoMediaPlayer:
                text = "Player: IjkExoMediaPlayer";
                break;
            default:
                text = "Player: None";
                break;
        }
        return text;
    }

    private void initVideoView(Context context) {
        mAppContext = context.getApplicationContext();
        initRenders();
        mVideoWidth = 0;
        mVideoHeight = 0;
        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
        mCurrentState = STATE_IDLE;
        mTargetState = STATE_IDLE;
    }

    /**
     * 设置渲染器
     */
    public void setRenderView(IRenderView renderView) {
        if (mRenderView != null) {
            if (mMediaPlayer != null)
                mMediaPlayer.setDisplay(null);

            View renderUIView = mRenderView.getView();
            mRenderView.removeRenderCallback(mSHCallback);
            mRenderView = null;
            removeView(renderUIView);
        }

        if (renderView == null)
            return;
        mRenderView = renderView;
        renderView.setAspectRatio(mCurrentAspectRatio);
        if (mVideoWidth > 0 && mVideoHeight > 0)
            renderView.setVideoSize(mVideoWidth, mVideoHeight);
        if (mVideoSarNum > 0 && mVideoSarDen > 0)
            renderView.setVideoSampleAspectRatio(mVideoSarNum, mVideoSarDen);

        View renderUIView = mRenderView.getView();
        //修复视频填充不满的问题
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mRenderView.addRenderCallback(mSHCallback);
        mRenderView.setVideoRotation(mVideoRotationDegree);

    }

    //设置渲染器
    public void setRender(int render) {
        switch (render) {
            case RENDER_NONE:
                setRenderView(null);
                break;
            case RENDER_TEXTURE_VIEW: {
                TextureRenderView renderView = new TextureRenderView(getContext());
                if (mMediaPlayer != null) {
                    renderView.getSurfaceHolder().bindToMediaPlayer(mMediaPlayer);
                    renderView.setVideoSize(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
                    renderView.setVideoSampleAspectRatio(mMediaPlayer.getVideoSarNum(), mMediaPlayer.getVideoSarDen());
                    renderView.setAspectRatio(mCurrentAspectRatio);
                }
                setRenderView(renderView);
                break;
            }
            case RENDER_SURFACE_VIEW: {
                SurfaceRenderView renderView = new SurfaceRenderView(getContext());
                setRenderView(renderView);
                break;
            }
            default:
                Log.e(TAG, String.format(Locale.getDefault(), "invalid render %d\n", render));
                break;
        }
    }

    /**
     * 设置旋转角度
     */
    public void setPlayerRotation(int rotation) {
        mVideoRotationDegree = rotation;
        if (mRenderView != null) {
            mRenderView.setVideoRotation(mVideoRotationDegree);
        }
    }

    /**
     * Sets video path.
     *
     * @param path the path of the video.
     */
    public void setVideoPath(String path) {
        setVideoURI(Uri.parse(path));
    }

    /**
     * Sets video URI.
     *
     * @param uri the URI of the video.
     */
    public void setVideoURI(Uri uri) {
        setVideoURI(uri, null);
    }

    /**
     * Sets video URI using specific headers.
     *
     * @param uri     the URI of the video.
     * @param headers the headers for the URI request.
     *                Note that the cross domain redirection is allowed by default, but that can be
     *                changed with key/value pairs through the headers parameter with
     *                "android-allow-cross-domain-redirect" as the key and "0" or "1" as the value
     *                to disallow or allow cross domain redirection.
     */
    private void setVideoURI(Uri uri, Map<String, String> headers) {
        mUri = uri;
        mHeaders = headers;
        openVideo();
        requestLayout();
        invalidate();
    }

    public void setUsingMediaCodec(boolean bol) {
        this.usingMediaCodec = bol;
    }

    public void setUsingMediaCodecAutoRotate(boolean bol) {
        this.usingMediaCodecAutoRotate = bol;
    }

    public void setOpenSLES(boolean bol) {
        this.usingOpenSLES = bol;
    }

    public void setPixelFormat(String format) {
        this.pixelFormat = format;
    }

    public void setEnableBackgroundPlay(boolean bol) {
        this.mEnableBackgroundPlay = bol;
    }

    public void setUsingMediaCodecHandleResolutio(boolean bol) {
        this.isUsingMediaCodecHandleResolutio = bol;
    }

    public void setUsingDetchSufaceTextrueView(boolean bol) {
        this.usingDetchSufaceTextrueView = bol;
    }

    public void setCanPause(boolean bol) {
        this.mCanPause = bol;
    }

    public void setCanSeekBack(boolean bol) {
        this.mCanSeekBack = bol;
    }

    public void setCanSeekForward(boolean bol) {
        this.mCanSeekForward = bol;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void openVideo() {
        if (mUri == null || mSurfaceHolder == null) {
            // not ready for playback just yet, will try again later
            return;
        }
        // we shouldn't clear the target state, because somebody might have
        // called start() previously
        release(false);

        AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

        try {
            mMediaPlayer = createPlayer(playerType);

            // TODO: create SubtitleController in MediaPlayer, but we need
            // a context for the subtitle renderers
            final Context context = getContext();
            // REMOVED: SubtitleController

            // REMOVED: mAudioSession
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setOnVideoSizeChangedListener(mSizeChangedListener);
            mMediaPlayer.setOnCompletionListener(mCompletionListener);
            mMediaPlayer.setOnErrorListener(mErrorListener);
            mMediaPlayer.setOnInfoListener(mInfoListener);
            mMediaPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener);
            mMediaPlayer.setOnSeekCompleteListener(mSeekCompleteListener);
            mCurrentBufferPercentage = 0;
            String scheme = mUri.getScheme();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    usingMediaCodec &&
                    (TextUtils.isEmpty(scheme) || scheme.equalsIgnoreCase("file"))) {
                IMediaDataSource dataSource = new FileMediaDataSource(new File(mUri.toString()));
                mMediaPlayer.setDataSource(dataSource);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                mMediaPlayer.setDataSource(mAppContext, mUri, mHeaders);
            } else {
                mMediaPlayer.setDataSource(mUri.toString());
            }
            bindSurfaceHolder(mMediaPlayer, mSurfaceHolder);
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setScreenOnWhilePlaying(true);
            mPrepareStartTime = System.currentTimeMillis();
            mMediaPlayer.prepareAsync();

            // REMOVED: mPendingSubtitleTracks

            // we don't set the target state here either, but preserve the
            // target state that was there before.
            mCurrentState = STATE_PREPARING;

            attachMediaController();
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + mUri, ex);
            mCurrentState = STATE_ERROR;
            mTargetState = STATE_ERROR;
            mErrorListener.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } finally {
            // REMOVED: mPendingSubtitleTracks.clear();
        }
    }

    private void attachMediaController() {
        if (mMediaPlayer != null && mMediaController != null) {
            mMediaController.setMediaPlayer(this);
            Log.d(TAG, "attachMediaController: " + mVideoHeight);
            View anchorView = this.getParent() instanceof View ?
                    (View) this.getParent() : this;
            mMediaController.setAnchorView(anchorView);
            mMediaController.setEnabled(isInPlaybackState());
        }
    }

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mOnPreparedListener = l;
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mOnCompletionListener = l;
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mOnErrorListener = l;
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(IMediaPlayer.OnInfoListener l) {
        mOnInfoListener = l;
    }

    // REMOVED: mSHCallback
    private void bindSurfaceHolder(IMediaPlayer mp, IRenderView.ISurfaceHolder holder) {
        if (mp == null)
            return;

        if (holder == null) {
            mp.setDisplay(null);
            return;
        }

        holder.bindToMediaPlayer(mp);
    }

    public void releaseWithoutStop() {
        if (mMediaPlayer != null)
            mMediaPlayer.setDisplay(null);
    }

    /*
     * release the media player in any state
     */
    public void release(boolean cleartargetstate) {
        if (mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
            // REMOVED: mPendingSubtitleTracks.clear();
            mCurrentState = STATE_IDLE;
            if (cleartargetstate) {
                mTargetState = STATE_IDLE;
            }
            AudioManager am = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
            am.abandonAudioFocus(null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (!mediaType.equals(Constant.MEDIA_TYPE_VIDEO) || !isAllowPlay) {
            return false;
        }
        float x = ev.getX();
        float y = ev.getY();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (mMediaController instanceof BaseVideoController) {
                    mMediaController.onControllerTouchDown(x, y);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mMediaController instanceof BaseVideoController) {
                    mMediaController.onControllerTouchMove(x, y);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mMediaController instanceof BaseVideoController) {
                    mMediaController.onControllerTouchUp();
                }
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        if (isInPlaybackState() && mMediaController != null) {
            toggleMediaControlsVisiblity();
        }
        return false;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean isKeyCodeSupported = keyCode != KeyEvent.KEYCODE_BACK &&
                keyCode != KeyEvent.KEYCODE_VOLUME_UP &&
                keyCode != KeyEvent.KEYCODE_VOLUME_DOWN &&
                keyCode != KeyEvent.KEYCODE_VOLUME_MUTE &&
                keyCode != KeyEvent.KEYCODE_MENU &&
                keyCode != KeyEvent.KEYCODE_CALL &&
                keyCode != KeyEvent.KEYCODE_ENDCALL;
        if (isInPlaybackState() && isKeyCodeSupported && mMediaController != null) {
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                    keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                } else {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                if (!mMediaPlayer.isPlaying()) {
                    start();
                    mMediaController.hide();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                    || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                if (mMediaPlayer.isPlaying()) {
                    pause();
                    mMediaController.show();
                }
                return true;
            } else {
                toggleMediaControlsVisiblity();
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void toggleMediaControlsVisiblity() {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show(0);
        }
    }


    //-------------------------
    // Extend: Aspect Ratio
    //-------------------------

    @Override
    public void start() {
        if (isInPlaybackState()) {
            if (!mUri.toString().startsWith("file") && !NetworkUtils.isWifiConnected(mAppContext)) {
                showWifiDialog();
                return;
            }

            startPlay();
        }
        mTargetState = STATE_PLAYING;
    }

    //开始播放
    private void startPlay() {
        keepScreenOn();
        mMediaPlayer.start();
        observaleVedioCurPostion();
        mCurrentState = STATE_PLAYING;
    }

    @Override
    public void pause() {
        if (isInPlaybackState()) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
            }
        }
        mTargetState = STATE_PAUSED;
    }

    public void suspend() {
        release(false);
    }

    public void resume() {
        openVideo();
    }

    public void onPause() {
        if (mMediaController == null) return;
        if (mediaType.equals(Constant.MEDIA_TYPE_VIDEO)) {
            if (!mMediaController.isShowThumbnail() && isAllowPlay) {
                getCovreBitmap();
            }
        }
        pause();
        mSeekWhenPrepared = getCurrentPosition();

    }

    public void onResume() {
        if (mMediaController == null) return;
        if (isAllowPlay) {
            if (mediaType.equals(Constant.MEDIA_TYPE_VIDEO)) {
                mMediaController.showVedioThumbnail(pauseCoverBmp);
            }
            mMediaController.show(0);
        }
    }

    @Override
    public int getDuration() {
        if (isInPlaybackState()) {
            return (int) mMediaPlayer.getDuration();
        }
        return -1;
    }

    @Override
    public int getCurrentPosition() {
        int position = 0;
        if (isInPlaybackState()) {
            position = (int) mMediaPlayer.getCurrentPosition();
        }
        return position;
    }

    @Override
    public void seekTo(int msec) {
        if (isInPlaybackState()) {
            mSeekStartTime = System.currentTimeMillis();
            mMediaPlayer.seekTo(msec);
            mSeekWhenPrepared = 0;
        } else {
            mSeekWhenPrepared = msec;
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        if (mMediaPlayer != null) {
            return mCurrentBufferPercentage;
        }
        return 0;
    }

    private boolean isInPlaybackState() {
        return (mMediaPlayer != null &&
                mCurrentState != STATE_ERROR &&
                mCurrentState != STATE_IDLE &&
                mCurrentState != STATE_PREPARING);
    }

    @Override
    public boolean canPause() {
        return mCanPause;
    }

    @Override
    public boolean canSeekBackward() {
        return mCanSeekBack;
    }

    @Override
    public boolean canSeekForward() {
        return mCanSeekForward;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    public int toggleAspectRatio() {
        mCurrentAspectRatioIndex++;
        mCurrentAspectRatioIndex %= s_allAspectRatio.length;

        mCurrentAspectRatio = s_allAspectRatio[mCurrentAspectRatioIndex];
        if (mRenderView != null)
            mRenderView.setAspectRatio(mCurrentAspectRatio);
        return mCurrentAspectRatio;
    }

    //初始化渲染器
    private void initRenders() {
        mAllRenders.clear();
        /**添加surface渲染*/
        mAllRenders.add(RENDER_SURFACE_VIEW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            /**添加texture渲染*/
            mAllRenders.add(RENDER_TEXTURE_VIEW);
            mCurrentRenderIndex = 1;
        } else {
            mCurrentRenderIndex = 0;
        }
        mCurrentRender = mAllRenders.get(mCurrentRenderIndex);
        setRender(mCurrentRender);
    }

    //-------------------------
    // 切换播放器
    //-------------------------
    public int togglePlayer() {
        if (mMediaPlayer != null)
            mMediaPlayer.release();

        if (mRenderView != null)
            mRenderView.getView().invalidate();
        openVideo();
        return playerType;
    }

    //-------------------------
    // Extend: Background
    //-------------------------

    public IMediaPlayer createPlayer(int playerType) {
        IMediaPlayer mediaPlayer = null;

        switch (playerType) {
            case PV_PLAYER__AndroidMediaPlayer: {
                AndroidMediaPlayer androidMediaPlayer = new AndroidMediaPlayer();
                mediaPlayer = androidMediaPlayer;
            }
            break;
            case PV_PLAYER__IjkMediaPlayer:
            default: {
                IjkMediaPlayer ijkMediaPlayer = null;
                if (mUri != null) {
                    ijkMediaPlayer = new IjkMediaPlayer();
                    ijkMediaPlayer.native_setLogLevel(IjkMediaPlayer.IJK_LOG_DEBUG);

                    if (usingMediaCodec) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
                        if (usingMediaCodecAutoRotate) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 0);
                        }
                        if (isUsingMediaCodecHandleResolutio) {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 1);
                        } else {
                            ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-handle-resolution-change", 0);
                        }
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 0);
                    }

                    if (usingOpenSLES) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 0);
                    }

                    if (TextUtils.isEmpty(pixelFormat)) {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", IjkMediaPlayer.SDL_FCC_RV32);
                    } else {
                        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "overlay-format", pixelFormat);
                    }
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1);
                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0);

                    ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48);
                }
                mediaPlayer = ijkMediaPlayer;
            }
            break;
        }

        if (usingDetchSufaceTextrueView) {
            mediaPlayer = new TextureMediaPlayer(mediaPlayer);
        }

        return mediaPlayer;
    }

    private void initBackground() {
        if (mEnableBackgroundPlay) {
            MediaPlayerService.intentToStart(getContext());
            mMediaPlayer = MediaPlayerService.getMediaPlayer();
        }
    }

    /**
     * 设置播放区域拉伸类型
     */
    public void setAspectRatio(int aspectRatio) {
        for (int i = 0; i < s_allAspectRatio.length; i++) {
            if (s_allAspectRatio[i] == aspectRatio) {
                mCurrentAspectRatioIndex = i;
                mCurrentAspectRatio = aspectRatio;
                if (mRenderView != null) {
                    mRenderView.setAspectRatio(mCurrentAspectRatio);
                }
                break;
            }
        }
    }

    /*
    * 获得暂停时候的封面图
    * */
    public Bitmap getCovreBitmap() {
        if (mRenderView == null) return null;
        if (mRenderView instanceof TextureRenderView) {
            pauseCoverBmp = ((TextureRenderView) mRenderView).getBitmap(mVideoWidth, mVideoHeight);
        }
        return pauseCoverBmp;
    }

    /*
    *设置播放控制器等相关信息
    * @activity 视频播放必须为Activity
    * */
    public void setMediaControllerInfo(Context activity, String type, ICloseListener closeListener) {
        if (mMediaController != null) {
            mMediaController.hide();
        }
        switch (type) {
            case Constant.MEDIA_TYPE_VIDEO:
                mMediaController = new VideoMediaController(mAppContext);
                mlightUtil = new LightUtil((Activity) activity, (VideoMediaController) mMediaController);
                mOrientationUtil = new OrientationUtil((Activity) activity, (VideoMediaController) mMediaController);
                ((VideoMediaController) mMediaController).setWindowLightListener(this);
                ((VideoMediaController) mMediaController).setCloseListener(closeListener);
                ((VideoMediaController) mMediaController).setOrientationListener(this);
                break;
            case Constant.MEDIA_TYPE_AUDIO:
                mMediaController = new AudioMediaController(mAppContext);
                ((AudioMediaController) mMediaController).setCloseListener(closeListener);
                break;
            case Constant.MEDIA_TYPE_IM_AUDIO:
                mMediaController = new ImAudioMediaController(mAppContext);
                break;
            default:
                break;
        }
        initVedioViewHeight(type);

    }

    public IMediaController getMediaController() {
        return mMediaController;
    }

    private void initVedioViewHeight(String type) {
        switch (type) {
            case Constant.MEDIA_TYPE_VIDEO:
                mInitVideoViewHeight = (int) getResources().getDimension(R.dimen.default_vedio_height);
                break;
            case Constant.MEDIA_TYPE_AUDIO:
                mInitVideoViewHeight = (int) getResources().getDimension(R.dimen.default_audio_height);
                break;
            case Constant.MEDIA_TYPE_IM_AUDIO:
                mInitVideoViewHeight = (int) getResources().getDimension(R.dimen.default_im_audio_height);
                break;
            default:
                break;
        }

        updateLayout(mInitVideoViewHeight);
    }

    //横竖屏切换更新界面
    public void onConfigChange(Configuration configuration) {
        if (mMediaController instanceof BaseVideoController) {
            int orientationType = configuration.orientation;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            if (orientationType == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                height = mInitVideoViewHeight;
            }
            updateLayout(height);
            ((BaseVideoController) mMediaController).onConfigurationChanged(configuration);
        }
    }

    //更新界面尺寸
    private void updateLayout(int height) {
        FrameLayout.LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        this.setLayoutParams(layoutParams);
    }

    //根据缓冲状态显示是否加载
    private void showBufferingStatus(boolean bol) {
        if (mMediaController instanceof BaseVideoController) {
            mMediaController.isBuffering(bol);
        }
    }

    @Override
    public void initWindowLight() {
        if (mlightUtil != null) {
            mlightUtil.initLight();
        }
    }

    @Override
    public void getWindowLight() {
        if (mlightUtil != null) {
            mlightUtil.setControllerLight();
        }
    }

    @Override
    public void setWindowLight(float light) {
        if (mlightUtil != null) {
            mlightUtil.setLight(light);
        }
    }

    @Override
    public void setScreenOrientation(int orientation) {
        if (mOrientationUtil != null) {
            mOrientationUtil.setScreenOrientation(orientation);
        }
    }

    //返回键退出全屏
    public void restoreToNormal() {
        if (mOrientationUtil != null) {
            mOrientationUtil.restoreToNormal();
        }
    }

    //设置开始跳过多长时间
    public void setSkipTime(int msec) {
        mSeekWhenPrepared = msec;
    }

    //WiFi 提示对话框
    public void showWifiDialog() {
        if (!NetworkUtils.isAvailable(mAppContext)) {
            Toast.makeText(mAppContext, getResources().getString(R.string.no_net), Toast.LENGTH_LONG).show();
            return;
        }
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setMessage(getResources().getString(R.string.not_wifi));
        builder.setPositiveButton(getResources().getString(R.string.not_wifi_confirm), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startPlay();
                dialog.dismiss();

            }
        });
        builder.setNegativeButton(getResources().getString(R.string.not_wifi_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    //设置最大播放时长
    public void setMaxPlayTime(int time) {
        this.mMaxPlayTime = time;
    }

    //是否是付费的
    public void isCharge(boolean mCharge) {
        mIsCharge = mCharge;
    }

    //开始监听是否达到最大播放时长
    private void observaleVedioCurPostion() {
        if (mIsCharge) {
            if (timer != null) {
                timer.cancel();
                timer.start();
            }
        }
    }


    //保存屏幕不熄屏
    private void keepScreenOn() {
        if (mediaType != null && mediaType.equals(Constant.MEDIA_TYPE_VIDEO)) {
            ((Activity) getContext()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    //清除屏幕不熄屏
    private void clearScreenOn() {
        if (mediaType != null && mediaType.equals(Constant.MEDIA_TYPE_VIDEO)) {
            ((Activity) getContext()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    //添加到达播放最大时长的提示View
    private void attachChareHintView() {
        ChargHintView chargHintView = new ChargHintView(mAppContext);
        View anchorView = this.getParent() instanceof View ?
                (View) this.getParent() : this;
        chargHintView.setAnchorView(anchorView);
    }

    //释放所有的多媒体信息
    public void restorAll() {
        release(true);
        FrameLayout frameLayout = (FrameLayout) this.getParent();
        if (frameLayout != null) {
            int childSize = frameLayout.getChildCount();
            if (childSize > 1) {
                for (int i = 1; i < childSize; i++) {
                    frameLayout.removeViewAt(i);
                }
            }

            mIsCharge = false;
            mMaxPlayTime = 0;
            isAllowPlay = true;
        }
    }

    /**
     * 结束视频播放
     */
    public void stopPlayback() {
        release(true);
        clearScreenOn();
    }

}
