package com.lib.media.controller;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.lib.media.PolicyCompat;
import com.lib.media.R;
import com.lib.media.ijkplayer.IjkVideoView;
import com.lib.media.listener.CloseListener;
import com.lib.media.listener.IMediaViewController;
import com.lib.media.listener.LightListener;
import com.lib.media.listener.OrientationListener;
import com.lib.media.util.ScreenUtil;
import com.lib.media.widget.seekbar.VerticalSeekBar;

import java.util.Formatter;
import java.util.Locale;

/**
 * author: heshantao
 * data: 2017/2/15.
 * 基础媒体控制器
 */

public class BaseVideoController extends FrameLayout implements IMediaViewController {
    private static final String TAG = BaseVideoController.class.getSimpleName();
    private MediaController.MediaPlayerControl mPlayer;
    private View mAnchor;
    private View mRoot;
    private WindowManager mWindowManager;
    private Window mWindow;
    private View mDecor;
    private WindowManager.LayoutParams mDecorLayoutParams;
    private ProgressBar mProgress;
    private VerticalSeekBar mVolumeProgress;
    private VerticalSeekBar mBrightnessProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mShowing;
    private boolean mDragging;
    private static final int sDefaultTimeout = 3000;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    StringBuilder mFormatBuilder;
    Formatter mFormatter;
    private ImageButton mPauseButton;
    private ImageButton mFullScreenBtn;
    private LinearLayout llVolume, llLight, llForwardBox, llBottomController;
    private RelativeLayout llClose;
    private ImageView mThumImg;
    private AppCompatTextView mForwardTxt;
    private AppCompatTextView mForwardTotalTxt;
    private AccessibilityManager mAccessibilityManager;
    private Context mActivity;
    //屏幕方向
    private int oritentionType = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
    //尽支持横屏
    private boolean mOnlyFullScreen = false;
    //是否是付费的
    private boolean mIsCharge = false;
    //最大播放时长(分钟)
    private int mMaxPlayTime;
    //当前亮度
    private float mBrightness;
    //当前音量
    private int mVolume;
    //当前播放进度
    private int mPosition;
    //最大音量
    private int mMaxVolume;
    //是否显示视频缩略图
    private boolean mShowThumbnail;
    /**
     * 音频管理器
     */
    private AudioManager audioManager;
    //是否可以通过手势滑动改变音量、亮度
    private boolean mWidgetUsable = true;
    //是否改变音量
    private boolean mChangeVolumn = false;
    //是否改变播放位置
    private boolean mChangePosition = false;
    //是否改变亮度
    private boolean mChangeBrightness = false;
    //触摸显示虚拟按键
    protected boolean mShowVKey = false;
    //触摸的X
    protected float mDownX;
    //触摸的Y
    protected float mDownY;
    //移动Y的距离
    protected float mMoveY;
    //手势偏差值
    protected int mGestureOffset = 40;
    //屏幕宽度
    protected int mScreenWidth;
    //屏幕高度
    protected int mScreenHeight;
    //手动滑动起始X轴偏移量(虚拟按键高度)
    protected int mSlideXOffset;
    //手动滑动起始Y轴偏移量(底部控制栏高度)
    protected int mSlideYOffset;
    private boolean mFirstTouch = false;
    //需要进入的位置
    private int mSeekTimePosition;
    private OrientationListener mlistener;
    private LightListener mLightListener;
    private CloseListener mCloseListener;
    private ImageButton mCloseBtn;
    //视频高度
    private int videoHeight;

    public BaseVideoController(Context cxt, AttributeSet attrs) {
        super(cxt, attrs);
        mRoot = this;
        this.mActivity = cxt;
        init();

    }


    public BaseVideoController(Context cxt) {
        super(cxt);
        this.mActivity = cxt;
        initFloatingWindowLayout();
        initFloatingWindow();
        init();

    }


    private void init() {
        audioManager = (AudioManager) mActivity.getSystemService(Context.AUDIO_SERVICE);
        mAccessibilityManager = (AccessibilityManager) mActivity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        initSlideOffset();

    }


