package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.adapter.CheckListAdapter;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.db.FunctionBean;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.model.ResultModel;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.io.File;
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

    private int DELAY_TIME = 2000;

    private String mDefaultPath;
    private boolean isCustomPath ;
    private String mCustomPath;
    private String mFileName ;
    private int mRoundsNumber;

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

        mDefaultPath = getResources().getString(R.string.run_in_save_log_default_path);
        mFileName = getResources().getString(R.string.run_in_save_log_file_name);
        mRoundsNumber = getResources().getInteger(R.integer.run_in_test_rounds_number);
        isCustomPath = getResources().getBoolean(R.bool.run_in_save_log_is_user_custom);
        mCustomPath = getResources().getString(R.string.run_in_save_log_custom_path);
        LogUtil.d("mDefaultPath:" + mDefaultPath +
                " mFileName:" + mFileName+
                " mRoundsNumber:" + mRoundsNumber+
                " isCustomPath:"+isCustomPath+
                " mCustomPath:"+mCustomPath);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new CheckListAdapter(this);
        mRecyclerView.setAdapter(mAdapter);

        super.mName = getIntent().getStringExtra("name");

        if (!TextUtils.isEmpty(super.mName)){
            super.mList = getFatherData(super.mName);
        }

        mAdapter.setData(getData(mRunInList,mRunInListConfig, Const.runInList,super.mList));
        ToastUtil.showBottomLong(getResources().getString(R.string.start_tag));
        mHandler.sendEmptyMessageDelayed(1001,DELAY_TIME);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    if (currPosition == 0){
                        mRoundsNumber--;
                    }
                    startActivity(mAdapter.getData().get(currPosition));
                    break;
                case 1002://test finish
                    ToastUtil.showBottomShort(getResources().getString(R.string.run_in_test_finish));
                    initPath(isCustomPath?mCustomPath:mDefaultPath,mFileName,createJsonResult());
                    mContext.finish();
                    break;
            }
        }
    };

    private String initPath(String path, String fileName, String result){
        if (!TextUtils.isEmpty(path) && !TextUtils.isEmpty(fileName)){
            File file = FileUtil.createRootDirectory(path);
            return FileUtil.writeFile(file,fileName,result);
        }
        return "";
    }

    private String createJsonResult(){
        List<FunctionBean> list = getFatherData(super.mName);
        List<ResultModel> resultList = new ArrayList<>();
        for (FunctionBean bean:list){
            ResultModel model = new ResultModel();
            if (bean.getResults() == 0){
                model.setResult(Const.RESULT_NOTEST);
            } else if (bean.getResults() == 1){
                model.setResult(Const.RESULT_FAILURE);
            } else if (bean.getResults() == 2){
                model.setResult(Const.RESULT_SUCCESS);
            }
            model.setFatherName(bean.getFatherName());
            model.setSubName(bean.getSubclassName());
            model.setReason(bean.getReason());
            resultList.add(model);
        }
        return JSON.toJSONString(resultList);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack)mContext.finish();
    }

    @Override
    public void onItemClick(int position) {
//        currPosition = position;
//        startActivity(mAdapter.getData().get(position));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 1111){
            if (data!=null){
                int results = data.getIntExtra("results",0);
                mAdapter.getData().get(currPosition).setType(results);
                mAdapter.notifyDataSetChanged();
                currPosition++;
                if ( currPosition == Const.runInList.length ){
                    currPosition = 0;
                    if (mRoundsNumber == 0){
                        mHandler.sendEmptyMessageDelayed(1002,DELAY_TIME);
                        return;
                    }
                }
                mHandler.sendEmptyMessageDelayed(1001,DELAY_TIME);
            }
        }
    }
}
