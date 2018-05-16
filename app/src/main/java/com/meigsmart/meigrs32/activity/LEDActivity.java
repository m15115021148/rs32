package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.application.MyApplication;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import butterknife.BindView;

public class LEDActivity extends BaseActivity implements View.OnClickListener
        ,PromptDialog.OnPromptDialogCallBack,Runnable{
    private LEDActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.leds)
    public TextView mLeds;
    @BindView(R.id.flag)
    public TextView mFlag;

    private static String BRIGHTNESS_GREEN =  "/sys/class/leds/green/brightness";//"/sys/devices/soc.0/gpio-leds.72/leds/green/brightness";//
    private static String BRIGHTNESS_RED =  "/sys/class/leds/red/brightness";//"/sys/devices/soc.0/78b9000.i2c/i2c-5/5-0045/leds/red/brightness";//
    private static String BRIGHTNESS_BLUE =  "/sys/class/leds/blue/brightness";//"/sys/devices/soc.0/78b9000.i2c/i2c-5/5-0045/leds/red/brightness";//
    final byte[] ON = { '2','5','5' };
    final byte[] OFF = { '0' };
    private String[] colorTitle = {"RED","GREEN","BLUE","GREEN"};
    private int TIME_VALUES = 2000;
    private int currPosition = 0;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_led;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_led);

        mConfigResult = getResources().getInteger(R.integer.leds_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mFlag.setText(R.string.start_tag);
        mHandler.postDelayed(this,TIME_VALUES);

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
            switch (msg.what){
                case 1001:
                    if (MyApplication.NAME.equals(mFatherName))deInit(SUCCESS);
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
        mHandler.removeCallbacks(mRun);
        mHandler.removeCallbacks(this);
        mHandler.removeMessages(1001);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
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

    @Override
    public void run() {
        mFlag.setText(R.string.led_flag);
        setLedColors(currPosition);
        mLeds.setText(colorTitle[currPosition]);
        currPosition++;
        if (currPosition == 4){
            currPosition = 0;
        }
        mHandler.postDelayed(this,TIME_VALUES);
    }

    private void setLedColors(int color) {
        boolean red = false;
        boolean green = false;
        boolean blue = false;
        switch (color) {
            case 0:
                red = true;
                break;
            case 1:
                green = true;
                break;
            case 2:
                blue = true;
                break;
            case 3:
                green = true;
                break;
            default:
                break;
        }
        try {
            FileOutputStream fRed = new FileOutputStream(BRIGHTNESS_RED);
            fRed.write(red ? ON : OFF);
            fRed.close();
            FileOutputStream fGreen = new FileOutputStream(BRIGHTNESS_GREEN);
            fGreen.write(green ? ON : OFF);
            fGreen.close();
            FileOutputStream fBlue = new FileOutputStream(BRIGHTNESS_BLUE);
            fBlue.write(blue ? ON : OFF);
            fBlue.close();
        } catch (Exception e) {
            sendErrorMsg(mHandler,e.getMessage());
        }
    }
}
