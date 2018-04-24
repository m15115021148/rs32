package com.meigsmart.meigrs32.activity;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;

import butterknife.BindView;

public class CpuActivity extends BaseActivity implements View.OnClickListener {
    private CpuActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_cpu;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_cpu);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
    }
}
