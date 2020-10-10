package com.example.foraa;

import android.app.Activity;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.view.SurfaceView;

public class CameraActivity extends Activity {
        private Camera mCamera;
        private SurfaceView preview;
        private MediaRecorder mediaRecorder;

        @Override
        protected void onPause() {
            super.onPause();
            releaseMediaRecorder();       // if you are using MediaRecorder, release it first
            releaseCamera();              // release the camera immediately on pause event
        }

        private void releaseMediaRecorder(){
            if (mediaRecorder != null) {
                mediaRecorder.reset();   // clear recorder configuration
                mediaRecorder.release(); // release the recorder object
                mediaRecorder = null;
                mCamera.lock();           // lock camera for later use
            }
        }

        private void releaseCamera(){
            if (mCamera != null){
                mCamera.release();        // release the camera for other applications
                mCamera = null;
            }
        }
    }

