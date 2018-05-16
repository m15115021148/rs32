package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class RearCameraAutoActivity extends BaseActivity implements View.OnClickListener, PromptDialog.OnPromptDialogCallBack {
    private RearCameraAutoActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";

    private int mConfigResult;
    private int mConfigTime;
    private Runnable mRun;

    @BindView(R.id.surfaceView)
    public SurfaceView mSurfaceView = null;
    private SurfaceHolder mHolder = null;
    private Camera mCamera = null;
    private int mCameraId = 0;
    /**
     * preview width
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview height
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * picture width
     */
    private static final int PICTURE_WIDTH = 640;
    /**
     * picture height
     */
    private static final int PICTURE_HEIGHT = 480;
    /**
     * preview width
     */
    private static final int PREVIEW_WIDTH_BACK = 1280;
    /**
     * preview height
     */
    private static final int PREVIEW_HEIGHT_BACK = 720;
    /**
     * picture width
     */
    private static final int PICTURE_WIDTH_BACK = 1280;
    /**
     * picture height
     */
    private static final int PICTURE_HEIGHT_BACK = 720;

    @BindView(R.id.preview)
    public Button previewButton;
    @BindView(R.id.btn_retry)
    public Button switchButton;
    @BindView(R.id.take_picture)
    public Button takepicture;
    @BindView(R.id.flashlight)
    public TextView flashlight;
    @BindView(R.id.ImageView)
    public ImageView imageView;

    private boolean isCanOpen = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_rear_camera_auto;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_rear_camera);

        mConfigResult = getResources().getInteger(R.integer.rear_camera_auto_default_config_standard_result);
        mConfigTime = getResources().getInteger(R.integer.run_in_test_default_time);
        mConfigTime = mConfigTime * 60;
        LogUtil.d("mConfigResult:" + mConfigResult + " mConfigTime:" + mConfigTime);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName, super.mName);
        LogUtil.d(mName);

        if (super.mName.equals(getResources().getString(R.string.pcba_rear_camera))){
            mCameraId = 1;
        } else if (super.mName.equals(getResources().getString(R.string.pcba_camera))){
            mCameraId = 0;
        }

        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(new SurfaceHolder.Callback() {

            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            public void surfaceCreated(SurfaceHolder holder) {
                startCamera(mCameraId);
            }

            public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                       int height) {

            }
        });
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        flashlight.setOnClickListener(this);
        if (!isCameraFlashEnable()) {
            flashlight.setVisibility(View.GONE);
        }
        switchButton.setOnClickListener(this);
        System.out.println(Camera.getNumberOfCameras());
        if (!isCameraFrontEnable(Camera.getNumberOfCameras())) {
            switchButton.setVisibility(View.GONE);
        } else {
            switchButton.setEnabled(false);
        }
        takepicture.setOnClickListener(this);
        takepicture.setEnabled(true);
        imageView.setVisibility(View.GONE);
        previewButton.setOnClickListener(this);
        previewButton.setEnabled(false);

        mRun = new Runnable() {
            @Override
            public void run() {
                mConfigTime--;
                if (mConfigTime == 0) {
                    mHandler.sendEmptyMessage(1001);
                }
                mHandler.postDelayed(this, 1000);
            }
        };
        mRun.run();

    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1001:
                    if (isCanOpen){
                        deInit(SUCCESS);
                    }else {
                        deInit(FAILURE,"camera is not open");
                    }
                    break;
                case 1002:
                    deInit(FAILURE,Const.RESULT_UNKNOWN);
                    break;
                case 9999:
                    deInit(FAILURE,msg.obj.toString());
                    break;
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCamera();
        mHandler.removeCallbacks(mRun);
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(9999);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCamera();
    }

    @Override
    public void onClick(View v) {
        if (v == mBack) {
            if (!mDialog.isShowing()) mDialog.show();
            mDialog.setTitle(super.mName);
        }
        if (v == switchButton){
            takepicture.setEnabled(true);
            previewButton.setEnabled(false);
            switchButton.setEnabled(false);
            mSurfaceView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            mCameraId = (mCameraId == 0)? 1 : 0;
            stopCamera();
            startCamera(mCameraId);
        }
        if (v == previewButton){
            if(mCameraId == 0){
                //hasPressPreview = true;
            }else if(mCameraId == 1){
                //hasPressPreviewfront = true;
            }
            takepicture.setEnabled(false);
            previewButton.setEnabled(false);
            switchButton.setEnabled(true);
            mSurfaceView.setVisibility(View.VISIBLE);
            imageView.setVisibility(View.GONE);
            startCamera(mCameraId);
        }
        if(v == takepicture){
            if(mCameraId == 0){
               // hasPressTakePic = true;
            }else if(mCameraId == 1){
                //hasPressTakePicfront = true;
            }
            takepicture.setEnabled(false);
            previewButton.setEnabled(true);
            switchButton.setEnabled(false);
            if(mCamera != null){
                mCamera.takePicture(null, null, picCallBack);
            }
            mSurfaceView.setVisibility(View.GONE);
            imageView.setVisibility(View.VISIBLE);
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

    private void deInit(int results,String reason){
        if (mDialog.isShowing())mDialog.dismiss();
        updateData(mFatherName,super.mName,results,reason);
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    public void onResultListener(int result) {
        if (result == 0){
            deInit(result,Const.RESULT_NOTEST);
        }else if (result == 1){
            deInit(result,Const.RESULT_UNKNOWN);
        }else if (result == 2){
            deInit(result);
        }
    }

    private Camera.PictureCallback picCallBack = new Camera.PictureCallback() {

        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                Bitmap bm = BitmapFactory.decodeByteArray(data, 0, data.length);
                Matrix matrix = new Matrix();

                if (mCameraId == 0) {
                    matrix.postRotate(90);
                }
                if (mCameraId == 1) {
                    matrix.postRotate(270);
                }

                if (bm != null) {
                    Bitmap bitMap = Bitmap.createBitmap(bm, 0, 0,
                            bm.getWidth(), bm.getHeight(), matrix, true);
                    bm = bitMap;
                    imageView.setImageBitmap(bm);
                    stopCamera();
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendErrorMsg(mHandler,e.getMessage());
            }
        }
    };

    private boolean isCameraFrontEnable(int cameraNumbers) {
        return cameraNumbers > 1;
    }

    private boolean isCameraFrontEnable() {
        PackageManager pm = (PackageManager) this.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo f : features) {
            if (PackageManager.FEATURE_CAMERA_FRONT.equals(f.name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isCameraFlashEnable() {
        PackageManager pm = (PackageManager) this.getPackageManager();
        FeatureInfo[] features = pm.getSystemAvailableFeatures();
        for (FeatureInfo f : features) {
            if (PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                return true;
            }
        }
        return false;
    }

    private void startCamera(int CameraId) {
        Camera.Parameters parameters = null;
        if (mCamera != null)
            return;
        try {
            mCamera = Camera.open(CameraId);
        } catch (RuntimeException e) {
            LogUtil.d("startCamera failed");
            e.printStackTrace();
            mCamera = null;
            sendErrorMsgDelayed(mHandler,e.getMessage());
        }
        if (mCamera != null) {
            setCameraDisplayOrientation(CameraId, mCamera);
            parameters = mCamera.getParameters();
            parameters.set("orientation", "portrait");
            if (CameraId == 1) {
                parameters.setPreviewSize(PREVIEW_WIDTH, PREVIEW_HEIGHT);
                parameters.setPictureSize(PICTURE_WIDTH, PICTURE_HEIGHT);
            } else {
                parameters.setPreviewSize(PREVIEW_WIDTH_BACK, PREVIEW_HEIGHT_BACK);
                parameters.setPictureSize(PICTURE_WIDTH_BACK, PICTURE_HEIGHT_BACK);
            }
            parameters.setPictureFormat(PixelFormat.JPEG);
            parameters.setPictureSize(PICTURE_WIDTH, PICTURE_HEIGHT);
            if (CameraId == 0) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }
            parameters.setAntibanding("50hz");
            mCamera.setParameters(parameters);
            try {
                mCamera.setPreviewDisplay(mHolder);
                LogUtil.d("start preview");
                isCanOpen =  true;
                mCamera.startPreview();
                mDialog.setSuccess();
            } catch (Exception e) {
                mCamera.release();
                mCamera = null;
                sendErrorMsgDelayed(mHandler,e.getMessage());
            }
        }
    }


    private void setCameraDisplayOrientation(int cameraId, Camera camera) {
        int degrees = 0;
        int result;

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private void stopCamera() {
        try {
            if (mCamera != null) {
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                mCamera.setParameters(parameters);
                mCamera.stopPreview();
                mCamera.release();
                mCamera = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
