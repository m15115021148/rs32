package com.meigsmart.meigrs32.application;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * Created by chenMeng on 2018/4/23.
 */
public class MyApplication extends Application {
    private static MyApplication instance;// application对象

    @Override
    public void onCreate() {
        super.onCreate();

    }

    public static MyApplication getInstance(){
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        instance = this;
    }

    /**
     * 获取版本号
     *
     * @return 当前应用的版本号
     */
    public String getVersionName() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            String version = info.versionName;
            return version;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * get App versionCode
     * @return
     */
    public String getVersionCode(){
        PackageManager packageManager=getApplicationContext().getPackageManager();
        PackageInfo packageInfo;
        String versionCode="";
        try {
            packageInfo=packageManager.getPackageInfo(getApplicationContext().getPackageName(),0);
            versionCode=packageInfo.versionCode+"";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return versionCode;
    }

}
