package com.meigsmart.meigrs32.activity;

import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;

import butterknife.BindArray;
import butterknife.BindView;

public class RunInActivity extends BaseActivity implements View.OnClickListener ,CheckListAdapter.OnCallBackCheckFunction {
    private RunInActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindView(R.id.back)
    public LinearLayout mBack;
    @BindArray(R.array.run_in_list)
    public String[] mRunInList;
    @BindArray(R.array.run_in_list_config)
    public int[] mRunInListConfig;
    private CheckListAdapter mAdapter;

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

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setData(getData(mRunInList,mRunInListConfig, Const.runInList));
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
    }

    @Override
    public void onItemClick(int position) {
        startActivity(mAdapter.getData().get(position));
    }
}
