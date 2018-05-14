package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class SimCallActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private SimCallActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.simState)
    public TextView mSimState;
    @BindView(R.id.timer)
    public TextView mTimer;
    @BindView(R.id.simNumber)
    public TextView mPhoneNumber;

    private int mConfigResult;
    private int mConfigTime;
    private boolean isConfigCustom;
    private int mConfigCustomNumber;
    private Runnable mRun;

    private int mCountdown = 5;

    private TelephonyManager telMgr;
    private SimStateReceive mReceive;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static int SIM_VALID = 0;
    private final static int SIM_INVALID = 1;
    private CustomPhoneStateListener mPhoneListener;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sim_call;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_sim_call);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mConfigTime = mConfigTime * 60;
        isConfigCustom = getResources().getBoolean(R.bool.sim_call_default_config_is_use_custom_phone_number);
        mConfigCustomNumber = getResources().getInteger(R.integer.sim_call_default_config_custom_phone_number);
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mHandler.sendEmptyMessageDelayed(1001,2000);
        mReceive = new SimStateReceive();
        mPhoneListener = new CustomPhoneStateListener();

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
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;

                    telMgr = TelephonyManager.from(mContext);

                    if (!isConfigCustom){
                        mConfigCustomNumber = getOperators();
                    }

                    telMgr.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_SIM_STATE_CHANGED);
                    filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
                    registerReceiver(mReceive, filter);

                    break;
                case 1002:
                    int state = (int) msg.obj;
                    if (state == SIM_VALID){
                        mTimer.setVisibility(View.VISIBLE);
                        mSimState.setText(R.string.sim_call_timer_call);
                        mPhoneNumber.setText(
                                Html.fromHtml(getResources().getString(R.string.sim_call_telphone)
                                        +"&nbsp;"+"<font color='#FF0000'>"+mConfigCustomNumber+"</font>")
                        );
                        timer.start();
                    }else if (state == SIM_INVALID){
                        mSimState.setText(R.string.sim_insert_sim);
                    }else {
                        mSimState.setText(R.string.sim_state_unknown);
                    }
                    break;
                case 1003:
                    deInit(SUCCESS);
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
        unregisterReceiver(mReceive);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
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

    private CountDownTimer timer = new CountDownTimer(1000*mCountdown, 1000) {
        @Override
        public void onTick(long l) {
            mTimer.setText(Html.fromHtml(getResources().getString(R.string.sim_call_tag)+"&nbsp;"+"<font color='#FF0000'>"+l/1000+"</font>"));
        }

        @Override
        public void onFinish() {
            mTimer.setVisibility(View.GONE);
            timer.cancel();
            LogUtil.d("onFinish... ");

            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"+mConfigCustomNumber));
            startActivity(intent);
        }
    };

    public int getOperators() {
        int number = 10086;
        String IMSI = telMgr.getSubscriberId();
        if (IMSI == null || IMSI.equals("")) {
            return number;
        }
        if (IMSI.startsWith("46000") || IMSI.startsWith("46002")) {
            number = 10086;
        } else if (IMSI.startsWith("46001")) {
            number = 10010;
        } else if (IMSI.startsWith("46003")) {
            number = 10000;
        }
        return number;
    }

    public class SimStateReceive extends BroadcastReceiver {
        private int simState = SIM_INVALID;

        public int getSimState() {
            return simState;
        }

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ACTION_SIM_STATE_CHANGED)) {
                int state = telMgr.getSimState();
                switch (state) {
                    case TelephonyManager.SIM_STATE_READY :
                        simState = SIM_VALID;
                        break;
                    case TelephonyManager.SIM_STATE_UNKNOWN :
                    case TelephonyManager.SIM_STATE_ABSENT :
                    case TelephonyManager.SIM_STATE_PIN_REQUIRED :
                    case TelephonyManager.SIM_STATE_PUK_REQUIRED :
                    case TelephonyManager.SIM_STATE_NETWORK_LOCKED :
                    default:
                        simState = SIM_INVALID;
                        break;
                }

                Message msg = mHandler.obtainMessage();
                msg.what = 1002;
                msg.obj = simState;
                mHandler.sendMessage(msg);
            } else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)){

            }
        }
    }

    private class CustomPhoneStateListener extends PhoneStateListener {

        @Override
        public void onServiceStateChanged(ServiceState serviceState) {
            super.onServiceStateChanged(serviceState);
            LogUtil.d("CustomPhoneStateListener onServiceStateChanged: " + serviceState);
        }

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            LogUtil.d("CustomPhoneStateListener state: "
                    + state + " incomingNumber: " + incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    LogUtil.d("CustomPhoneStateListener onCallStateChanged endCall");
                    TelephonyManager.getDefault().endCall();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    TelephonyManager.getDefault().endCall();
                    mHandler.sendEmptyMessageDelayed(1003,5000);
                    break;
                default:
                    sendErrorMsgDelayed(mHandler,"unknown");
                    break;
            }
        }
    }

}
