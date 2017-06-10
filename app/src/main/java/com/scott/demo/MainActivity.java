package com.scott.demo;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;

import com.lib.media.constant.Constant;
import com.lib.media.controller.ImAudioMediaController;
import com.lib.media.ijkplayer.IMediaController;
import com.lib.media.ijkplayer.IjkVideoView;
import com.lib.media.listener.ICloseListener;
import com.lib.media.listener.ILightListener;
import com.lib.media.listener.IOrientationListener;
import com.lib.media.util.LightUtil;
import com.lib.media.util.OrientationUtil;

public class MainActivity extends AppCompatActivity implements ICloseListener {
    IjkVideoView videoView;
    IMediaController videoMediaController;
    IMediaController audioMediaController;
    IMediaController imAudioMediaController;
    private AppCompatImageView img;
    LightUtil lightUtil;
    OrientationUtil orientationUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        AppCompatButton btn = (AppCompatButton) findViewById(R.id.btn);
        AppCompatButton btn1 = (AppCompatButton) findViewById(R.id.change_btn);
        img = (AppCompatImageView) findViewById(R.id.img_cover);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                img.setImageBitmap(videoView.getCovreBitmap());
            }
        });
        setSupportActionBar(toolbar);


        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //videoView.release(true);
                //String url1 = "http://o6wf52jln.bkt.clouddn.com/演员.mp3";
                //videoView.setVideoURI(Uri.parse(url1));
                //FrameLayout fragment = (FrameLayout) videoView.getParent();
                //fragment.removeViewAt(1);
                videoView.restorAll();
                initAudio();
            }
        });

        //IjkMediaPlayer.loadLibrariesOnce(null);
        //IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        videoView = (IjkVideoView) findViewById(R.id.ijkPlayer);


        //git initImAudio();
        //initVideo();
        initAudio();

        //本地格式
        //String url = "/storage/emulated/0/Download/test.h264";
        //String url = "/storage/emulated/0/DCIM/Camera/haohao.mp4";

        //videoView.setAspectRatio(1);
        //videoView.setCanSeekBack(false);
        //videoView.setCanSeekForward(false);
        //videoView.start();
    }


    private void initVideo() {
        //videoMediaController = new VideoMediaController(this);
        //videoView.setMediaController((VideoMediaController)videoMediaController);
        //lightUtil = new LightUtil(MainActivity.this, videoMediaController);
        //orientationUtil = new OrientationUtil(MainActivity.this, videoMediaController);
        //((VideoMediaController)videoMediaController).setOrientationListener(this);
        //videoMediaController.setWindowLightListener(this);
        //videoMediaController.setCloseListener(this);

        videoView.setMediaControllerInfo(MainActivity.this, Constant.MEDIA_TYPE_VIDEO, this);
        //videoView.isCharge(true);
        //videoView.setMaxPlayTime(5 * 60 * 1000);
        String url = "http://cdn.course1.1dabang.cn/087/all/index.m3u8";
        videoView.setVideoURI(Uri.parse(url));
    }


    private void initAudio() {
        videoView.setMediaControllerInfo(MainActivity.this, Constant.MEDIA_TYPE_AUDIO, this);
        //audioMediaController = new AudioMediaController(this);
        // audioMediaController.setCloseListener(this);
        //videoView.setMediaController(audioMediaController);
        //videoView.setMediaType(Constant.MEDIA_TYPE_AUDIO);
        //String url = "http://o6wf52jln.bkt.clouddn.com/演员.mp3";
        //String url = "/storage/emulated/0/Android/data/com.zbsd.ydb/files/recoder/1497053184182.amr";
        String url = "https://cdn-files.1dabang.cn/936523B4-361B-4D8A-B476-89249AA47743.amr";
        //String url = "http://medbigbang-1.oss-cn-beijing.aliyuncs.com/936523B4-361B-4D8A-B476-89249AA47743.amr";
        videoView.setVideoURI(Uri.parse(url));
        videoView.start();
    }

    private void initImAudio() {
        //imAudioMediaController = new ImAudioMediaController(this);
        //imAudioMediaController.setDuration(8000);
        //videoView.setMediaController(imAudioMediaController);
        //videoView.setMediaType(Constant.MEDIA_TYPE_IM_AUDIO);
        String url = "http://o6wf52jln.bkt.clouddn.com/演员.mp3";
        //String url = "https://cdn-files.1dabang.cn/936523B4-361B-4D8A-B476-89249AA47743.amr";
        //String url = "http://medbigbang-1.oss-cn-beijing.aliyuncs.com/936523B4-361B-4D8A-B476-89249AA47743.amr";

        //String url = "/storage/emulated/0/Android/data/com.zbsd.ydb/files/recoder/1497053184182.amr";

        videoView.setMediaControllerInfo(MainActivity.this, Constant.MEDIA_TYPE_IM_AUDIO, this);
        videoView.setVideoURI(Uri.parse(url));
    }


    @Override
    protected void onResume() {
        super.onResume();
        videoView.onResume();
        //img.setImageBitmap(videoView.getCovreBitmap());
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.onPause();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        videoView.onConfigChange(newConfig);
    }

    @Override
    public void closed() {
        videoView.release(true);
    }


    @Override
    public void onBackPressed() {
        Log.d("TAG", "onBackPressed: 返回键111111");
        videoView.restoreToNormal();
        //super.onBackPressed();
    }
}
