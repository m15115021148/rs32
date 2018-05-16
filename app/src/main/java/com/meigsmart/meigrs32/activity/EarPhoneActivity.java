package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.service.AudioLoopbackService;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.util.ToastUtil;
import com.meigsmart.meigrs32.view.PromptDialog;
import com.meigsmart.meigrs32.view.VolumeView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import butterknife.BindView;

public class EarPhoneActivity extends BaseActivity implements OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private EarPhoneActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.play)
    public Button mPlay;

    public byte mPLBTestFlag[] = new byte[1];
    private static final String HEADSET_UEVENT_MATCH = "DEVPATH=/devices/virtual/switch/h2w";
    private static final String HEADSET_STATE_PATH = "/sys/class/switch/h2w/state";
    private static final String HEADSET_NAME_PATH = "/sys/class/switch/h2w/name";
    private AudioManager mAudioManager;
    private final int STREAM_TYPE = AudioManager.STREAM_MUSIC;
    @BindView(R.id.volumeView)
    public VolumeView mVolumeView;
    private MediaSession mSession;

    private final static String HEADSET_TYPE_PATH = "/sys/devices/soc.0/sound.70/hstype";
    private final static int HEADSET_PENDING = 2;
    private final static int HEADSET_CN = 1;
    private final static int HEADSET_US = 0;
    private final static int HEADSET_UNKNOW = -1;

    public boolean hasPressButton = false;
    private boolean mIsHeadsetCnPass = false;
    private boolean mIsHeadsetUsPass = false;
    private boolean mIsHeadsetExistCn = false;
    private boolean mIsHeadsetExistUs = false;
    private boolean showmessage = true;
    private boolean mHasVoicePass = false;
    private boolean isHeadsetExist = false;
    private int mRetryCount = 0;

    private boolean isCustomPath ;
    private String mCustomPath;
    private String mCustomFileName ;

    private MediaPlayer mMediaPlayer;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_ear_phone;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.pcba_audio_earphone);

        isCustomPath = getResources().getBoolean(R.bool.earphone_default_config_is_use_custom_path);
        mCustomPath = getResources().getString(R.string.earphone_default_config_custom_path);
        mCustomFileName = getResources().getString(R.string.earphone_default_config_custom_file_name);
        LogUtil.d("isCustomPath:"+isCustomPath+" mCustomPath:"+mCustomPath);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(STREAM_TYPE, 0, 0);

        mSession = new MediaSession(getApplicationContext(), this.getClass()
                .getName());
        mSession.setCallback(mCallback);
        mSession.setFlags(MediaSession.FLAG_EXCLUSIVE_GLOBAL_PRIORITY
                | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
        mSession.setActive(true);

    }

    private final MediaSession.Callback mCallback = new MediaSession.Callback() {

        public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
            if (isHeadsetExists()) {//&& mIsHeadsetCnPass&& mIsHeadsetUsPass) {

            }
            KeyEvent event = (KeyEvent) mediaButtonIntent
                    .getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            int key_action = event.getAction();
            if (event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
                if (key_action == KeyEvent.ACTION_UP) {
                    hasPressButton = true;
                    if (!mIsHeadsetCnPass && mIsHeadsetExistCn) { // CN headset
                        // test
                        if (isHeadsetExists() && showmessage && mHasVoicePass) {
                            mIsHeadsetCnPass = true;
//                            showText(R.string.headset_unplugged_us);
                            stopAudioLoopbackService();
                        }
                    }
                    if (mIsHeadsetCnPass && !mIsHeadsetUsPass
                            && mIsHeadsetExistUs) { // US headset test
                        if (isHeadsetExist && showmessage && mHasVoicePass) {
                            mIsHeadsetUsPass = true;
                            stopAudioLoopbackService();
                        }
                    }

                }
            }
            return true;
        }

        ;
    };

    private Handler mMediaHandler = new Handler(new Handler.Callback() {

        public boolean handleMessage(Message msg) {
            int type = getCnUsType();
            if (!isHeadsetExist) {
                return true;
            }
            if (mRetryCount >= 40) {
                return true;
            }
            if (type == HEADSET_PENDING) {
                mRetryCount++;
                mMediaHandler.sendEmptyMessageDelayed(1, 100);
                return true;
            }
            mRetryCount = 0;
            if (!mIsHeadsetCnPass && HEADSET_CN == getCnUsType()) {
                startAudioLoopbackService();
                mIsHeadsetExistCn = true;
                mIsHeadsetExistUs = false;
//                textview.setText(R.string.headset_plugged_cn);
                mAudioManager.setStreamVolume(STREAM_TYPE, 5, 0);
            }
            if (mIsHeadsetCnPass && !mIsHeadsetUsPass
                    && HEADSET_US == getCnUsType()) {
                startAudioLoopbackService();
                mIsHeadsetExistCn = false;
                mIsHeadsetExistUs = true;
//                textview.setText(R.string.headset_plugged_us);
                mAudioManager.setStreamVolume(STREAM_TYPE, 5, 0);
            }
            mSession.setActive(false);
            mSession = new MediaSession(getApplicationContext(), this
                    .getClass().getName());
            mSession.setCallback(mCallback);
            mSession.setFlags(MediaSession.FLAG_EXCLUSIVE_GLOBAL_PRIORITY
                    | MediaSession.FLAG_HANDLES_MEDIA_BUTTONS);
            mSession.setActive(true);
            return false;
        }
    });

    private boolean isHeadsetExists() {
        char[] buffer = new char[1024];

        int newState = 0;
        FileReader file = null;
        try {
            file = new FileReader(HEADSET_STATE_PATH);
            int len = file.read(buffer, 0, 1024);
            newState = Integer.valueOf((new String(buffer, 0, len)).trim());
            if (file != null) {
                file.close();
                file = null;
            }
        } catch (FileNotFoundException e) {
            LogUtil.e("This kernel does not have wired headset support");
        } catch (Exception e) {
            try {
                if (file != null) {
                    file.close();
                    file = null;
                }
            } catch (IOException io) {
            }
        }
        return newState != 0;
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(earphonePluginReceiver, filter);

        IntentFilter mfilter = new IntentFilter(AudioLoopbackService.ACTION_VOLUME_UPDATED);
        registerReceiver(mReceiver, mfilter);

        Intent intent = new Intent(getApplicationContext(), AudioLoopbackService.class);
        intent.putExtra("isEarPhone", true);
        startService(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
        unregisterReceiver(earphonePluginReceiver);
        unregisterReceiver(mReceiver);
        Intent intent = new Intent(getApplicationContext(), AudioLoopbackService.class);
        stopService(intent);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(AudioLoopbackService.ACTION_VOLUME_UPDATED)) {
                mVolumeView.setVolume(intent.getIntExtra("volume", 0));
                if (mVolumeView.getVolume() >= Short.MAX_VALUE * 0.8) {
                    if (isHeadsetExists()) {
                        LogUtil.d("isHeadsetExists");
                    }
                }
            }
        }
    };

    private BroadcastReceiver earphonePluginReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent earphoneIntent) {

            if (earphoneIntent != null && earphoneIntent.getAction() != null) {

                if (earphoneIntent.getAction().equalsIgnoreCase(Intent.ACTION_HEADSET_PLUG)) {

                    int st = 0;

                    st = earphoneIntent.getIntExtra("state", 0);

                    String nm = earphoneIntent.getStringExtra("name");

                    if (st == 0) {
                        mDialog.setSuccess(false);
                        ToastUtil.showBottomShort(getResources().getString(R.string.earphone_pulled_out));
                    } else if (st == 1) {
                        mDialog.setSuccess();
                        ToastUtil.showBottomShort(getResources().getString(R.string.earphone_inserted));
                    }

                }
            }

        }
    };

    private int getCnUsType() {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(HEADSET_TYPE_PATH));
            String type = reader.readLine();
            int headsetType = Integer.parseInt(type);
            if (HEADSET_CN == headsetType) {
                return HEADSET_CN;
            } else if (HEADSET_US == headsetType) {
                return HEADSET_US;
            } else if (HEADSET_PENDING == headsetType) {
                return HEADSET_PENDING;
            }
        } catch (IOException e) {
            return HEADSET_UNKNOW;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }

        return HEADSET_UNKNOW;

    }

    private void startAudioLoopbackService() {
        Intent intent = new Intent(getApplicationContext(),
                AudioLoopbackService.class);
        intent.putExtra("isEarPhone", true);
        startService(intent);
    }

    private void stopAudioLoopbackService() {
        Intent intent = new Intent(getApplicationContext(),
                AudioLoopbackService.class);
        intent.setAction(AudioLoopbackService.ACTION_STOP);
        startService(intent);
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void deInit(int results) {
        if (mDialog.isShowing()) mDialog.dismiss();
        updateData(mFatherName, super.mName, results);
        Intent intent = new Intent();
        intent.putExtra("results", results);
        setResult(1111, intent);
        mContext.finish();
    }

    private void deInit(int results, String reason) {
        if (mDialog.isShowing()) mDialog.dismiss();
        updateData(mFatherName, super.mName, results, reason);
        Intent intent = new Intent();
        intent.putExtra("results", results);
        setResult(1111, intent);
        mContext.finish();
    }


    private void playVideo(){
        mMediaPlayer = new MediaPlayer();
        try {
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.reset();
            setDataSource(isCustomPath);
            mMediaPlayer.start();
        } catch (Exception e) {
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
    }

    private void setDataSource(boolean isCustom){
        try {
            if (isCustom){
                if (TextUtils.isEmpty(mCustomPath) && TextUtils.isEmpty(mCustomFileName)){
                    sendErrorMsgDelayed(mHandler,"the custom file path is null");
                    return;
                }
                File file = FileUtil.createRootDirectory(mCustomPath);
                File file1 = FileUtil.mkDir(file);
                File f = new File(file1.getPath(),mCustomFileName);

                mMediaPlayer.setDataSource(f.getPath());
            }else {
                AssetFileDescriptor afd = this.getAssets().openFd("music.mp3");
                mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            }
            mMediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0) {
            deInit(result, Const.RESULT_NOTEST);
        } else if (result == 1) {
            deInit(result, Const.RESULT_UNKNOWN);
        } else if (result == 2) {
            deInit(result);
        }
    }

    public void onPlay(View view) {
        playVideo();
    }

    public void onRecord(View view) {
    }


}
