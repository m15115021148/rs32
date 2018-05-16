package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class StorageCardActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private StorageCardActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.sdState)
    public TextView mSDState;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;
    private StorageManager mManager;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_storage_card;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_storage_card);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mHandler.sendEmptyMessageDelayed(1001, 2000);

        mManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);

        long  l = Environment.getDataDirectory().getTotalSpace();
        long s = Environment.getStorageDirectory().getFreeSpace();
        long maxFileSize = Environment.getDataDirectory().getUsableSpace();
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), l));
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), s));
        LogUtil.w(Formatter.formatFileSize(getBaseContext(), maxFileSize));

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;

                ///if (isStartTest)mHandler.sendEmptyMessage(1003);
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
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;
                    mManager.registerListener(mListener);

                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        mSDState.setText(getResult(getRomSize(),getTotalMemory(),getSDCardMemory()));
                    }else {
                        mSDState.setText(R.string.sd_state_flag);
                    }
                    break;
                case 1002:
                    String newState = (String) msg.obj;
                    if (newState.equals(Environment.MEDIA_MOUNTED)){//sd use
                        mSDState.setText(getResult(getRomSize(),getTotalMemory(),getSDCardMemory()));
                    }else if (newState.equals(Environment.MEDIA_BAD_REMOVAL)){//remove
                        mSDState.setText(R.string.sd_state_flag);
                    }
                    break;
                case 9999:
                    deInit(FAILURE, msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(9999);
    }

    private String getResult(String rom,String ram,String sd){
        StringBuffer sb = new StringBuffer();
        sb.append("ROM size:").append(" ").append(rom).append("\n");
        sb.append("RAM size:").append(" ").append(ram).append("\n");
        sb.append("Storage SD size:").append(" ").append(sd).append("\n");
        return sb.toString();
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

    private StorageEventListener mListener = new StorageEventListener(){
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            Message msg = mHandler.obtainMessage();
            msg.what = 1002;
            msg.obj = newState;
            mHandler.sendMessage(msg);
        }
    };

    /**
     * ram
     * @return
     */
    private String getTotalMemory() {
        long l = 0;
        if (mManager!=null){
            l = mManager.getPrimaryStorageSize();
        }
        return Formatter.formatFileSize(getBaseContext(), l);
    }

    /**
     * sd
     * @return
     */
    public String getSDCardMemory() {
        long[] sdCardInfo=new long[2];
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            File sdcardDir = Environment.getExternalStorageDirectory();
            StatFs sf = new StatFs(sdcardDir.getPath());
            long bSize = sf.getBlockSize();
            long bCount = sf.getBlockCount();
            long availBlocks = sf.getAvailableBlocks();

            sdCardInfo[0] = bSize * bCount;//总大小
            sdCardInfo[1] = bSize * availBlocks;//可用大小
        }
        return Formatter.formatFileSize(getBaseContext(), sdCardInfo[0]);
    }

    public String getRomSize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        long l =  totalBlocks * blockSize;
        return Formatter.formatFileSize(getBaseContext(), l);
    }

}
