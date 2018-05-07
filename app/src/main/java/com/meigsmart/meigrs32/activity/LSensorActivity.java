package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class LSensorActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private LSensorActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.values)
    public TextView mValues;

    private SensorManager mSensorManager;
    private Sensor mSensorLight;

    private int mConfigResult;
    private int mConfigTime;

    private boolean isMixValue = false;
    private boolean isMaxValue = false;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_l_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_l_sensor);

        mConfigResult = getResources().getInteger(R.integer.l_sensor_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        LogUtil.w(mSensorLight.getName() + "  " + mSensorLight.getType() + " " + mSensorLight.toString());
        if (mSensorLight == null) {
            LogUtil.e("LSensor is null!");
            deInit(1);
            return;
        }

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    mHandler.sendEmptyMessage(1002);
                }
                if (isMixValue && isMaxValue) deInit(2);
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    private SensorEventListener mLightListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_LIGHT) {
                return;
            }
            Message msg = mHandler.obtainMessage();
            msg.what = 1001;
            msg.obj = event.values[0];
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    float f = (float) msg.obj;
                    mValues.setText(String.valueOf(f));
                    if (f < mConfigResult) {
                        isMixValue = true;
                    }
                    if (f > mConfigResult) {
                        isMaxValue = true;
                    }
                    break;
                case 1002:
                    if (isMixValue && isMaxValue) {
                        deInit(2);
                    } else {
                        deInit(1);
                    }
                    break;
            }
        }
    };


    @Override
    protected void onResume() {
        super.onResume();
        if (mSensorManager != null)
            mSensorManager.registerListener(mLightListener, mSensorLight, 2);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) mSensorManager.unregisterListener(mLightListener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
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

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }
}
