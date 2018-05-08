package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.cpuservice.CpuService1;
import com.meigsmart.meigrs32.cpuservice.CpuTest;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.RandomAccessFile;

import butterknife.BindView;

public class CpuActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private CpuActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private CpuTest mCpuTest;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;
    private int count = 0;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cpu;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_cpu);
        mDialog.setCallBack(this);

        mConfigResult = getResources().getInteger(R.integer.cpu_default_config_threshold);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName,super.mName);

        init();

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    mHandler.sendEmptyMessage(1003);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCpu();
        mHandler.removeCallbacks(mRun);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    calculateCpuUsage();
                    break;
                case 1002:
                    useCpu();
                    break;
                case 1003:
                    if (count>=10){
                        deInit(SUCCESS);
                    }else{
                        deInit(FAILURE,"CPU usage is less than 70%");
                    }
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    public void onClick(View v) {
        if (v == mBack){
            stopCpu();
            mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void init(){
        this.mCpuTest = new CpuTest(this,mHandler);
        this.mCpuTest.start();
    }

    private void stopCpu(){
        if (this.mCpuTest != null) {
            this.mCpuTest.stop();
        }
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
    }

    private void calculateCpuUsage() {
        new Thread(new Runnable() {
            public void run() {
                float f = readUsage();
                if ( f >= mConfigResult )count++;
                String str = "Cpu Usage : " + f + "%";
                updateCpuInfoText(str);
                mHandler.sendEmptyMessageDelayed(1001, 1000L);
            }
        }, "CalCpu").start();
    }

    private float readUsage() {
        try {
            RandomAccessFile localObject1 = new RandomAccessFile("/proc/stat", "r");
            String[] localObject2 = localObject1.readLine().split(" ");
            long l1 = Long.parseLong(localObject2[5]);
            long l2 = Long.parseLong(localObject2[2]) + Long.parseLong(localObject2[3]) + Long.parseLong(localObject2[4]) + Long.parseLong(localObject2[6]) + Long.parseLong(localObject2[7]) + Long.parseLong(localObject2[8]);
            try {
                Thread.sleep(100L);
                localObject1.seek(0L);
                String str = localObject1.readLine();
                localObject1.close();
                String[] str1 = str.split(" ");
                long l3 = Long.parseLong(str1[5]);
                long l4 = Long.parseLong(str1[2]) + Long.parseLong(str1[3]) + Long.parseLong(str1[4]) + Long.parseLong(str1[6]) + Long.parseLong(str1[7]) + Long.parseLong(str1[8]);
                return (float) (Long.valueOf(100L * (l4 - l2) / (l4 + l3 - (l2 + l1))).longValue());
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorMsg(mHandler,e.getMessage());
            }
            return 0.0F;
        } catch (Exception localException1) {
            localException1.printStackTrace();
            sendErrorMsg(mHandler,localException1.getMessage());
        }
        return 0.0F;
    }

    private void updateCpuInfoText(String paramString) {
        Intent localIntent = new Intent(this, CpuService1.class);
        localIntent.putExtra("update", true);
        localIntent.putExtra("level", paramString);
        startService(localIntent);
    }

    private void useCpu() {
        new Thread(new Runnable() {
            public void run() {
                int i = 500000000;
                for (; ; ) {
                    if (i == 1) {
                        mHandler.sendEmptyMessageDelayed(1002, 500L);
                        break;
                    }
                    i -= 1;
                }
            }
        }, "UseCpu").start();
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
}
