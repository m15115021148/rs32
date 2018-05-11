package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;

public class SpeakerActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SpeakerActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.flag)
    public TextView mFlag;

    private AudioManager mManager;

    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;
    private MediaPlayer mediaPlayer;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_speaker;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_speaker);

        isCustomPath = getResources().getBoolean(R.bool.speaker_default_config_is_use_custom_path);
        mCustomPath = getResources().getString(R.string.speaker_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.speaker_default_config_custom_file_name);
        LogUtil.d("isCustomPath:"+isCustomPath+" mCustomPath:"+mCustomPath);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mHandler.sendEmptyMessageDelayed(1001, 2000);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    init(isCustomPath);
                    break;
                case 1002:
                    deInit(SUCCESS);
                    break;
                case 9999:
                    deInit(FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    private void init(boolean isCustom){
        mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mediaPlayer = new MediaPlayer();
        mManager.setMicrophoneMute(false);
        mManager.setSpeakerphoneOn(true);//使用扬声器外放，即使已经插入耳机
        setVolumeControlStream(AudioManager.STREAM_MUSIC);//控制声音的大小
        mManager.setMode(AudioManager.STREAM_MUSIC);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.reset();
        setDataSource(isCustom);
        mediaPlayer.setLooping(false);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.seekTo(0);
                mp.start();
            }
        });
    }

    private void setDataSource(boolean isCustom){
        try {
            if (isCustom){
                if (TextUtils.isEmpty(mCustomPath) && TextUtils.isEmpty(mCustomFileName)){
                    sendErrorMsgDelayed(mHandler,"the custom file path is null");
                    return;
                }
                File file = FileUtil.createRootDirectory(mCustomPath);
                File file1 = FileUtil.mkDir(file);
                File f = new File(file1.getPath(),mCustomFileName);

                mediaPlayer.setDataSource(f.getPath());
            }else {
                AssetFileDescriptor afd = this.getAssets().openFd("ring.mp3");
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
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
}
