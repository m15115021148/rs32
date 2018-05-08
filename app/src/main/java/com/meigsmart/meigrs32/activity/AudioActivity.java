package com.meigsmart.meigrs32.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.service.MusicService;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.File;
import java.text.SimpleDateFormat;

import butterknife.BindView;

public class AudioActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack, Runnable{
    private AudioActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.image)
    public ImageView mImg;
    @BindView(R.id.musicTime)
    public TextView mCurrTime;
    @BindView(R.id.musicTotal)
    public TextView mTotalTime;
    @BindView(R.id.musicSeekBar)
    public SeekBar mSb;

    private SimpleDateFormat time = new SimpleDateFormat("mm:ss");
    private MusicService musicService;
    public Handler handler = new Handler();
    private ObjectAnimator animator;
    private Intent intentMusic;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;

    private String mCustomFilePath ;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_audio;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_audio);

        mConfigResult = getResources().getInteger(R.integer.audio_auto_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        isCustomPath = getResources().getBoolean(R.bool.audio_default_config_is_user_custom_path);
        mCustomPath = getResources().getString(R.string.audio_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.audio_default_config_custom_file_name);
        LogUtil.d("mConfigResult:" + mConfigResult +
                " mConfigTime:" + mConfigTime+
                " mCustomPath:" + mCustomPath+
                " mCustomFileName:"+mCustomFileName+
                " isCustomPath:"+isCustomPath);
        mCustomFilePath = getCustomFilePath();

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        bindServiceConnection();

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    private String getCustomFilePath(){
        if (isCustomPath){
            if (!TextUtils.isEmpty(mCustomPath) && !TextUtils.isEmpty(mCustomFileName)){
                File file = FileUtil.createRootDirectory(mCustomPath);
                File f = new File(file.getPath(),mCustomFileName);
                if (f.exists()){
                    return f.getPath();
                }else{
                    ToastUtil.showBottomShort("the file is not exists");
                    mHandler.sendEmptyMessageAtTime(1002,2000);
                }
            }else {
                ToastUtil.showBottomShort("the file path is not null");
                mHandler.sendEmptyMessageAtTime(1002,2000);
            }
        }
        return null;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    deInit(SUCCESS);
                    break;
                case 1002:
                    deInit(FAILURE);
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        if (musicService!=null)musicService.stop();
        if (intentMusic!=null)stopService(intentMusic);
        unbindService(serviceConnection);
        handler.removeCallbacks(this);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void bindServiceConnection() {
        intentMusic = new Intent(this, MusicService.class);
        startService(intentMusic);
        bindService(intentMusic, serviceConnection, this.BIND_AUTO_CREATE);
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicService = ((MusicService.MyBinder) (service)).getService(isCustomPath,mCustomFilePath);
            handler.post(AudioActivity.this);
            mTotalTime.setText(time.format(musicService.mediaPlayer.getDuration()));
            rotationImg();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
        }
    };

    private void deInit(int results){
        if (mDialog.isShowing())mDialog.dismiss();
        updateData(mFatherName,super.mName,results);
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }

    @Override
    public void run() {
        if (musicService.isPlay){
            mCurrTime.setText(time.format(musicService.mediaPlayer.getCurrentPosition()));
            mSb.setProgress(musicService.mediaPlayer.getCurrentPosition());
            mSb.setMax(musicService.mediaPlayer.getDuration());
            mTotalTime.setText(time.format(musicService.mediaPlayer.getDuration()));
            handler.postDelayed(this, 200);
        }else {
            handler.removeCallbacks(this);
            unbindService(serviceConnection);
            if (intentMusic!=null)stopService(intentMusic);
            deInit(SUCCESS);
        }
    }

    private void rotationImg(){
        animator = ObjectAnimator.ofFloat(mImg, "rotation", 0f, 360.0f);
        animator.setDuration(10000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(-1);
        animator.start();
    }
}
