package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;

import java.util.ArrayList;
import java.util.List;

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
    private int currPosition = 0;

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

        super.mName = getIntent().getStringExtra("name");

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
        }

        mAdapter.setData(getData(mRunInList,mRunInListConfig, Const.runInList,super.mList));
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
    }

    @Override
    public void onItemClick(int position) {
        currPosition = position;
        startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();
            }
        }
    }
}
