package com.example.foraa;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraDevice;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

@RequiresApi(api = Build.VERSION_CODES.M)

public class MainActivity extends AppCompatActivity {
    private Button flipCamera, pictureButton, selectPicture;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CameraDevice camera2;
    private Camera mCamera;
    private CameraPreview mPreview;
    private int camFlipFlop = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
        createCamPreview(camFlipFlop);
        flipCamera = findViewById(R.id.flipCamera);
        flipCamera.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCamera.release();
                if (camFlipFlop == 1) camFlipFlop = 0;
                else camFlipFlop = 1;
                createCamPreview(camFlipFlop);
            }
        });
        pictureButton = findViewById(R.id.pictureButton);
        pictureButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mCamera.takePicture(null,null,null,null);
            }
        });

    }

    public void createCamPreview(int cam) {
        mCamera = getCameraInstance(cam);
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    public static Camera getCameraInstance(int cam) {
        Camera c = null;
        try {
            c = Camera.open(cam); // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
}
