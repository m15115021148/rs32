package com.meigsmart.meigrs32.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.model.TypeModel;
import com.meigsmart.meigrs32.util.SystemManagerUtil;
import com.meigsmart.meigrs32.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.Unbinder;

public abstract class BaseActivity extends AppCompatActivity {
    protected static final String TAG_ESC_ACTIVITY = "com.broader.esc";
    private MyBroaderEsc receiver;//广播
    private Unbinder butterKnife;//取消绑定
    protected boolean startBlockKeys = true;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(getLayoutId());
        SystemManagerUtil.setSystemManager(this,getResources().getColor(R.color.black));
        if (getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {//设置为竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        // 注册广播
        receiver = new MyBroaderEsc();
        registerReceiver(receiver, new IntentFilter(TAG_ESC_ACTIVITY));
        // 反射注解机制初始化
        butterKnife = ButterKnife.bind(this);
        initData();
    }

    protected abstract int getLayoutId();//获取布局layout

    protected abstract void initData();//初始化数据

    class MyBroaderEsc extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                butterKnife.unbind();
                finish();
            }
        }
    }

    protected List<TypeModel> getData(String[] array, int[] ids, Class[] cls){
        List<TypeModel> list = new ArrayList<>();
        for (int i=0;i<array.length;i++){
            if (ids[i] == 1){
                TypeModel model = new TypeModel();
                model.setId(i);
                model.setName(array[i]);
                model.setCls(cls[i]);
                model.setType(0);
                list.add(model);
            }
        }
        return list;
    }

    protected void startActivity(TypeModel model){
        if (model.getCls().equals(Class.class)){
            ToastUtil.showBottomShort(getResources().getString(R.string.to_be_developed));
            return;
        }
        if (model.getCls() != null){
            Intent intent = new Intent(this,model.getCls());
            startActivity(intent);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_BACK:
                if (startBlockKeys) return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
