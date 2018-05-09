package com.meigsmart.meigrs32.config;

import com.meigsmart.meigrs32.activity.AudioActivity;
import com.meigsmart.meigrs32.activity.BatteryActivity;
import com.meigsmart.meigrs32.activity.BatteryChargeActivity;
import com.meigsmart.meigrs32.activity.CpuActivity;
import com.meigsmart.meigrs32.activity.EComPassActivity;
import com.meigsmart.meigrs32.activity.EarPhoneActivity;
import com.meigsmart.meigrs32.activity.GSensorActivity;
import com.meigsmart.meigrs32.activity.GpsActivity;
import com.meigsmart.meigrs32.activity.GyroMeterActivity;
import com.meigsmart.meigrs32.activity.LCDBrightnessActivity;
import com.meigsmart.meigrs32.activity.LCDRGBActivity;
import com.meigsmart.meigrs32.activity.LEDActivity;
import com.meigsmart.meigrs32.activity.LSensorActivity;
import com.meigsmart.meigrs32.activity.MemoryActivity;
import com.meigsmart.meigrs32.activity.NFCActivity;
import com.meigsmart.meigrs32.activity.PCBAActivity;
import com.meigsmart.meigrs32.activity.PowerConsumptionActivity;
import com.meigsmart.meigrs32.activity.RearCameraAutoActivity;
import com.meigsmart.meigrs32.activity.ReceiverOrMicActivity;
import com.meigsmart.meigrs32.activity.RecordActivity;
import com.meigsmart.meigrs32.activity.RunInActivity;
import com.meigsmart.meigrs32.activity.SpeakerActivity;
import com.meigsmart.meigrs32.activity.VibratorActivity;

/**
 * Created by chenMeng on 2018/4/24.
 */
public class Const {
    public static boolean isCanBackKey = true;
    public static final String RESULT_SUCCESS = "Success";
    public static final String RESULT_FAILURE = "Failure";
    public static final String RESULT_NOTEST = "NOTEST";
    public static final String RESULT_UNKNOWN = "unknown";

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
            BatteryChargeActivity.class,
            EarPhoneActivity.class,
            SpeakerActivity.class,
            ReceiverOrMicActivity.class,
            RecordActivity.class,
            Class.class,
            GpsActivity.class,
            VibratorActivity.class,
            PowerConsumptionActivity.class
    };

    public static Class[] runInList = {
            CpuActivity.class,
            MemoryActivity.class,
            BatteryActivity.class,
            LCDRGBActivity.class,
            LCDBrightnessActivity.class,
            AudioActivity.class,
            LEDActivity.class,
            NFCActivity.class,
            RearCameraAutoActivity.class,
            GSensorActivity.class,
            EComPassActivity.class,
            LSensorActivity.class,
            GyroMeterActivity.class,
            VibratorActivity.class
    };
}
