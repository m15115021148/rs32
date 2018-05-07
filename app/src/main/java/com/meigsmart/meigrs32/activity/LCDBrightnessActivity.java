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

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
        curBackground = Settings.System.getInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 100);
        mHandler.post(this);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

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
        pm.setBacklightBrightness(100);
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
        mValues.setText(String.valueOf(curBackground));
        pm.setBacklightBrightness(curBackground);
        Settings.System.putInt(getApplication().getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, curBackground);
        mHandler.postDelayed(this,40);
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(this);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(this);
        super.onPause();
    }

}
