package com.meigsmart.meigrs32.util;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;

import java.io.File;

/**
 * Created by chenMeng on 2018/5/9.
 */
public class RecordUtil {
    private MediaPlayer mediaPlayer;
    private Handler mHandler;
    private Context mContext;

    public RecordUtil(Context context, Handler handler) {
        this.mContext = context;
        this.mHandler = handler;
    }

    public void playRecordFile(File file) {
        if (file.exists()) {
            if (mediaPlayer == null) {
                Uri uri = Uri.fromFile(file);
                mediaPlayer = MediaPlayer.create(mContext, uri);
            }
            mediaPlayer.start();

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                @Override
                public void onCompletion(MediaPlayer paramMediaPlayer) {

                }
            });
        }
    }

    public void pausePlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void stopPlay() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0);
        }
    }
}
