package com.meigsmart.meigrs32.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.meigsmart.meigrs32.R;
import com.meigsmart.meigrs32.config.Const;
import com.meigsmart.meigrs32.log.LogUtil;
import com.meigsmart.meigrs32.util.FileUtil;
import com.meigsmart.meigrs32.view.PromptDialog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import butterknife.BindView;

public class MemoryActivity extends BaseActivity implements View.OnClickListener ,PromptDialog.OnPromptDialogCallBack{
    private MemoryActivity mContext;
    @BindView(R.id.title)
    public TextView mTitle;
    @BindView(R.id.back)
    public LinearLayout mBack;
    private String mFatherName = "";
    @BindView(R.id.progressBar)
    public ProgressBar mProgress;
    @BindView(R.id.result)
    public TextView mResult;
    private int progressValue;
    private int length;//标记文件大小
    private int type = 0;//0  assets中读取
    private String path = "";
    private Thread mWriteThread;
    private Thread mReadThread;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_memory;
    }

    @Override
    protected void initData() {
        mContext = this;
        super.startBlockKeys = Const.isCanBackKey;
        mBack.setVisibility(View.VISIBLE);
        mBack.setOnClickListener(this);
        mTitle.setText(R.string.run_in_memory);

        mDialog.setCallBack(this);
        mFatherName = getIntent().getStringExtra("fatherName");
        super.mName = getIntent().getStringExtra("name");
        addData(mFatherName,super.mName);

        path = FileUtil.createInnerPath(this,"memory_test.txt");

        init(FileUtil.getFileSize(new File(path))>0?path:"");
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1001:
                    progressValue += msg.arg1;
                    mProgress.setProgress(progressValue);
                    if(progressValue == length){
                        mHandler.sendEmptyMessage(1002);
                    }
                    mResult.setText(msg.obj.toString());
                    break;
                case 1002:
                    initWrite(mResult.getText().toString());
                    break;
                case 1003:
                    progressValue += msg.arg1;
                    mProgress.setProgress(progressValue);
                    if(progressValue == length){
                        mHandler.sendEmptyMessage(1004);
                    }
                    mResult.setText(msg.obj.toString());
                    break;
                case 1004:
                    mResult.setText("");
                    progressValue = 0;
                    mProgress.setProgress(0);

                    init(path);
                    break;
                default:
                    break;
            }
        }
    };

    private void init(final String path) {
        if (TextUtils.isEmpty(path)){
            type =0;
        }else{
            File file = new File(path);
            if (file.exists()) {
                type = 1;
                length = (int) file.length();
                mProgress.setMax(length);//设置进度条最大值
            }else{
                deInit(1);
                return;
            }
        }

        mReadThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (type == 1){
                    readFromFile(path);
                }else{
                    readFromResets();
                }
            }
        },"readFile");
        mReadThread.start();

    }

    @Override
    public void onClick(View v) {
        if (v == mBack){
            stop();
            if (!mDialog.isShowing())mDialog.show();
            mDialog.setTitle(super.mName);
        }
    }

    private void stop(){
        mHandler.removeMessages(1001);
        mHandler.removeMessages(1002);
        mHandler.removeMessages(1003);
        mHandler.removeMessages(1004);
//        if (mReadThread!=null)mReadThread.interrupt();
//        if (mWriteThread!=null)mWriteThread.interrupt();
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
    public void onResultListener(int result) {
        deInit(result);
    }

    public void readFromFile(String path) {
        try {
            int line;
            FileInputStream fis = new FileInputStream(path);
            DataInputStream dis = new DataInputStream(fis);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                Message msg = new Message();
                msg.what = 1001;
                msg.arg1 = line;
                msg.obj = sb.toString();
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            dis.close();
            fis.close();
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            deInit(1);
        }
    }

    private void initWrite(final String msg){
        if (TextUtils.isEmpty(msg)){
            deInit(1);
            return;
        }
        progressValue = 0;
        mProgress.setProgress(0);
        length = (int) msg.length();
        mProgress.setMax(length);//设置进度条最大值

        mWriteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeFile(msg);
            }
        },"writeFile");
        mWriteThread.start();
    }

    private void writeFile(String data){
        try {
            OutputStream out = new FileOutputStream(path);
            InputStream is = new ByteArrayInputStream(data.getBytes());
            StringBuffer sb = new StringBuffer();
            byte[] buff = new byte[1];
            int len = 0;
            while((len=is.read(buff))!=-1){
                out.write(buff, 0, len);

                String mData = new String(buff, 0, len);
                sb.append(mData);

                Message msg = new Message();
                msg.what = 1003;
                msg.arg1 = len;
                msg.obj = data.replace(sb.toString(),"");
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            is.close();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtil.e(e.getMessage());
            deInit(1);
        }
    }

    public void readFromResets() {
        try {
            int line;
            InputStream is = this.getAssets().open("memory.txt");

            length = is.available();
            mProgress.setMax(length);//设置进度条最大值
            DataInputStream dis = new DataInputStream(is);
            StringBuffer sb = new StringBuffer();
            byte b[] = new byte[1];
            while ((line = dis.read(b)) != -1) {
                String mData = new String(b, 0, line);
                sb.append(mData);
                Message msg = new Message();
                msg.what = 1001;
                msg.arg1 = line;
                msg.obj = sb.toString();
                mHandler.sendMessage(msg);
                Thread.sleep(50);
            }
            dis.close();
            is.close();
        } catch (Exception e) {
            LogUtil.e(e.getMessage());
            deInit(1);
        }
    }

}
