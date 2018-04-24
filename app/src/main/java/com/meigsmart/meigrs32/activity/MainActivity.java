package com.meigsmart.meigrs32.activity;

import android.widget.TextView;

import com.meigsmart.meigrs32.R;

import butterknife.BindView;

public class MainActivity extends BaseActivity {
    private MainActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;


    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initData() {
        mContext = this;
        mTitle.setText(R.string.main_title);

    }

}
