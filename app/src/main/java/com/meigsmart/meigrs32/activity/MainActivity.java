package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.FunctionListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.model.ClassModel;
import com.meigsmart.meigrs32.model.TypeModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindArray;
import butterknife.BindInt;
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
    private List<TypeModel> mList;
    private List<ClassModel> mClassList;

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
        mList = new ArrayList<>();
        mClassList = getClassName(mFunctionListConfig, Const.functionList);
        mAdapter.setData(getData(mFunctionList,mFunctionListConfig));
    }

    private List<TypeModel> getData(String[] array, int[] ids){
        List<TypeModel> list = new ArrayList<>();
        for (int i=0;i<array.length;i++){
            if (ids[i] == 1){
                TypeModel model = new TypeModel();
                model.setId(i);
                model.setName(array[i]);
                list.add(model);
            }
        }
        return list;
    }

    private List<ClassModel> getClassName(int[] ids,Class[] cls){
        List<ClassModel> list = new ArrayList<>();
        for (int i=0;i<ids.length;i++){
            if (ids[i] == 1){
                ClassModel model = new ClassModel();
                model.setId(i);
                model.setCls(cls[i]);
                list.add(model);
            }
        }
        return list;
    }

    @Override
    public void onClickItem(int position) {
        Intent intent = new Intent(mContext,mClassList.get(position).getCls());
        startActivity(intent);
    }
}
