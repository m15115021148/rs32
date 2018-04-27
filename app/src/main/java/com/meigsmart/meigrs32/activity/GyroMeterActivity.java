package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class GyroMeterActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private GyroMeterActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";

    @Override
    protected int getLayoutId() {
        return R.layout.activity_gyro_meter;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = true;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_gyro_meter);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void deInit(int results){
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        updateData(mFatherName,super.mName,result);
        deInit(result);
    }
}
