package com.meigsmart.meigrs32.activity;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

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

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        NfcManager manager = (NfcManager) getSystemService(Context.NFC_SERVICE);
        mDefaultAdapter = manager.getDefaultAdapter();
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mDefaultAdapter!=null){
            mDefaultAdapter.enableForegroundDispatch(this,pendingIntent,null,null);//打开前台发布系统，使页面优于其它nfc处理
        }else{
            LogUtil.e("mDefaultAdapter is null");
        }
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

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }
}
