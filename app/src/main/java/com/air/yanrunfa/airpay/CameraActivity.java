package com.air.yanrunfa.airpay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.air.yanrunfa.airpay.MainActivity;
import com.air.yanrunfa.airpay.R;
import com.air.yanrunfa.airpay.RegisterActivity;
import com.air.yanrunfa.palmprint.Palmprint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * Created by zyson on 16-4-9.
 */
public class CameraActivity extends Activity implements View.OnClickListener, Camera.PictureCallback {
    private final static String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/AirPay/";
    private final static int EXTRACTION_FAIL = 0x111;
    private final static int EXTRACTION_SUCCEED=0x112;
    private CameraPreview mPreview = null;
    public static Button mCaptureButton = null;
    private String TAG = "Zyson";
    private int failNum = 0;
    public Bitmap bitmap;
    public mHandler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // Create our Preview view
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(this);
        preview.addView(mPreview);

        //Add listener to the caputure button
        mCaptureButton = (Button) findViewById(R.id.capture_button);
        mCaptureButton.setOnClickListener(this);
    }

    @Override
    public void onPictureTaken(byte[] data, Camera camera) {

        bitmap = decodeToBitMap(data, camera);
        Palmprint palmprint = new Palmprint(bitmap);
        Bundle bundle = new Bundle();
        try {
            bundle.putByteArray("palmprint", palmprint.extraction());
            Message msg = new Message();
            msg.what = 0x112;
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        } catch (IOException e) {
            failNum++;
            if (failNum == 5) {
                bitmap = Bitmap.createBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), 500, 500);
                bundle.putByteArray("palmprint", palmprint.extractionWithROI(bitmap));
                Message msg = new Message();
                msg.what = 0x112;
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            } else {
                Message msg = new Message();
                msg.what = 0x111;
                mHandler.sendMessage(msg);
            }
        }

        // Restart Preview
        camera.startPreview();

        // See if need to enable or not
        mCaptureButton.setEnabled(true);
    }

    private Bitmap decodeToBitMap(byte[] data, Camera _camera) {
        Camera.Size size = _camera.getParameters().getPreviewSize();
        Bitmap bitmap;
        try {
            YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
            bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
            stream.close();
            Matrix matrix = new Matrix();
            matrix.reset();
            matrix.setRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return bitmap;
        } catch (Exception ex) {
            Log.e("Sys", "Error:" + ex.getMessage());
        }
        return null;
    }

    @Override
    public void onClick(View v) {
        mCaptureButton.setEnabled(false);

        mPreview.takePicture(this);
    }

    static class mHandler extends Handler {
        WeakReference<CameraActivity> mActivity;

        mHandler(CameraActivity activity){
            mActivity=new WeakReference<CameraActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            CameraActivity cameraActivity=mActivity.get();
            Log.v("aaa",String.valueOf(msg.what));
            if (msg.what==EXTRACTION_SUCCEED){
                Toast toast=Toast.makeText(cameraActivity,"掌纹提取成功",Toast.LENGTH_SHORT);
                toast.show();
                if (cameraActivity.getIntent().getBooleanExtra("register",false)){
                    Intent intent=new Intent(cameraActivity, RegisterActivity.class);
                    intent.putExtra("palmprint",msg.getData());
                    intent.putExtra("ip",cameraActivity.getIntent().getStringExtra("ip"));
                    cameraActivity.startActivity(intent);
                    //cameraActivity.mCamera.release();

                }else {
                    Intent intent=new Intent(cameraActivity, MainActivity.class);
                    //Intent intent=new Intent(cameraActivity, PayActivity.class);
                    intent.putExtra("isLogin",true);
                    intent.putExtra("palmprint",msg.getData());
                    //  intent.putExtra("ip",cameraActivity.getIntent().getStringExtra("ip"));
                    cameraActivity.startActivity(intent);
                    //cameraActivity.mCamera.release();
                }
            }
            else if (msg.what==EXTRACTION_FAIL){
                //Toast toast=Toast.makeText(cameraActivity,"掌纹提取失败",Toast.LENGTH_SHORT);
                //toast.show();
            }
        }
    }
}
