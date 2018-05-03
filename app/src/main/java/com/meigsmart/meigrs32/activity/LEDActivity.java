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

    private static String green =  "/sys/class/leds/green/brightness";//"/sys/devices/soc.0/gpio-leds.72/leds/green/brightness";//
    private static String red =  "/sys/class/leds/red/brightness";//"/sys/devices/soc.0/78b9000.i2c/i2c-5/5-0045/leds/red/brightness";//
    private static String blue =  "/sys/class/leds/blue/brightness";//"/sys/devices/soc.0/78b9000.i2c/i2c-5/5-0045/leds/red/brightness";//
    final byte[] ON = { '1','2','7' };
    final byte[] OFF = { '0' };
    private String[] colors = {red,green,blue,green};
    private String[] colorTitle = {"RED","GREEN","BLUE","GREEN"};
    private int TIME_VALUES = 2000;
    private int currPosition = 0;
    private int count = 0;

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

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
        mHandler.postDelayed(this,TIME_VALUES);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            deInit(2);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    void enableDevice(String fileNode, boolean enable) {
        try {
            byte[] ledData = enable ? ON : OFF;
            FileOutputStream fileOutputStream = new FileOutputStream(fileNode);
            fileOutputStream.write(ledData);
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            LogUtil.e(e.getMessage());
            deInit(1);
        } catch (IOException e) {
            LogUtil.e(e.getMessage());
            deInit(1);
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

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }

    @Override
    public void run() {
        enableDevice(colors[currPosition],false);
        enableDevice(colors[currPosition],true);
        mLeds.setText(colorTitle[currPosition]);
        currPosition++;
        if (currPosition == 3){
            currPosition = 0;
            count++;
        }
        if (count == 2){
            count = 0;
            mHandler.removeCallbacks(this);
            mHandler.sendEmptyMessageDelayed(1001,TIME_VALUES);
        }
        mHandler.postDelayed(this,TIME_VALUES);
    }
}
