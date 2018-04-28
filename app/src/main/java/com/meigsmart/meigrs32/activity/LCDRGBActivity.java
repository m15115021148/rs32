package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class LCDRGBActivity extends BaseActivity implements View.OnClickListener ,
        PromptDialog.OnPromptDialogCallBack ,Runnable{
    private LCDRGBActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    private int[] ids = {
            Color.parseColor("#FF0000"),Color.parseColor("#00FF00"),Color.parseColor("#0000FF"),
            Color.parseColor("#888888"),Color.parseColor("#000000"),Color.parseColor("#FFFFFF"),
    };
    private int TIME_VALUES = 2000;
    private int currPosition = 0;
    private int count = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_lcd_rgb;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_lcd_rgb);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mHandler.post(this);
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

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }

    @Override
    public void run() {
        mLayout.setBackgroundColor(ids[currPosition]);
        currPosition++;
        if (currPosition == 6){
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
