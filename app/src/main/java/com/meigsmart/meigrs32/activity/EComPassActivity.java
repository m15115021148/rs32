package com.meigsmart.meigrs32.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import butterknife.BindView;

public class EComPassActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private EComPassActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.layout)
    public View mView;

    private Bitmap mBitmap, mBackground, mPointer;
    private Canvas mCanvas;
    private SensorManager mManager;
    private Sensor mOSensor;
    private Sensor mMSensor;
    private SensorEventListener mOListener;
    private SensorEventListener mMListener;
    private static final float FULL_DEGREES = 360f;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_e_compass;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_e_compass);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        initDrawer();
        initSensor();
    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void deInit(int results){
        if (mDialog.isShowing())mDialog.dismiss();
        updateData(mFatherName,super.mName,results);
        Intent intent = new Intent();
        intent.putExtra("results",results);
        setResult(1111,intent);
        mContext.finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mManager.registerListener(mOListener, mOSensor, SensorManager.SENSOR_DELAY_UI);
        mManager.registerListener(mMListener, mMSensor, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        mManager.unregisterListener(mOListener);
        mManager.unregisterListener(mMListener);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mManager.unregisterListener(mOListener);
        mManager.unregisterListener(mMListener);
        mBackground.recycle();
        mPointer.recycle();
        mBitmap.recycle();
        mCanvas = null;
        super.onDestroy();
    }

    private void initSensor() {
        mManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mOSensor = mManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mOListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                draw(x);
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        mMSensor = mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mMListener = new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                showInfo(x, y, z);
            }

            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
    }

    private void showInfo(float x, float y, float z) {
        StringBuffer sb = new StringBuffer();
        if(mMSensor != null)
            sb.append("chip id: " + mMSensor.getName() + "\n");
        sb.append(" X = " + x + "\n");
        sb.append(" Y = " + y + "\n");
        sb.append(" Z = " + z + "\n");
    }

    private void initDrawer() {
        mBackground = BitmapFactory.decodeResource(getResources(), R.drawable.compass_bg);
        mPointer = BitmapFactory.decodeResource(getResources(), R.drawable.compass_p);
        mBitmap = Bitmap.createBitmap(mPointer.getWidth(), mPointer.getHeight(), Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    private void draw(float value) {
        Matrix matrix = new Matrix();
        Paint paint = new Paint();
        float width = mPointer.getWidth() / (float) 2;
        float height = mPointer.getHeight() / (float) 2;
        matrix.postRotate(FULL_DEGREES - value, width, height);

        mCanvas.drawBitmap(mBackground, new Matrix(), paint);
        mCanvas.drawBitmap(mPointer, matrix, paint);

        mView.setBackgroundDrawable(new BitmapDrawable(mBitmap));
    }

    @Override
    public void onResultListener(int result) {
        deInit(result);
    }
}
