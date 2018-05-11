package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.WifiListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class WifiActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack   {
    private WifiActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.wifiMac)
    public TextView mWifiMac;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.scan)
    public TextView mScan;
    private WifiListAdapter mAdapter;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    private WifiManager wifimanager = null;
    private static final int DELAY_TIME = 15000;
    private StartScanThread mStartScanThread = null;
    private WifiEnableReceiver wifiEnableReceiver = null;
    private WifiScanReceiver wifiScanReceiver = null;

    private List<ScanResult> mList = new ArrayList<>();


    @Override
    protected int getLayoutId() {
        return R.layout.activity_wifi;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_wifi);

        mConfigResult = getResources().getInteger(R.integer.wifi_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mAdapter = new WifiListAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

        mHandler.sendEmptyMessageDelayed(1001,2000);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    // mHandler.sendEmptyMessage(1001);
                }
                if (isStartTest)mHandler.sendEmptyMessageDelayed(1002,3000);
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
                    mLayout.setVisibility(View.VISIBLE);

                    wifimanager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifInfo = wifimanager.getConnectionInfo();
                    mWifiMac.setText(Html.fromHtml(getResources().getString(R.string.wifi_mac_address)+"&nbsp;"+wifInfo.getMacAddress()));
                    startWifi();
                    break;
                case 1002:
                    mAdapter.setData(mList);
                    if (!TextUtils.isEmpty(getMac()) && "02:00:00:00:00:00".equals(getMac())){
                        if (mList.size()>0){
                            deInit(SUCCESS);
                        }
                    }else {
                        sendErrorMsgDelayed(mHandler,"wifi mac address is null");
                    }
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
        stopWifi();
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

    /**
     * 获取mac地址 wifi
     *
     * @return
     */
    public String getMac() {
        String macSerial = "";
        String str = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address ");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            for (; null != str;) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();// 去空格
                    break;
                }
            }
        } catch (IOException ex) {
            sendErrorMsgDelayed(mHandler,ex.getMessage());
            ex.printStackTrace();
        }
        return macSerial;
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

    private void startWifi(){
        int wifiDefaultState = wifimanager.getWifiState();
        LogUtil.d("wifiDefaultState:"+wifiDefaultState);
        switch(wifiDefaultState){
            case WifiManager.WIFI_STATE_ENABLED:
                scanDevices();
                mStartScanThread = new StartScanThread();
                mStartScanThread.start();
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                if(!enableWifi())
                    LogUtil.e("wifi open fail");
                break;
        }

    }

    /**
     * start scan thread
     */
    class StartScanThread extends Thread {
        @Override
        public void run() {
            try {
                LogUtil.d("RUN ......");
                // wait until other actions finish.
                wifimanager.startScan();
                SystemClock.sleep(DELAY_TIME);
            } catch (Exception e) {
                // do nothing
            }
        }
    }


    /**
     * Wifi scan receiver class
     *
     * @see BroadcastReceiver
     * @author
     */
    private class WifiScanReceiver extends BroadcastReceiver {
        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d("onReceive ......");
            // get scan result lisg
            List<ScanResult> wifiScanResultList = wifimanager.getScanResults();
            // check result
            if ((wifiScanResultList != null) && (wifiScanResultList.size() > 0)) {
                for (ScanResult r : wifiScanResultList){
                    if (!mList.contains(r)){
                        mList.add(r);
                    }
                }
            } else {
                sendErrorMsgDelayed(mHandler,"wifi scan failure");
            }
        }
    }

    /**
     * Wifi enabled receiver class
     *
     * @see BroadcastReceiver
     * @author
     */
    private class WifiEnableReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (wifimanager.getWifiState()) {
                case WifiManager.WIFI_STATE_ENABLED:
                    // enabled wifi ok
                    scanDevices();
                    mStartScanThread = new StartScanThread();
                    mStartScanThread.start();

                    break;
                case WifiManager.WIFI_STATE_DISABLED:
                case WifiManager.WIFI_STATE_DISABLING:
                case WifiManager.WIFI_STATE_UNKNOWN:
                case WifiManager.WIFI_STATE_ENABLING:
                default:
                    // do nothing
            }
        }
    }

    /**
     * Create wifi state change receiver and set wifi eanbled
     *
     * @return result of setWifiEnabled
     */
    private boolean enableWifi() {
        if(wifiEnableReceiver == null){
            wifiEnableReceiver = new WifiEnableReceiver();
            IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wifiEnableReceiver, filter);
        }
        // return wifi enabled result
        return wifimanager.setWifiEnabled(true);

    }

    /**
     * Create wifi scan result receiver and start scan
     */
    private void scanDevices() {
        if(wifiScanReceiver == null){
            wifiScanReceiver = new WifiScanReceiver();
            IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(wifiScanReceiver, filter);
        }
    }

    private void stopWifi() {
        // release wifi enabled receiver
        if (wifiEnableReceiver != null) {
            unregisterReceiver(wifiEnableReceiver);
            wifiEnableReceiver = null;
        }

        // release wifi scan receiver
        if (wifiScanReceiver != null) {
            unregisterReceiver(wifiScanReceiver);
            wifiScanReceiver = null;
        }

    }

}
