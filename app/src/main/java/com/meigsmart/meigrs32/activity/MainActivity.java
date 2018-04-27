package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.FunctionListAdapter;
import com.meigsmart.meigrs32.config.Const;

import butterknife.BindArray;
import butterknife.BindView;

public class MainActivity extends BaseActivity implements FunctionListAdapter.OnFunctionItemClick{
    private MainActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.recycleView)
    public RecyclerView mRecyclerView;
    @BindArray(R.array.function_list)
    public String[] mFunctionList;
    @BindArray(R.array.function_list_config)
    public int[] mFunctionListConfig;
    private FunctionListAdapter mAdapter;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = false;
        mTitle.setText(R.string.main_title);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new FunctionListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);
        mAdapter.setData(getData(mFunctionList,mFunctionListConfig,Const.functionList,null));
    }

    @Override
    public void onClickItem(int position) {
        startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent();
        intent.setAction(BaseActivity.TAG_ESC_ACTIVITY);
        sendBroadcast(intent);
        System.exit(0);
        finish();
        super.onDestroy();
    }
}
