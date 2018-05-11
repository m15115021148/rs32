package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.Html;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.service.GpsService;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;

public class GpsActivity extends BaseActivity implements View.OnClickListener,PromptDialog.OnPromptDialogCallBack {
    private GpsActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;
    @BindView(R.id.lng)
    public TextView mLng;
    @BindView(R.id.lat)
    public TextView mLat;
    @BindView(R.id.count)
    public TextView mCount;

    private Intent locationIntent;
    private boolean isBindLocation;
    private Runnable mRun;

    /** location manager object */
    private LocationManager manager = null;
    /** GPS provider name */
    private static final String PROVIDER = LocationManager.GPS_PROVIDER;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gps;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_gps);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Settings.Secure.setLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER, true);

        mHandler.sendEmptyMessageDelayed(1001,2000);
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
                    startService();
                    break;
                case GpsService.GPS_FROM_SERVER:
                    double lat = msg.getData().getDouble("lat");
                    double lng = msg.getData().getDouble("lng");
                    int count = msg.getData().getInt("count");
                    mLat.setText(Html.fromHtml(getResources().getString(R.string.gps_current_lat)+"&nbsp;"+lat));
                    mLng.setText(Html.fromHtml(getResources().getString(R.string.gps_current_lng)+"&nbsp;"+lng));
                    mCount.setText(Html.fromHtml(getResources().getString(R.string.gps_count_num)+"&nbsp;"+count));
                    if (lat!=0 && lng!=0 && count>=4){
                        deInit(SUCCESS);
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
        if (isBindLocation){
            stopService(locationIntent);
            this.unbindService(connLocation);
        }
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(9999);
        Settings.Secure.setLocationProviderEnabled(getContentResolver(),
                LocationManager.GPS_PROVIDER, false);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }


    /**
     * 启动定位服务
     */
    private void startService(){
        // 启动服务
        locationIntent = new Intent(this, GpsService.class);
        isBindLocation = bindService(locationIntent, connLocation, Context.BIND_AUTO_CREATE);
        startService(locationIntent);
    }

    ServiceConnection connLocation = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, final IBinder service) {
            mRun = new Runnable() {
                @Override
                public void run() {
                    try {
                        Messenger messenger = new Messenger(service);
                        Message msg = Message.obtain(null, GpsService.GPS_FROM_CLIENT);
                        msg.replyTo = new Messenger(mHandler);
                        messenger.send(msg);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mHandler.postDelayed(this,1000);
                }
            };
            mRun.run();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    /**
     * Check gps state
     *
     * @return true if enabled
     */
    private boolean isGpsEnabled() {
        if (manager == null) {
            return false;
        }
        return manager.isProviderEnabled(PROVIDER);
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
