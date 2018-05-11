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

import butterknife.BindView;

public class RecordActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
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

    private int mConfigResult;
    private int mConfigTime;
    private String mFilePath;
    private RecordRun mRun;

    private boolean isPlay = true;
    private AudioManager mAudioManager;
    private String path = "";
    private AudioRecord mRecord;
    private RecordUtil recordUtil;
    private MediaPlayer mMediaPlayer = null;

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
        addData(mFatherName,super.mName);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        File f = FileUtil.createRootDirectory(mFilePath);
        File file = FileUtil.mkDir(f);
        path = file.getPath();
        LogUtil.d("path:"+path);

        recordUtil = RecordUtil.getInstance(path);
        recordUtil.setHandle(mHandler);

        mRun = new RecordRun();
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    int v = (int) msg.obj;
                    mVolumeView.setVolume(v);
                    break;
                case 1002:
                    playRecord(recordUtil.getCurrentFilePath());
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
        mHandler.removeCallbacks(mRun);
        if (mRecord!=null){
            mRecord.release();
            mRecord = null;
        }
        if (mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void deInit(int results){
        if (mDialog.isShowing())mDialog.dismiss();
        updateData(mFatherName,super.mName,results);
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    private void deInit(int results,String reason){
        if (mDialog.isShowing())mDialog.dismiss();
        updateData(mFatherName,super.mName,results,reason);
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(result);
        }
    }

    public void onStartRecord(View view) {
        if (isPlay){
            isPlay = false;
            mStart.setText(R.string.record_completed);

            //start record
            recordUtil.prepareAudio();

        }else {
            isPlay = true;
            mStart.setText(R.string.record_start);
            //play record

            recordUtil.release();

            mHandler.sendEmptyMessageDelayed(1002,2000);
        }
    }

    private void playRecord(String filePath){
        LogUtil.d("play record:"+filePath);
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(filePath);
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (Exception e) {
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
    }

    public class RecordRun implements Runnable{
        private int mBufferSize;
        private final int SAMPLE_RATE = 8000;
        private final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
        private final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
        private final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
        private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;

        public RecordRun(){
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
            }catch (Exception e){
                e.printStackTrace();
                sendErrorMsgDelayed(mHandler,e.getMessage());
            }
            mHandler.post(this);
        }
    }

}