    private void initFloatingWindow() {
        mWindowManager = (WindowManager) mActivity.getSystemService(Context.WINDOW_SERVICE);
        mWindow = PolicyCompat.createWindow(mActivity);
        mWindow.setWindowManager(mWindowManager, null, null);
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mDecor = mWindow.getDecorView();
        mDecor.setOnTouchListener(mTouchListener);
        mWindow.setContentView(this);
        mWindow.setBackgroundDrawableResource(android.R.color.transparent);
        // While the media controller is up, the volume control keys should
        // affect the media stream type
        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    // Allocate and initialize the static parts of mDecorLayoutParams. Must
    // also call updateFloatingWindowLayout() to fill in the dynamic parts
    // (y and width) before mDecorLayoutParams can be used.
    private void initFloatingWindowLayout() {
        mDecorLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.height = FrameLayout.LayoutParams.WRAP_CONTENT;
        p.x = 0;
        p.format = PixelFormat.TRANSLUCENT;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
        p.token = null;
        p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
    }


    //旋转状态改变(横竖屏切换按钮、方向传感器)
    public void onConfigurationChanged(final Configuration newConfig) {
        oritentionType = newConfig.orientation;
        Log.d(TAG, "onConfigurationChanged: ----" + oritentionType);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                updateFullScreenBtn();
            }
        });
    }


    // Update the dynamic parts of mDecorLayoutParams
    // Must be called with mAnchor != NULL.
    private void updateFloatingWindowLayout() {
        int[] anchorPos = new int[2];
        mAnchor.getLocationOnScreen(anchorPos);

        // we need to know the size of the controller so we can properly position it
        // within its space
        mDecor.measure(View.MeasureSpec.makeMeasureSpec(mAnchor.getWidth(), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(mAnchor.getHeight(), View.MeasureSpec.AT_MOST));

        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.width = mAnchor.getWidth();
        p.x = anchorPos[0] + (mAnchor.getWidth() - p.width) / 2;
        p.y = anchorPos[1] + mAnchor.getHeight() - mDecor.getMeasuredHeight();
    }

    // This is called whenever mAnchor's layout bound changes
    private final View.OnLayoutChangeListener mLayoutChangeListener =
            new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right,
                                           int bottom, int oldLeft, int oldTop, int oldRight,
                                           int oldBottom) {
                    Log.d(TAG, "onSurfaceChanged  onLayoutChange: ");
                    updateControllerLayoutParams();
                    updateFloatingWindowLayout();
                    if (mShowing) {
                        mWindowManager.updateViewLayout(mDecor, mDecorLayoutParams);
                    }
                }
            };


    private final View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (mShowing) {
                    hide();
                }
            }
            return false;
        }
    };

    public void setMediaPlayer(MediaController.MediaPlayerControl player) {
        mPlayer = player;
        updatePausePlay();
    }

    /**
     * Set the view that acts as the anchor for the control view.
     * This can for example be a VideoView, or your Activity's main view.
     * When VideoView calls this method, it will use the VideoView's parent
     * as the anchor.
     *
     * @param view The view to which to anchor the controller when it is visible.
     */
    public void setAnchorView(View view) {
        if (mAnchor != null) {
            mAnchor.removeOnLayoutChangeListener(mLayoutChangeListener);
        }
        mAnchor = view;
        if (mAnchor != null) {
            mAnchor.addOnLayoutChangeListener(mLayoutChangeListener);
        }

        Log.d(TAG, "onSurfaceChanged  setAnchorView: ");

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );

        removeAllViews();
        View v = makeControllerView();
        addView(v, frameParams);
        show(0);
    }


    //更新控制面板高度
    public void updateControllerLayoutParams() {
        if (mAnchor == null) return;
        View view = ((ViewGroup) mAnchor).getChildAt(0);
        if (view instanceof IjkVideoView) {
            videoHeight = view.getHeight();
        }
        Log.d(TAG, "updateControllerLayoutParams: +" + videoHeight);
        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, videoHeight);
        View child = this.getChildAt(0);
        if (child != null) {
            child.setLayoutParams(frameParams);
        }
    }


    @Override
    public void onFinishInflate() {
        if (mRoot != null)
            initControllerView(mRoot);
    }

    /**
     * Create the view that holds the widgets that control playback.
     * Derived classes can override this to create their own.
     *
     * @return The controller view.
     * @hide This doesn't work as advertised
     */
    protected View makeControllerView() {
        LayoutInflater inflate = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(getControllerLayoutId(), null);
        initControllerView(mRoot);
        return mRoot;
    }

    public int getControllerLayoutId() {
        return R.layout.default_video_controller;
    }

    //初始化控制面板
    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.play_pause_btn);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(mPauseListener);
        }

        mFullScreenBtn = (ImageButton) v.findViewById(R.id.fullscreen_btn);
        if (mFullScreenBtn != null) {
            mFullScreenBtn.requestFocus();
            mFullScreenBtn.setOnClickListener(mFullScreenListener);
        }

        mCloseBtn = (ImageButton) v.findViewById(R.id.close_btn);
        if (mCloseBtn != null) {
            mCloseBtn.requestFocus();
            mCloseBtn.setOnClickListener(mCloseBtnListener);
        }

        mProgress = (SeekBar) v.findViewById(R.id.progress_sb);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                seeker.setOnSeekBarChangeListener(mSeekListener);
            }
            mProgress.setMax(1000);
        }


        mVolumeProgress = (VerticalSeekBar) v.findViewById(R.id.volume_skbar);
        if (mVolumeProgress != null) {
            if (mVolumeProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mVolumeProgress;
            }
            mVolumeProgress.setMax(100);
        }
        initVolumn();

        mBrightnessProgress = (VerticalSeekBar) v.findViewById(R.id.brightness_skbar);
        if (mBrightnessProgress != null) {
            if (mBrightnessProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mBrightnessProgress;
            }
            mBrightnessProgress.setMax(100);
        }

        initBrightness();
        mEndTime = (TextView) v.findViewById(R.id.end_time_tv);
        mCurrentTime = (TextView) v.findViewById(R.id.current_time_tv);
        mThumImg = (ImageView) v.findViewById(R.id.thum_img);
        mForwardTxt = (AppCompatTextView) v.findViewById(R.id.txt_forward_time);
        mForwardTotalTxt = (AppCompatTextView) v.findViewById(R.id.txt_forward_total_time);
        mFormatBuilder = new StringBuilder();
        mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        llBottomController = (LinearLayout) v.findViewById(R.id.bottom_contr_ll);
        llForwardBox = (LinearLayout) v.findViewById(R.id.ll_forward_box);
        llLight = (LinearLayout) v.findViewById(R.id.ll_light);
        llVolume = (LinearLayout) v.findViewById(R.id.ll_volume);
        llClose = (RelativeLayout) v.findViewById(R.id.ll_close);

    }

    //初始化音量
    private void initVolumn() {
        mVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mVolumeProgress.setProgress(mVolume * 100 / mMaxVolume);
    }

    //初始化亮度
    public void initBrightness() {
        try {
            int e = Settings.System.getInt(mActivity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            float progress = 1.0F * (float) e / 255.0F;
            mLightListener.initWindowLight();
            mBrightnessProgress.setProgress((int) (progress * 100));
            Log.d(TAG, "initBrightness: 亮度" + progress * 100);
        } catch (Settings.SettingNotFoundException var7) {
            var7.printStackTrace();
        }

    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    public void show() {
        show(sDefaultTimeout);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }

            // TODO What we really should do is add a canSeek to the MediaPlayerControl interface;
            // this scheme can break the case when applications want to allow seek through the
            // progress bar but disable forward/backward buttons.
            //
            // However, currently the flags SEEK_BACKWARD_AVAILABLE, SEEK_FORWARD_AVAILABLE,
            // and SEEK_AVAILABLE are all (un)set together; as such the aforementioned issue
            // shouldn't arise in existing applications.
            if (mProgress != null && !mPlayer.canSeekBackward() && !mPlayer.canSeekForward()) {
                mProgress.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     *                the controller until hide() is called.
     */
    public void show(int timeout) {
        Log.d(TAG, "show: ----------" + timeout + "----是否显示---" + mShowing);
        if (!mShowing && mAnchor != null) {
            setProgress();
            if (mPauseButton != null) {
                mPauseButton.requestFocus();
            }
            if (mCloseBtn != null) {
                mCloseBtn.requestFocus();
            }
            if (mFullScreenBtn != null) {
                mFullScreenBtn.requestFocus();
            }

            disableUnsupportedButtons();
            updateFloatingWindowLayout();
            mWindowManager.addView(mDecor, mDecorLayoutParams);
            mShowing = true;
        }
        updatePausePlay();

        // cause the progress bar to be updated even if mShowing
        // was already true.  This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        mHandler.sendEmptyMessage(SHOW_PROGRESS);

        if (timeout != 0 && !mAccessibilityManager.isTouchExplorationEnabled()) {
            mHandler.removeMessages(FADE_OUT);
            Message msg = mHandler.obtainMessage(FADE_OUT);
            mHandler.sendMessageDelayed(msg, timeout);
        }
    }

    public boolean isShowing() {
        return mShowing;
    }

    /**
     * Remove the controller from the screen.
     */
    public void hide() {
        if (mAnchor == null)
            return;

        if (mShowing) {
            try {
                mHandler.removeMessages(SHOW_PROGRESS);
                mWindowManager.removeView(mDecor);
            } catch (IllegalArgumentException ex) {
                Log.w("MediaController", "already removed");
            }
            mShowing = false;
        }
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            switch (msg.what) {
                case FADE_OUT:
                    hide();
                    break;
                case SHOW_PROGRESS:
                    pos = setProgress();
                    if (!mDragging && mShowing && mPlayer.isPlaying()) {
                        msg = obtainMessage(SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
            }
        }
    };

    //时间格式
    private String stringForTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        mFormatBuilder.setLength(0);
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null)
            mEndTime.setText(stringForTime(duration));
        if (mCurrentTime != null)
            mCurrentTime.setText(stringForTime(position));

        return position;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "onTouchEvent: -----B----down-------");
                onControllerTouchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                Log.d(TAG, "onTouchEvent: -----B----move-------");
                onControllerTouchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "onTouchEvent: -----B----up-------");
                onControllerTouchUp();
                break;
            case MotionEvent.ACTION_CANCEL:
                hide();
                break;
            default:
                break;
        }
        return true;
    }

    //控制器Action_Down
    public void onControllerTouchDown(float x, float y) {
        mHandler.removeMessages(FADE_OUT);
        show(0);
        mDownX = x;
        mDownY = y;
        mMoveY = 0;
        mChangeVolumn = false;
        mChangePosition = false;
        mShowVKey = false;
        mChangeBrightness = false;
        mFirstTouch = true;
        llVolume.setVisibility(GONE);
        llLight.setVisibility(GONE);
        llForwardBox.setVisibility(GONE);
    }


    //控制器Action_Move
    public void onControllerTouchMove(float x, float y) {
        float deltaX = x - mDownX;
        float deltaY = y - mDownY;
        float absDeltaX = Math.abs(deltaX);
        float absDeltaY = Math.abs(deltaY);
        if (oritentionType != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT && mWidgetUsable) {
            if (!mChangePosition && !mChangeVolumn && !mChangeBrightness) {
                if (absDeltaX >= mGestureOffset) {
                    //防止全屏虚拟按键
                    mScreenWidth = ScreenUtil.getScreenWidth(getContext());
                    if (Math.abs(mScreenWidth - mDownX) > mSlideXOffset) {
                        if (mPlayer.canSeekBackward() && mPlayer.canSeekForward()) {
                            mChangePosition = true;
                            if (mFirstTouch) {
                                mPosition = getCurrentPositionWhenPlaying();
                                llForwardBox.setVisibility(VISIBLE);
                                mFirstTouch = false;
                            }
                        }
                    }
                } else if (absDeltaY > mGestureOffset) {
                    mScreenHeight = ScreenUtil.getScreenHeight(getContext());
                    mScreenWidth = ScreenUtil.getScreenWidth(getContext());
                    boolean noEnd = Math.abs(mScreenHeight - mDownY) > mSlideYOffset;
                    mChangeBrightness = (mDownX < mScreenWidth * 0.5f) && noEnd;
                    if (mChangeBrightness) {
                        if (mFirstTouch) {
                            mLightListener.getWindowLight();
                            llLight.setVisibility(VISIBLE);
                            mFirstTouch = false;
                        }
                    } else {
                        mChangeVolumn = true;
                        if (mFirstTouch) {
                            mVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                            llVolume.setVisibility(VISIBLE);
                            mFirstTouch = false;
                        }
                    }
                }
            }

        }

        if (mChangePosition) {
            slideProgress(deltaX);
        } else if (mChangeVolumn) {
            slideVolume(-deltaY);
        } else if (!mChangeVolumn && mChangeBrightness) {
            slideLight(-deltaY);
        }
    }

    //控制器Action_UP
    public void onControllerTouchUp() {
        llVolume.setVisibility(GONE);
        llLight.setVisibility(GONE);
        llForwardBox.setVisibility(GONE);
        if (!mShowThumbnail) {
            show(sDefaultTimeout);
        }
        if (mChangePosition) {
            seekToPosition();
            restForwardBox();
        }
    }

    @Override
    public boolean onTrackballEvent(MotionEvent ev) {
        show(sDefaultTimeout);
        return false;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                show(sDefaultTimeout);
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP
                || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
                || keyCode == KeyEvent.KEYCODE_CAMERA) {
            // don't show the controls for volume adjustment
            return super.dispatchKeyEvent(event);
        } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
            if (uniqueDown) {
                hide();
            }
            return true;
        }

        show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    //播放暂停监听
    private final View.OnClickListener mPauseListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            disMissVedioThumbnail();
            doPauseResume();
            show(sDefaultTimeout);
        }
    };

    //播放全屏监听
    private final View.OnClickListener mFullScreenListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleFullScreen();
        }
    };


    //关闭按钮监听
    private final View.OnClickListener mCloseBtnListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            mCloseListener.closed();
        }
    };

    private void updatePausePlay() {
        if (mRoot == null || mPauseButton == null)
            return;

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(R.drawable.pause_selector);
        } else {
            mPauseButton.setImageResource(R.drawable.play_selector);
        }
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    // There are two scenarios that can trigger the seekbar listener to trigger:
    //
    // The first is the user using the touchpad to adjust the posititon of the
    // seekbar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "mDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private final SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            show(3600000);

            mDragging = true;

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            mHandler.removeMessages(SHOW_PROGRESS);
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
            if (!fromuser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }

            long duration = mPlayer.getDuration();
            long newposition = (duration * progress) / 1000L;
            mPlayer.seekTo((int) newposition);
            if (mCurrentTime != null)
                mCurrentTime.setText(stringForTime((int) newposition));
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            mDragging = false;
            setProgress();
            updatePausePlay();
            show(sDefaultTimeout);

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            mHandler.sendEmptyMessage(SHOW_PROGRESS);
        }
    };


    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public CharSequence getAccessibilityClassName() {
        return MediaController.class.getName();
    }


    /**
     * 横屏、竖屏切换
     */
    public void toggleFullScreen() {
        int orientation;
        if (oritentionType == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        } else {
            orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        mlistener.setScreenOrientation(orientation);
    }


    /**
     * 更新全屏和半屏按钮
     */
    public void updateFullScreenBtn() {
        if (oritentionType == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            mFullScreenBtn.setImageResource(R.mipmap.icon_fullscreen_stretch);
        } else {
            mFullScreenBtn.setImageResource(R.mipmap.icon_fullscreen_shrink);
        }
    }


    //设置是否仅支持横屏
    public void setOnlyFullScreen(boolean bool) {
        this.mOnlyFullScreen = bool;
    }


    public boolean getIsOnlyFullScreen() {
        return mOnlyFullScreen;
    }


    public void setOritentionType(int type) {
        this.oritentionType = type;
    }

    public void setOrientationListener(OrientationListener listener) {
        mlistener = listener;
    }

    public void setWindowLightListener(LightListener listener) {
        mLightListener = listener;
    }

    public void setCloseListener(CloseListener listener) {
        mCloseListener = listener;
    }

    //设置亮度
    public void setLight(float brightness) {
        this.mBrightness = brightness;
    }

    //音量，亮度,快进控件是否可用
    public void setVolumeLightForwardBoxWidgetUsable(boolean bol) {
        this.mWidgetUsable = bol;
    }


    //初始化X,Y轴手势偏移量
    public void initSlideOffset() {
        int navBarHeight = ScreenUtil.getNavigationBarHeight(mActivity);
        mSlideXOffset = navBarHeight;
        mSlideYOffset = (int) mActivity.getResources().getDimension(R.dimen.default_video_controller_bottom_height);
    }

    /**
     * 获取当前播放进度
     */
    public int getCurrentPositionWhenPlaying() {
        if (mPlayer == null) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();

        return position;
    }


    //快进跳转到特定位置
    private void seekToPosition() {
        if (mPlayer == null) return;
        mPlayer.seekTo(mSeekTimePosition);
        //如果是暂停状态转化为播放状态
        if (!mPlayer.isPlaying()) {
            mPlayer.start();
            updatePausePlay();
        }
    }


    private void restForwardBox() {
        mForwardTotalTxt.setText("00:00/00:00");
        mForwardTxt.setText("0");
    }


    @Override
    public void slideVolume(float deltaY) {
        if (mAnchor == null) return;
        float pecentVolume = 3 * deltaY / videoHeight;
        int volume = (int) (pecentVolume * mMaxVolume) + mVolume;
        if (volume > mMaxVolume)
            volume = mMaxVolume;
        else if (volume < 0)
            volume = 0;
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
        int seekPecent = volume * 100 / mMaxVolume;
        mVolumeProgress.setProgress(seekPecent);
    }

    @Override
    public void slideLight(float deltaY) {
        if (mAnchor == null) return;
        float pecentBrightness = 3 * deltaY / videoHeight;
        pecentBrightness = mBrightness + pecentBrightness;
        Log.d(TAG, "slideLight: 移动 " + mBrightness);
        if (pecentBrightness > 1.0f) {
            pecentBrightness = 1.0f;
        } else if (pecentBrightness < 0.01f) {
            pecentBrightness = 0.01f;
        }

        mLightListener.setWindowLight(pecentBrightness);
        int seekPecent = (int) (pecentBrightness * 100);
        mBrightnessProgress.setProgress(seekPecent);
    }

    @Override
    public void slideProgress(float deltaX) {
        if (mPlayer == null) return;
        int duration = mPlayer.getDuration();
        int forwardDuration = (int) (deltaX * duration / mScreenWidth / 10);
        mSeekTimePosition = mPosition + forwardDuration;
        if (mSeekTimePosition > duration)
            mSeekTimePosition = duration;
        String seekToTime = stringForTime(mSeekTimePosition);
        String totalTime = stringForTime(duration);
        String forwardStr = forwardDuration / 1000 % 60 + "秒";
        forwardStr = deltaX > 0 ? "+" + forwardStr : "-" + forwardStr;
        mForwardTxt.setText(forwardStr);
        mForwardTotalTxt.setText(seekToTime + "/" + totalTime);
    }

    public void showVedioThumbnail(Bitmap bitmap) {
        try {
            if (!mPlayer.isPlaying() && bitmap != null
                    && !bitmap.isRecycled()) {
                mThumImg.setImageBitmap(bitmap);
                mThumImg.setVisibility(VISIBLE);
                mShowThumbnail = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isShowThumbnail() {
        return mShowThumbnail;
    }

    //隐藏封面图
    public void disMissVedioThumbnail() {
        mShowThumbnail = false;
        mThumImg.setVisibility(GONE);
    }

}
