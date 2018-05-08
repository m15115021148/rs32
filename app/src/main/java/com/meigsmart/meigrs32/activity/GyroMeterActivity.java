package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.List;

import butterknife.BindView;
import butterknife.BindViews;

public class GyroMeterActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GyroMeterActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindViews({R.id.gyro_x,R.id.gyro_y,R.id.gyro_z})
    public List<TextView> mGyroList;
    @BindView(R.id.flag)
    public TextView mFlag;

    private SensorManager sensorManager;//管理器对象
    private Sensor gyroSensor;//传感器对象

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gyro_meter;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_gyro_meter);

        mConfigResult = getResources().getInteger(R.integer.gyro_meter_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mHandler.sendEmptyMessageDelayed(1001,2000);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    mHandler.sendEmptyMessage(1002);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    /**
     * 对象的初始化
     */
    private void init(){
        sensorManager=(SensorManager) getSystemService(SENSOR_SERVICE);
        gyroSensor=sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if(gyroSensor==null){
            sendErrorMsg(mHandler,"gyro-meter sensor is no supper");
            return;
        }else{
            sensorManager.registerListener(sensoreventlistener, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mFlag.setVisibility(View.GONE);
                    mGyroList.get(0).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(0)));
                    mGyroList.get(1).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(0)));
                    mGyroList.get(2).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(0)));
                    init();
                    break;
                case 1002:
                    deInit(SUCCESS);
                    break;
                case 1003:
                    deInit(FAILURE,Const.RESULT_UNKNOWN);
                    break;
                case 2:
                    float[] f = (float[]) msg.obj;
                    mGyroList.get(0).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(f[0])));
                    mGyroList.get(1).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(f[1])));
                    mGyroList.get(2).setText(Html.fromHtml(getResources().getString(R.string.gyro_x_angle)+"&nbsp;"+Float.toString(f[2])));
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    /**
     * 传感器的监听
     */
    private SensorEventListener sensoreventlistener=new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            float[] value = event.values;
            Message msg = mHandler.obtainMessage();
            msg.what = 2;
            msg.obj = value;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
        if (sensorManager!=null)sensorManager.unregisterListener(sensoreventlistener);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(2);
        mHandler.removeMessages(9999);
    }

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
