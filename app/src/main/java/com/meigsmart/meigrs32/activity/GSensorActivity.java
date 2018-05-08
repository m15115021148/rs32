package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Context;
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
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.BindViews;

public class GSensorActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GSensorActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindViews({R.id.up,R.id.down,R.id.left,R.id.right})
    public List<TextView> mTextViewList;
    @BindView(R.id.flag)
    public TextView mFlag;

    private Sensor sensor = null;
    private SensorEventListener listener = null;
    private float[] mValues;
    private boolean mDxOk = false;
    private boolean mDyOk = false;
    private boolean mDzOk = false;

    private boolean upok = false;
    private boolean downok = false;
    private boolean leftok = false;
    private boolean rightok = false;

    private SensorManager manager = null;
    private static final int DATA_X = 0;
    private Timer mTimer;
    private static final int DATA_Y = 1;
    private static final int DATA_Z = 2;
    private static final int DELAY_TIME = 300;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_g_sensor;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_g_sensor);

        mConfigResult = getResources().getInteger(R.integer.g_sensor_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mFlag.setText(R.string.start_tag);
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

    @SuppressLint("HandlerLeak")
    public Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mFlag.setText(R.string.g_sensor_layout_tag);
                    mTextViewList.get(0).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_up)));
                    mTextViewList.get(1).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_down)));
                    mTextViewList.get(2).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_left)));
                    mTextViewList.get(3).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_right)));

                    initSensor();
                    break;
                case 1002:
                    if (upok && downok && leftok && rightok){
                        deInit(SUCCESS);
                    }else {
                        deInit(FAILURE,"no pass");
                    }
                    break;
                case 1003:
                    if (upok)mTextViewList.get(0).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_up)+"&nbsp;"+"<font color='#00FF00'>"+"Pass"+"</font>"));
                    if (downok)mTextViewList.get(1).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_down)+"&nbsp;"+"<font color='#00FF00'>"+"Pass"+"</font>"));
                    if (leftok)mTextViewList.get(2).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_left)+"&nbsp;"+"<font color='#00FF00'>"+"Pass"+"</font>"));
                    if (rightok)mTextViewList.get(3).setText(Html.fromHtml(getResources().getString(R.string.g_sensor_right)+"&nbsp;"+"<font color='#00FF00'>"+"Pass"+"</font>"));
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
        manager.unregisterListener(listener);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void showArrow(float x, float y, float z) {

        if (Math.abs(x) <= Math.abs(y)) {
            if (y < 0) {
                // up is low
                upok = true;
            } else if (y > 0) {
                // down is low
                downok = true;
            } else if (y == 0) {
                // do nothing
            }
        } else {
            if (x < 0 && z < 11) {
                // right is low
                rightok = true;
            } else {
                // left is low
                leftok = true;
            }
        }
        mHandler.sendEmptyMessage(1003);
    }

    @Override
    protected void onPause() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        manager.unregisterListener(listener);
        super.onPause();
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

    private void initSensor() {
        listener = new SensorEventListener() {
            public void onAccuracyChanged(Sensor s, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                mValues = event.values;
                float x = event.values[SensorManager.DATA_X];
                float y = event.values[SensorManager.DATA_Y];
                float z = event.values[SensorManager.DATA_Z];

                double dx = Math.abs(9.8 - Math.abs(x));
                double dy = Math.abs(9.8 - Math.abs(y));
                double dz = Math.abs(9.8 - Math.abs(z));
                double ref = 9.8 * 0.08;
                if (!mDxOk)
                    mDxOk = dx < ref;
                if (!mDyOk)
                    mDyOk = dy < ref;
                if (!mDzOk)
                    mDzOk = dz < ref;
                if (mDxOk && mDyOk && mDzOk) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            int status = 0;
                            try {
                                Process p = Runtime.getRuntime().exec("chmod 660 /dev/mc3xxxmd");
                                status = p.waitFor();
                            } catch (InterruptedException i) {
                                i.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (0 == status) {
                                LogUtil.w("onclick successful");
                            } else {
                                LogUtil.w("onclick failed");
                            }
                        }
                    }, 10);
                }
            }
        };

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert manager != null;
        sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME);

        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }

        mTimer = new Timer();
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mValues != null) {
                            float x = mValues[DATA_X];
                            float y = mValues[DATA_Y];
                            float z = mValues[DATA_Z];
                            if (Math.abs(x) < 1) {
                                x = 0;
                            }
                            if (Math.abs(y) < 1) {
                                y = 0;
                            }
                            if (Math.abs(z) < 1) {
                                z = 0;
                            }
                            showArrow(x, y, z);
                        }
                    }
                });
            }
        }, 0, DELAY_TIME);

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
