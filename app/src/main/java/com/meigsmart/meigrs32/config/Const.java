package com.meigsmart.meigrs32.config;

import com.meigsmart.meigrs32.activity.CpuActivity;
import com.meigsmart.meigrs32.activity.MemoryActivity;
import com.meigsmart.meigrs32.activity.PCBAActivity;
import com.meigsmart.meigrs32.activity.RunInActivity;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class Const {
    public static Class[] functionList = {
            PCBAActivity.class,
            RunInActivity.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class
    };

    public static Class[] pcbaList = {
            MemoryActivity.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class
    };

    public static Class[] runInList = {
            CpuActivity.class,
            MemoryActivity.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class,
            Class.class
    };
}
