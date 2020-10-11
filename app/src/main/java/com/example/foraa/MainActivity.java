package com.example.foraa;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.io.IOException;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.M)

public class MainActivity extends AppCompatActivity {
    private Button flipCamera, pictureButton, selectPicture;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private CameraDevice camera2;
    private Camera mCamera;
    private CameraPreview mPreview;
    private int camFlipFlop = 0;
    private int imageFlipFlop = 0;
    private int PICK_FROM_CAMERA = 1;
    private CameraPreview mCameraPreview;
    String TAG = "TAG";
    int height=1;
    private byte[] image;
    private static Bitmap photo = null;
    Bitmap cameraBitmap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pictureButton = findViewById(R.id.pictureButton);
        flipCamera = findViewById(R.id.flipCamera);
        selectPicture = findViewById(R.id.cameraRollButton);
        imageView = findViewById(R.id.imageView2);
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        createCamPreview(camFlipFlop);
        flipCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.release();
                if (camFlipFlop == 1) camFlipFlop = 0;
                else camFlipFlop = 1;
                createCamPreview(camFlipFlop);
            }
        });
        pictureButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 if (imageFlipFlop == 1){
                     imageFlipFlop = 0;
                     mCamera.takePicture(null, null, mPicture);
                 }
                 else{
                     createCamPreview(camFlipFlop);
                     imageView.setVisibility(View.INVISIBLE);
                     imageFlipFlop = 1;
                 }
             }
            }
        );
        selectPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent c = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(c, 1);
            }
        });
    }
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        Bitmap imageBitmap = BitmapFactory.decodeByteArray(data.getByteArrayExtra());
//        imageBitmap = RotateBitmap(imageBitmap,-90);
//    }
        static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    Bitmap imageBitmap;
    private static Uri mImageCaptureUri;

    public void createCamPreview(int cam) {
        mCamera = getCameraInstance(cam);
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        height = preview.getHeight();
    }

    public static Camera getCameraInstance(int cam) {
        Camera c = null;
        try {
            c = Camera.open(cam);
            // attempt to get a Camera instance
        } catch (Exception e) {
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }
    byte[] imgData;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.release();
            imageView.setVisibility(View.VISIBLE);
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            imageBitmap = RotateBitmap(imageBitmap,-90);
            if(camFlipFlop==1){
                imageBitmap = createFlippedBitmap(imageBitmap);
            }else{
                imageBitmap = RotateBitmap(imageBitmap,180);
            }
            imageView.setImageBitmap(imageBitmap);
            final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
            FirebaseVisionObjectDetectorOptions option = new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification().build();
            final FirebaseVisionObjectDetector objectDetector =
                    FirebaseVision.getInstance().getOnDeviceObjectDetector();
            FirebaseVisionImageLabeler foodLaberler = FirebaseVision.getInstance().getCloudImageLabeler();
            Log.d(TAG, "cool");
            final TextView txtView = (TextView) findViewById(R.id.textView3);
          objectDetector.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionObject>>(){
               @Override
               public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                   for (FirebaseVisionObject object : firebaseVisionObjects) {
                       Log.d(TAG, String.valueOf(object.getBoundingBox()));
                   }
               }
           });
//           foodLaberler.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
//                @Override
//                public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels) {
//                    objectDetector.processImage(image);
//                    String egg = " ";
//                    for (FirebaseVisionImageLabel label : firebaseVisionImageLabels) {
//                        Log.d(TAG,label.getText());
//                    }
//                    txtView.setMovementMethod(new ScrollingMovementMethod());
//                    txtView.setTextSize(24);
//                    txtView.setText(egg);
//                }
//            });
        }
    };

    //Was unable to Get Bounding from FirebaseVisionObjectDetectorOptions as it kept geting rejected but this is how it would work
    public static int getobjectSubImage(Bitmap image,List<FirebaseVisionObject> firebaseVisionObjects){
        for (FirebaseVisionObject object : firebaseVisionObjects) {
            Rect rect = object.getBoundingBox();
            Bitmap resultBmp = Bitmap.createBitmap(image,rect.left,rect.top,rect.right-rect.left, rect.bottom-rect.top);
        }
        return 1;
    }
    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    public static Bitmap createFlippedBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    }
