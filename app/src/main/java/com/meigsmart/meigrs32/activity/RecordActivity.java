package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.service.AudioLoopbackService;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.RecordUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.view.VolumeView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import butterknife.BindView;

public class RecordActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private RecordActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.volumeView)
    public VolumeView mVolumeView;
    @BindView(R.id.start)
    public Button mStart;
    @BindView(R.id.earphone)
    public TextView mEarPhone;

    private int mConfigResult;
    private int mConfigTime;
    private String mFilePath;
//    private RecordRun mRun;
    private Runnable mRun;

    private boolean isPlay = true;
    private AudioManager mAudioManager;
    private String path = "";
    private String mAudioFilePath = "";
    private AudioRecord mRecord;

    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    private MediaRecorder mMediaRecorder = null;
    private static final String ACTION_RECORD = "com.meigsmart.mic.record";
    private static final String STATE = "state";
    private static final String STATE_RECORDING = "recording";
    private static final String STATE_IS_PLAY = "playing";
    private static final String STATE_PLAY_FINISH = "playFinish";
    private StateRecordReceiver mReceiver;
    private boolean isRecord = false;
    private EarphonePluginReceiver earphonePluginReceiver = null;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_record;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_record);

        mConfigResult = getResources().getInteger(R.integer.record_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mFilePath = getResources().getString(R.string.record_default_config_save_file_path);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(STREAM_TYPE, 15, 0);
        File f = FileUtil.createRootDirectory(mFilePath);
        File file = FileUtil.mkDir(f);
        path = file.getPath();
        LogUtil.d("path:" + path);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (isRecord){
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
//                    int v = (int) msg.obj;
                    if (mMediaRecorder!=null){
                        int ratio = mMediaRecorder.getMaxAmplitude();
                        LogUtil.d("ratio:"+ratio);
                        int db = 0;// 分贝
                        if (ratio > 1)
                            db = (int) (20 * Math.log10(ratio));
                        LogUtil.d("db:"+db);
                        mVolumeView.setVolume(ratio);
                    }
                    break;
                case 1002:
                    isRecord = false;
                    mVolumeView.setVolume(0);
                    mVolumeView.clearVolume();
                    mStart.setEnabled(false);
                    play(mAudioFilePath);
                    break;
                case 1003:
                    record();
                    break;
                case 9999:
                    File file = new File(mAudioFilePath);
                    if (file.exists()) file.delete();
                    deInit(FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    private void registerRecord(){
        mReceiver = new StateRecordReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_RECORD);
        registerReceiver(mReceiver, intentFilter);
    }

    private void registerHeadsetPlugReceiver() {
        earphonePluginReceiver = new EarphonePluginReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(earphonePluginReceiver, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerRecord();
        registerHeadsetPlugReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (earphonePluginReceiver!=null)unregisterReceiver(earphonePluginReceiver);
        if (mReceiver!=null)unregisterReceiver(mReceiver);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
        mHandler.removeCallbacks(mRun);
        if (mRecord!=null){
            mRecord.release();
            mRecord = null;
        }
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void deInit(int results) {
        if (mDialog.isShowing()) mDialog.dismiss();
        updateData(mFatherName, super.mName, results);
        Intent intent = new Intent();
        intent.putExtra("results", results);
        setResult(1111, intent);
        mContext.finish();
    }

    private void deInit(int results, String reason) {
        if (mDialog.isShowing()) mDialog.dismiss();
        updateData(mFatherName, super.mName, results, reason);
        Intent intent = new Intent();
        intent.putExtra("results", results);
        setResult(1111, intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(result);
        }
    }

    public void onStartRecord(View view) {
        if (isPlay) {
            isPlay = false;
            mStart.setText(R.string.record_completed);

            //start record
            mHandler.sendEmptyMessage(1003);

        } else {
            isRecord = false;
            isPlay = true;
            mStart.setText(R.string.record_start);
            //play record
            stopRecord();
            mStart.setEnabled(false);
            mStart.setTextColor(R.color.pop_txt_bg);

            mHandler.sendEmptyMessageDelayed(1002, 2000);
        }
    }

    private void record() {
        try {
            isRecord = true;
            String fileNameString = generalFileName();
            File file = new File(path, fileNameString);

            mMediaRecorder = new MediaRecorder();

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
            mMediaRecorder.setAudioSamplingRate(1);
            mMediaRecorder.setOutputFile(file.getAbsolutePath());
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setAudioEncodingBitRate(96000);

            mMediaRecorder.prepare();
            mMediaRecorder.start();

            mAudioFilePath = file.getAbsolutePath();
            Intent intent = new Intent();
            intent.setAction(ACTION_RECORD);
            intent.putExtra(STATE, STATE_RECORDING);
            sendBroadcast(intent);
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorMsgDelayed(mHandler, e.getMessage());
        }
    }

    public void stopRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    public void play(String filePath) {
        try {
            File file = new File(filePath);
            FileInputStream mFileInputStream = new FileInputStream(file);
            final MediaPlayer mMediaPlayer = new MediaPlayer();

            mMediaPlayer.setAudioStreamType(STREAM_TYPE);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(mFileInputStream.getFD());
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            Intent intent = new Intent();
            intent.setAction(ACTION_RECORD);
            intent.putExtra(STATE, STATE_IS_PLAY);
            sendBroadcast(intent);
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mPlayer) {
                    mPlayer.stop();
                    mPlayer.reset();
                    mPlayer.release();

                    Intent intent = new Intent();
                    intent.setAction(ACTION_RECORD);
                    intent.putExtra(STATE, STATE_PLAY_FINISH);
                    mContext.sendBroadcast(intent);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            sendErrorMsgDelayed(mHandler, e.getMessage());
        }
    }

    private String generalFileName() {
        return String.valueOf(System.currentTimeMillis()) + ".amr";
    }

    public class StateRecordReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String message = intent.getExtras().getString(STATE);
            if (message.equals(STATE_RECORDING)) {
                LogUtil.d("record is recording");
            } else if (message.equals(STATE_IS_PLAY)) {
                LogUtil.d("record is playing");
                mStart.setEnabled(false);

            } else if (message.equals(STATE_PLAY_FINISH)) {
                LogUtil.d("record is finish...........");
                mStart.setEnabled(true);
                mStart.setTextColor(R.color.black_dan);

                File file = new File(mAudioFilePath);
                if (file.exists()) file.delete();
            }
        }
    }

    public class EarphonePluginReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent earphoneIntent) {
            if (earphoneIntent.hasExtra("state")) {
                int st = earphoneIntent.getIntExtra("state", 0);
                if (st ==  1) {
                    mEarPhone.setVisibility(View.VISIBLE);
                    mStart.setEnabled(false);
                } else if (st == 0) {
                    mEarPhone.setVisibility(View.GONE);
                    mStart.setEnabled(true);
                }
            }
        }
    }

    public class RecordRun implements Runnable {
        private int mBufferSize;
        private final int SAMPLE_RATE = 8000;
        private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
        private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
        private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
        private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;

        public RecordRun() {
            mBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
            int size = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

            if (size > mBufferSize) {
                mBufferSize = size;
            }

            AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, mBufferSize);
            mRecord = record;
        }

        @Override
        public void run() {
            int mode = mAudioManager.getMode();
            mAudioManager.setMode(AudioManager.MODE_IN_CALL);
            int valume = mAudioManager.getStreamVolume(STREAM_TYPE);
            mAudioManager.setStreamVolume(STREAM_TYPE, 6, 0);
            try {
                mRecord.startRecording();
                short[] buff = new short[mBufferSize / (Short.SIZE / 8)];
                int length = mRecord.read(buff, 0, buff.length);

                if (length < 0) {
                    return;
                }
                int maxAmplitude = 0;
                for (int i = length - 1; i >= 0; i--) {
                    if (buff[i] > maxAmplitude) {
                        maxAmplitude = buff[i];
                    }
                }
                maxAmplitude = maxAmplitude / 5;

                Message msg = mHandler.obtainMessage();
                msg.what = 1001;
                msg.obj = maxAmplitude;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorMsgDelayed(mHandler, e.getMessage());
            }
            mHandler.post(this);
        }
    }

}
