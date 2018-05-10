package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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

import org.json.JSONException;

import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class BatteryChargeActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private BatteryChargeActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.battery)
    public TextView mBattery;
    @BindView(R.id.secondBattery)
    public TextView mSecondBattery;

    private static final String BATTERY_ELECTRONIC = "/sys/class/power_supply/bms/current_now";//read
    private static final String BATTERY_V = "/sys/class/power_supply/bms/voltage_ocv";
    private static final String STATUS = "status";
    private static final String LEVEL = "level";
    private static final String PLUGGED = "plugged";
    private static final String VOLTAGE = "voltage";

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_battery_charge;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_battery_charge);

        mConfigResult = getResources().getInteger(R.integer.battery_charge_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
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
                //if (isStartTest)mHandler.sendEmptyMessage(1003);
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
                    isStartTest = true;
                    mFlag.setVisibility(View.GONE);
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_BATTERY_CHANGED);
                    registerReceiver(mBroadcastReceiver, filter);
                    break;
                case 1002:
                    break;
                case 1003:
                    BatteryVolume volume = (BatteryVolume) msg.obj;
                    mBattery.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_charge_is)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getLevel()+"%"+"</font>"
                            ));

                    mSecondBattery.setText(
                            Html.fromHtml(
                                    getResources().getString(R.string.battery_second_voltage)+
                                            "&nbsp;"+"<font color='#FF0000'>"+volume.getVoltage()+ " mv"+"</font>"
                            ));
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
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
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

    private float getBatteryElectronic() {
        char[] buffer = new char[1024];

        float batteryElectronic = 0;
        FileReader file = null;
        try {
            file = new FileReader(BATTERY_ELECTRONIC);
            int len = file.read(buffer, 0, 1024);
            batteryElectronic = Float.valueOf((new String(buffer, 0, len)));
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (Exception e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
                LogUtil.e("getBatteryElectronic fail");
            }
        }
        return batteryElectronic;
    }

    private float getBatteryVoltage() {
        char[] buffer = new char[1024];

        float batteryVoltage = 0;
        FileReader file = null;
        try {
            file = new FileReader(BATTERY_V);
            int len = file.read(buffer, 0, 1024);
            batteryVoltage = Float.valueOf((new String(buffer, 0, len)));
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (Exception e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
                LogUtil.e("getBatteryElectronic fail");
            }
        }
        return batteryVoltage;
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        String action;


        @Override
        public void onReceive(Context context, Intent intent) {
            action = intent.getAction();
            BatteryVolume volume = new BatteryVolume();

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra(STATUS, 0);
                int plugged = intent.getIntExtra(PLUGGED, 0);
                volume.setVoltage(intent.getIntExtra(VOLTAGE, 0));
                volume.setLevel(intent.getIntExtra(LEVEL,0));
                String statusString = "";
                String acString = "";
                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        statusString = "BATTERY_STATUS_UNKNOWN";
                        break;

                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = "BATTERY_STATUS_CHARGING";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = "BATTERY_STATUS_DISCHARGING";
                        break;

                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = "BATTERY_STATUS_NOT_CHARGING";
                        break;

                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = "BATTERY_STATUS_FULL";
                        break;
                    default:
                        break;
                }
                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        acString = "BATTERY_PLUGGED_AC";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        acString = "BATTERY_PLUGGED_USB";
                        break;
                    default:
                        acString = "BATTERY_PLUGGED_USB";
                        break;
                }
                volume.setStatus(statusString);
                volume.setPlugged(acString);
                LogUtil.d(volume.toString());
                Message msg = mHandler.obtainMessage();
                msg.what = 1003;
                msg.obj = volume;
                mHandler.sendMessage(msg);
            }
        }
    };

    public class BatteryVolume {
        private String status ;
        private int level;// battery charge is
        private String plugged;//battery content
        private int voltage;// battery voltage

        @Override
        public String toString() {
            return "level:"+level+" voltage:"+voltage;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }

        public String getPlugged() {
            return plugged;
        }

        public void setPlugged(String plugged) {
            this.plugged = plugged;
        }

        public int getVoltage() {
            return voltage;
        }

        public void setVoltage(int voltage) {
            this.voltage = voltage;
        }
    }
}
