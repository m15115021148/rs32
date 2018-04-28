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

        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");

        addData(mFatherName,super.mName);

        init();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCpu();
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
    }

    private void calculateCpuUsage() {
        new Thread(new Runnable() {
            public void run() {
                float f = readUsage();
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
            }
            return 0.0F;
        } catch (Exception localException1) {
            localException1.printStackTrace();
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
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        updateData(mFatherName,super.mName,result);
        deInit(result);
    }
}
