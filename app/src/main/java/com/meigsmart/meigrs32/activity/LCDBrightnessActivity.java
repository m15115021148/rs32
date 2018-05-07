package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class LCDBrightnessActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack,Runnable{
    private LCDBrightnessActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.values)
    public TextView mValues;
    private PowerManager pm;
    private int curBackground = 100;
    private boolean isAdd = true;
    private int background = 100;
    @BindView(R.id.flag)
    public TextView mFlag;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_lcd_brightness;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_lcd_brightness);

        mConfigResult = getResources().getInteger(R.integer.lcd_brightness_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        curBackground = Settings.System.getInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 100);
        background = curBackground;

        mFlag.setText(R.string.start_tag);
        mHandler.postDelayed(this,2000);

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

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            deInit(SUCCESS);
        }
    };

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

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }

    @Override
    public void run() {
        if (isAdd){
            curBackground++;
        }else{
            curBackground--;
        }

        if (curBackground == 255){
            isAdd = false;
        }
        if (curBackground == 1){
            isAdd = true;
        }
        mFlag.setText(R.string.brightness_title);
        mValues.setText(String.valueOf(curBackground));
        pm.setBacklightBrightness(curBackground);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, curBackground);
        mHandler.postDelayed(this,40);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        pm.setBacklightBrightness(background);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, background);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(this);
        super.onPause();
    }

}
