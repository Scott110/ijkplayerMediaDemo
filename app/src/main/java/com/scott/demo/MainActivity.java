package com.scott.demo;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.lib.media.constant.Constant;
import com.lib.media.controller.AudioMediaController;
import com.lib.media.controller.VideoMediaController;
import com.lib.media.ijkplayer.AndroidMediaController;
import com.lib.media.ijkplayer.IjkVideoView;
import com.lib.media.listener.CloseListener;
import com.lib.media.listener.LightListener;
import com.lib.media.listener.OrientationListener;
import com.lib.media.util.LightUtil;
import com.lib.media.util.OrientationUtil;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity implements LightListener, OrientationListener, CloseListener {
    IjkVideoView videoView;
    VideoMediaController videoMediaController;
    AudioMediaController audioMediaController;
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
                videoView.release(true);
                String url1 = "http://o6wf52jln.bkt.clouddn.com/演员.mp3";
                videoView.setVideoURI(Uri.parse(url1));
            }
        });

        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        videoView = (IjkVideoView) findViewById(R.id.ijkPlayer);
        videoMediaController = new VideoMediaController(this);


        videoView.setMediaController(videoMediaController);
        lightUtil = new LightUtil(MainActivity.this, videoMediaController);
        orientationUtil = new OrientationUtil(MainActivity.this, videoMediaController);
        videoMediaController.setOrientationListener(this);
        videoMediaController.setWindowLightListener(this);
        videoMediaController.setCloseListener(this);
        String url = "http://cdn.course1.1dabang.cn/087/all/index.m3u8";

        audioMediaController = new AudioMediaController(this);
        audioMediaController.setCloseListener(this);
        //videoView.setMediaController(audioMediaController);
        //videoView.setMediaType(Constant.MEDIA_TYPE_AUDIO);
        //String url = "http://o6wf52jln.bkt.clouddn.com/演员.mp3";
        //String url = "http://medbigbang-1.oss-cn-beijing.aliyuncs.com/936523B4-361B-4D8A-B476-89249AA47743.amr";

        //本地格式
        //String url = "/storage/emulated/0/Download/test.h264";
        //String url = "/storage/emulated/0/DCIM/Camera/haohao.mp4";
        videoView.setVideoURI(Uri.parse(url));
        videoView.setAspectRatio(1);
        //videoView.setCanSeekBack(false);
        //videoView.setCanSeekForward(false);
        //videoView.start();
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
        //videoMediaController.onConfigurationChanged(newConfig);
    }

    @Override
    public void initWindowLight() {
        lightUtil.initLight();

    }

    @Override
    public void getWindowLight() {
        lightUtil.setControllerLight();
    }

    @Override
    public void setWindowLight(float light) {
        lightUtil.setLight(light);
    }

    @Override
    public void setScreenOrientation(int orientation) {
        orientationUtil.setScreenOrientation(orientation);
    }

    @Override
    public void closed() {
        videoView.release(true);
    }
}
