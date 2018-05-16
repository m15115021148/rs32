package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.lang.reflect.Method;

import butterknife.BindView;

public class NFCActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private NFCActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    private NfcAdapter mDefaultAdapter;
    private PendingIntent pendingIntent;
    @BindView(R.id.flag)
    public TextView mFlag;
    @BindView(R.id.layout)
    public LinearLayout mLayout;

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    private static IntentFilter[] NFC_FILTERS;
    private static String[][] NFC_TECHLISTS;

    private boolean isPass;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nfc;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_nfc);

        mConfigResult = getResources().getInteger(R.integer.nfc_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        mHandler.sendEmptyMessageDelayed(1000,2000);



        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
//                    mHandler.sendEmptyMessage(1001);
                }
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
                case 1000:
                    mFlag.setVisibility(View.GONE);
                    mLayout.setVisibility(View.VISIBLE);
                    isStartTest = true;

                    NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
                    mDefaultAdapter = manager.getDefaultAdapter();
                    String[][] strings = new String[1][];
                    //最常见的卡片类型就是IsoDep
                    strings[0] = new String[] { IsoDep.class.getName() };
                    NFC_TECHLISTS = strings;
                    try {
                        NFC_FILTERS =
                                new IntentFilter[] { new IntentFilter("android.nfc.action.TECH_DISCOVERED", "*/*") };
                    } catch (IntentFilter.MalformedMimeTypeException e) {
                        e.printStackTrace();
                    }

                    pendingIntent = PendingIntent.getActivity(
                            mContext,
                            0,
                            new Intent(mContext, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            0);

                    if (mDefaultAdapter!=null){
                        mDefaultAdapter.enableForegroundDispatch(mContext,pendingIntent, null, null);
                    }else{
                        sendErrorMsgDelayed(mHandler,"mDefaultAdapter is null");
                    }


//                    mDefaultAdapter.getNfcFCardEmulationService()

                    break;
                case 1001:
                    if (isPass){
                        deInit(SUCCESS);
                    }else {
                        deInit(FAILURE,Const.RESULT_UNKNOWN);
                    }
                    break;
                case 1002:
                    deInit(FAILURE);
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    private void intSE(){
        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(mRun);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mDefaultAdapter!=null){
            mDefaultAdapter.disableForegroundDispatch(this);//关闭前台发布系统
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag mTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        String[] techList=mTag.getTechList();

        for (String tech:techList){
            isPass = true;
            LogUtil.w("tech:"+tech);
        }
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
