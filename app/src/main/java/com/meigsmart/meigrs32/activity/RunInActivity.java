package com.meigsmart.meigrs32.activity;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;

import butterknife.BindView;

public class RunInActivity extends BaseActivity implements View.OnClickListener {
    private RunInActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.back)
    public LinearLayout mBack;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_run_in;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.function_run_in);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
    }
}
