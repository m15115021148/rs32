package com.meigsmart.meigrs32.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder.AudioSource;
import android.os.IBinder;
import android.os.RemoteException;

public class AudioLoopbackService extends Service {
    public static final String ACTION_VOLUME_UPDATED = "com.meigsmart.function.service.VOLUME_UPDATED";

    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private final int SAMPLE_RATE = 8000;
    private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    public final static int STATE_IDEL = 0;
    public final static int STATE_RUNNING = 1;
    public final static int STATE_STOPPING = 2;
    public final static int STATE_STOPPED = 3;

    private int mState = STATE_STOPPED;
    private AudioManager mAudioManager;
    private int mBufferSize;
    public final static String ACTION_STOP = "com.meigsmart.function.service.stop";

    public boolean startLoopbackThread() {

        if (mState != STATE_STOPPED) {
            return false;
        }

        try {
            AudioLoopbackThread thread = new AudioLoopbackThread();
            thread.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean stopLoopbackThread() {
        if (mState == STATE_RUNNING) {
            mState = STATE_STOPPING;

            for (int i = 0; i < 10 && mState != STATE_STOPPED; i++) {
                try {
                    synchronized (this) {
                        wait(100);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        return true;
    }

    @Override
    public void onCreate() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        stopLoopbackThread();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (ACTION_STOP.equals(intent.getAction())) {
            stopLoopbackThread();
            return super.onStartCommand(intent, flags, startId);
        }
        startLoopbackThread();
        return super.onStartCommand(intent, flags, startId);
    }

    private class AudioLoopbackThread extends Thread {
        private AudioRecord mRecord;
        private AudioTrack mTrack;

        public AudioLoopbackThread() {
            super();

            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
            int size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

            if (size > mBufferSize) {
                mBufferSize = size;
            }

            AudioRecord record = new AudioRecord(AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, mBufferSize);
            AudioTrack track = new AudioTrack(STREAM_TYPE, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, size, AudioTrack.MODE_STREAM);

            mRecord = record;
            mTrack = track;
        }

        public void run() {

            mState = STATE_RUNNING;

            int mode = mAudioManager.getMode();
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            //mAudioManager.setMode(RecordUtil.MODE_NORMAL);
            int valume = mAudioManager.getStreamVolume(STREAM_TYPE);
            mAudioManager.setStreamVolume(STREAM_TYPE, 6, 0);
            try {
                mRecord.startRecording();
                mTrack.play();
                short[] buff = new short[mBufferSize / (Short.SIZE / 8)];
                Intent intent = new Intent(ACTION_VOLUME_UPDATED);
                while (mState == STATE_RUNNING) {
                    int length = mRecord.read(buff, 0, buff.length);
                    if (length < 0) {
                        break;
                    }

                    int maxAmplitude = 0;

                    for (int i = length - 1; i >= 0; i--) {
                        if (buff[i] > maxAmplitude) {
                            maxAmplitude = buff[i];
                        }
                    }

                    //Intent intent = new Intent(ACTION_VOLUME_UPDATED);
                    //intent.putExtra("volume", maxAmplitude);
                    //sendBroadcast(intent);
                    length = mTrack.write(buff, 0, length);
                    maxAmplitude = maxAmplitude / 5;
                    intent.putExtra("volume", maxAmplitude);
                    sendBroadcast(intent);
                    if (length < 0) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mTrack.release();
                mRecord.release();
                mAudioManager.setStreamVolume(STREAM_TYPE, valume, 0);
                mAudioManager.setMode(mode);
                //mAudioManager.abandonAudioFocusForCall();
                mState = STATE_STOPPED;

            }

        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
}
