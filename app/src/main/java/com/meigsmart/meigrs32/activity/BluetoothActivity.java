package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.BluetoothListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class BluetoothActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack  {
    private BluetoothActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.btSn)
    public TextView mBtSn;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.scan)
    public TextView mScan;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDiscoveryReceiver btDiscoveryReceiver;
    private BlueToothStateReceiver btStateReceiver;
    private List<BluetoothDevice> bluetoothDeviceList = new ArrayList<BluetoothDevice>();;
    private BluetoothListAdapter mAdapter;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_bluetooth;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mScan.setOnClickListener(this);
        mTitle.setText(R.string.pcba_bt);

        mConfigResult = getResources().getInteger(R.integer.bt_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.pcba_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mHandler.sendEmptyMessageDelayed(1001,2000);

        mAdapter = new BluetoothListAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);

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
                    init();
                    registerAllReceiver();
                    btStart();
                    mBtSn.setText(Html.fromHtml(getResources().getString(R.string.bluetooth_address)+"&nbsp;"+getBluetoothAddress()));
                    break;
                case 1002:
                    mScan.setVisibility(View.GONE);
                    mBtSn.setText(Html.fromHtml(getResources().getString(R.string.bluetooth_address)+"&nbsp;"+getBluetoothAddress()));
                    mAdapter.setData(bluetoothDeviceList);
                    if (!TextUtils.isEmpty(getBluetoothAddress())){
                        if (bluetoothDeviceList.size()>0 ){
                            deInit(SUCCESS);
                        }else {
//                            mScan.setVisibility(View.VISIBLE);
//                            ToastUtil.showBottomShort(getResources().getString(R.string.bluetooth_no_device));
                        }
                    }else {
                        deInit(FAILURE,"bluetooth mac address is null");
                    }
                    break;
                case 1003:
                    mScan.setVisibility(View.VISIBLE);
                    ToastUtil.showBottomShort(getResources().getString(R.string.bluetooth_no_device));
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private void init(){
        mBluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            sendErrorMsgDelayed(mHandler,"Unable to initialize BluetoothManager.");
            return ;
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            sendErrorMsgDelayed(mHandler,"Unable to obtain a BluetoothListAdapter.");
            return ;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterAllReceiver();
        btStop();
        mHandler.removeMessages(1001);
        mHandler.removeCallbacks(mRun);
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
        if (v == mScan){
            btStart();
        }
    }

    private void btStart(){
        switch(mBluetoothAdapter.getState()){
            case BluetoothAdapter.STATE_ON:
            case BluetoothAdapter.STATE_TURNING_ON:
                btStartDiscovery();
                break;
            case BluetoothAdapter.STATE_OFF:
            case BluetoothAdapter.STATE_TURNING_OFF:
                btEnable();
                break;
        }
    }

    private void btStop() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
        }
    }

    private void btEnable() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.enable();
        }
    }

    private void btStartDiscovery() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.startDiscovery();
        } else {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mBluetoothAdapter.startDiscovery();
        }
    }

    private void unregisterAllReceiver() {
        if (btDiscoveryReceiver != null) {
            unregisterReceiver(btDiscoveryReceiver);
        }
        if (btStateReceiver != null) {
            unregisterReceiver(btStateReceiver);
        }

    }

    private void registerAllReceiver() {
        // register receiver for bt search
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        btDiscoveryReceiver = new BluetoothDiscoveryReceiver();
        registerReceiver(btDiscoveryReceiver, intent);
        // register reveiver for bt state change
        btStateReceiver = new BlueToothStateReceiver();
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btStateReceiver, filter);
    }

    private class BluetoothDiscoveryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtil.d( "BluetoothDiscoveryReceiver action : " + action);
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                LogUtil.d("Search bluetooth device");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if(bluetoothDeviceList.contains(device))
                    return;
                if (device != null) {
                    bluetoothDeviceList.add(device);
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        StringBuffer deviceInfo = new StringBuffer();
                        deviceInfo.append("name:");
                        deviceInfo.append(device.getName());
                        deviceInfo.append("\naddress: ");
                        deviceInfo.append(device.getAddress());
                        deviceInfo.append("\n");
                        LogUtil.w(deviceInfo.toString());
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                LogUtil.d("Search bluetooth finished !");
                btStop();
                mHandler.sendEmptyMessage(1003);
            }

        }

    }

    private class BlueToothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtil.d("BlueToothStateReceiver:"+mBluetoothAdapter.getState());
            switch (mBluetoothAdapter.getState()) {
                case BluetoothAdapter.STATE_ON:
                    btStartDiscovery();
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_OFF:
                    btEnable();
                    break;
                case BluetoothAdapter.STATE_TURNING_ON:
                    break;
                default:
                    // do nothing
            }
        }
    }

    /**
     * 获取蓝牙地址
     *
     * @return
     */
    private String getBluetoothAddress() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Field field = bluetoothAdapter.getClass().getDeclaredField("mService");
            // 参数值为true，禁用访问控制检查
            field.setAccessible(true);
            Object bluetoothManagerService = field.get(bluetoothAdapter);
            if (bluetoothManagerService == null) {
                return null;
            }
            Method method = bluetoothManagerService.getClass().getMethod("getAddress");
            Object address = method.invoke(bluetoothManagerService);
            if (address != null && address instanceof String) {
                return (String) address;
            } else {
                return null;
            }

        } catch (IllegalArgumentException | NoSuchFieldException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
        return null;
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
