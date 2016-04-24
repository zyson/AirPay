package com.air.yanrunfa.airpay;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

/**
 * Created by zyson on 16-4-9.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private String TAG = "Zyson";

    public CameraPreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);

        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated() is called");

        try {
            mCamera = Camera.open();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

        Log.d(TAG, "surfaceChanged() is called");

        try {
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
        //实现自动对焦
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success){
                    initCamera();
                    camera.cancelAutoFocus();//只有加上了这一句，才会自动对焦。
                }
            }

        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        Log.d(TAG, "surfaceDestroyed() is called");
    }

    public void takePicture(Camera.PictureCallback imageCallback){
        mCamera.takePicture(null, null, imageCallback);
    }

    //相机参数的初始化设置
    private void initCamera()
    {
        Camera.Parameters parameters;
        parameters=mCamera.getParameters();
        parameters.setPictureFormat(PixelFormat.JPEG);
        //parameters.setPictureSize(surfaceView.getWidth(), surfaceView.getHeight());  // 部分定制手机，无法正常识别该方法。
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//1连续对焦
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上

    }
}
