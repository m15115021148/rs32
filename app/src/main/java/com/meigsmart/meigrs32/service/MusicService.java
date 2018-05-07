package com.meigsmart.meigrs32.service;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;

/**
 * Created by chenMeng on 2018/1/30.
 */

public class MusicService extends Service {
    public MediaPlayer mediaPlayer;
    public boolean isPlay = false;

    public MusicService() {
    }

    private MusicService getInstance(boolean isCustom,String customFilePath){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.reset();
        if (isCustom){
            try {
                mediaPlayer.setDataSource(customFilePath);
            } catch (IOException e) {
                stop();
                e.printStackTrace();
            }
        }else{
            setDefaultDataSource();
        }
        mediaPlayer.setLooping(false);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                isPlay = true;
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                isPlay = true;
                mp.seekTo(0);
                mp.start();
            }
        });
        return MusicService.this;
    }

    private void setDefaultDataSource(){
        try {
            AssetFileDescriptor afd = this.getAssets().openFd("music.mp3");
            mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MyBinder binder = new MyBinder();

    public class MyBinder extends Binder {
        public MusicService getService(boolean isCustom,String customFilePath) {
            return getInstance(isCustom, customFilePath);
        }
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
