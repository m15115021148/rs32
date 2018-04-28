package com.meigsmart.meigrs32.activity;

import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.service.MusicService;
import com.meigsmart.meigrs32.view.PromptDialog;

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

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        bindServiceConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (intentMusic!=null)stopService(intentMusic);
        unbindService(serviceConnection);
        handler.removeCallbacks(this);
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
            musicService = ((MusicService.MyBinder) (service)).getService();
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
            deInit(2);
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
