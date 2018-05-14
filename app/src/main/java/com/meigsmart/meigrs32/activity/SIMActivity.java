package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class SIMActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private SIMActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.sim1)
    public TextView mSim1;
    @BindView(R.id.sim2)
    public TextView mSim2;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    private int phoneCount = TelephonyManager.getDefault().getSimCount();
    private TelephonyManager telMgr;
    private SimStateReceive mReceive;
    private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
    private final static int SIM_VALID = 0;
    private final static int SIM_INVALID = 1;

    private boolean isInsert = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_sim;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_sim);

        mConfigResult = getResources().getInteger(R.integer.sim_card_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mReceive = new SimStateReceive();

        mHandler.sendEmptyMessageDelayed(1001, 2000);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;

                if (isStartTest)mHandler.sendEmptyMessage(1003);
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
                    isStartTest = true;
                    mLayout.setVisibility(View.VISIBLE);
                    telMgr = TelephonyManager.from(mContext);

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_SIM_STATE_CHANGED);
                    registerReceiver(mReceive, filter);
                    break;
                case 1002:
                    int state = (int) msg.obj;
                    if (state == SIM_VALID){
                        showDevice();
                    }else if (state == SIM_INVALID){
                        mSim1.setText(R.string.sim_insert_sim);
                    }else {
                        mSim1.setText(R.string.sim_state_unknown);
                    }
                    break;
                case 1003:
                    if (isInsert)deInit(SUCCESS);
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
        unregisterReceiver(mReceive);
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(9999);
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

    private void showDevice() {
        LogUtil.d("sim card number :"+phoneCount);
        mSim1.setText("");
        mSim2.setText("");
        int i;

        List<String> keyList = getKeyList();
        List<String> resultList0 = getResultList(0);
        List<String> resultList1 = getResultList(1);
        LogUtil.d("list 0 size:"+resultList0.size());
        LogUtil.d("list 1 size:"+resultList1.size());

        for (i = 0; i < keyList.size(); i++) {
            mSim1.append(keyList.get(i) + resultList0.get(i) + "\n");
        }

        switch (phoneCount) {
            case 2:
                for (i = 0; i < keyList.size(); i++) {
                    //mSim2.append(keyList.get(i) + resultList1.get(i) + "\n");
                }
                break;
            case 1:
                break;
        }
    }

    @SuppressLint("NewApi")
    private List<String> getResultList(int simId) {
        List<String> resultList = new ArrayList<String>();

        if (telMgr == null) {
            sendErrorMsgDelayed(mHandler, "TelephonyManager is null");
            return null;
        }

        if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_READY) {
            resultList.add("sim state fine");
            isInsert = true;
        } else if (telMgr.getSimState(simId) == TelephonyManager.SIM_STATE_ABSENT) {
            resultList.add("sim state no sim");
        } else {
            resultList.add("sim state unknown");
        }

        if (telMgr.getSimCountryIsoForPhone(simId).equals("")) {
            resultList.add("can not get country");
        } else {
            resultList.add(telMgr.getSimCountryIsoForPhone(simId));
        }

        if (telMgr.getSimOperatorNumericForPhone(simId).equals("")) {
            resultList.add("can not get operator");
        } else {
            resultList.add(telMgr.getSimOperatorNumericForPhone(simId));
        }
        if (telMgr.getSimOperatorNameForPhone(simId).equals("")) {
            resultList.add("can not get operator name");
        } else {
            resultList.add(telMgr.getSimOperatorNameForPhone(simId));
        }

        if (!TextUtils.isEmpty(telMgr.getSimSerialNumber(simId))) {
            resultList.add(telMgr.getSimSerialNumber(simId));
        } else {
            resultList.add("can not get serial number");
        }

        if (telMgr.getSubscriberId(simId) != null) {
            resultList.add(telMgr.getSubscriberId(simId));
        } else {
            resultList.add("can not get subscriber id");
        }

        if (telMgr.getDeviceId(simId) != null) {
            resultList.add(telMgr.getDeviceId(simId));
        } else {
            resultList.add("can not get device id");
        }

        if (telMgr.getLine1Number(simId) != null) {
            resultList.add(telMgr.getLine1Number(simId));
        } else {
            resultList.add("can not get phone number");
        }

        if (telMgr.getPhoneType(simId) == 0) {
            resultList.add("NONE");
        } else if (telMgr.getPhoneType(simId) == 1) {
            resultList.add("GSM");
        } else if (telMgr.getPhoneType(simId) == 2) {
            resultList.add("CDMA");
        } else if (telMgr.getPhoneType(simId) == 3) {
            resultList.add("SIP");
        }

        if (telMgr.getDataState() == 0) {
            resultList.add("disconnected");
        } else if (telMgr.getDataState() == 1) {
            resultList.add("connecting");
        } else if (telMgr.getDataState() == 2) {
            resultList.add("connected");
        } else if (telMgr.getDataState() == 3) {
            resultList.add("suspended");
        }

        if (telMgr.getDataActivity() == 0) {
            resultList.add("none");
        } else if (telMgr.getDataActivity() == 1) {
            resultList.add("in");
        } else if (telMgr.getDataActivity() == 2) {
            resultList.add("out");
        } else if (telMgr.getDataActivity() == 3) {
            resultList.add("in/out");
        } else if (telMgr.getDataActivity() == 4) {
            resultList.add("dormant");
        }

        if (!telMgr.getNetworkCountryIsoForPhone(simId).equals("")) {
            resultList.add(telMgr.getNetworkCountryIsoForPhone(simId));
        } else {
            resultList.add("can not get network country");
        }

        if (telMgr.getNetworkType(simId) == 0) {
            resultList.add("unknown");
        } else if (telMgr.getNetworkType(simId) == 1) {
            resultList.add("gprs");
        } else if (telMgr.getNetworkType(simId) == 2) {
            resultList.add("edge");
        } else if (telMgr.getNetworkType(simId) == 3) {
            resultList.add("umts");
        } else if (telMgr.getNetworkType(simId) == 4) {
            resultList.add("hsdpa");
        } else if (telMgr.getNetworkType(simId) == 5) {
            resultList.add("hsupa");
        } else if (telMgr.getNetworkType(simId) == 6) {
            resultList.add("hspa");
        } else if (telMgr.getNetworkType(simId) == 7) {
            resultList.add("cdma");
        } else if (telMgr.getNetworkType(simId) == 8) {
            resultList.add("evdo 0");
        } else if (telMgr.getNetworkType(simId) == 9) {
            resultList.add("evdo a");
        } else if (telMgr.getNetworkType(simId) == 10) {
            resultList.add("evdo b");
        } else if (telMgr.getNetworkType(simId) == 11) {
            resultList.add("1xrtt");
        } else if (telMgr.getNetworkType(simId) == 12) {
            resultList.add("iden");
        } else if (telMgr.getNetworkType(simId) == 13) {
            resultList.add("lte");
        } else if (telMgr.getNetworkType(simId) == 14) {
            resultList.add("ehrpd");
        } else if (telMgr.getNetworkType(simId) == 15) {
            resultList.add("hspap");
        }

        return resultList;
    }

    private List<String> getKeyList() {
        List<String> keyList = new ArrayList<String>();
        keyList.add("Sim Status:  ");

        keyList.add("Sim Country:  ");
        keyList.add("Sim Operator:  ");
        keyList.add("Sim Operator Name:  ");
        keyList.add("Sim Serial Number:  ");
        keyList.add("Subscriber Id:  ");
        keyList.add("Device Id:  ");
        //keyList.add("Line 1 Number:  ");
        keyList.add("Phone Type:  ");
        keyList.add("Data State:  ");
        keyList.add("Data Activity:  ");
        keyList.add("Network Country:  ");
        //keyList.add("Network Operator:  ");
        keyList.add("Network Type:  ");

        return keyList;
    }

    public class SimStateReceive extends BroadcastReceiver{

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
            }

        }
    }

}
