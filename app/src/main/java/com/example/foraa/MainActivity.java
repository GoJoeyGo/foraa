package com.example.foraa;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.*;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;
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
            c = Camera.open(cam); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("Camera Error", "Camera is not available (in use or does not exist)");
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
            //noinspection deprecation
            FirebaseVisionObjectDetectorOptions option = new FirebaseVisionObjectDetectorOptions.Builder()
                    .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                    .enableMultipleObjects()
                    .enableClassification().build();
            FirebaseVisionObjectDetector objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(option);
            // FirebaseVisionImageLabeler foodLaberler = FirebaseVision.getInstance().getCloudImageLabeler();
            Log.d(TAG, "cool");
            final TextView txtView = (TextView) findViewById(R.id.textView3);
            final Bitmap finalImageBitmap = imageBitmap;
            objectDetector.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionObject>>(){
               @Override
               public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                   getobjectSubImage(finalImageBitmap,firebaseVisionObjects);
               }
           });
        }
    };

    //Was unable to Get Bounding from FirebaseVisionObjectDetectorOptions as it kept geting rejected but this is how it would work
    public int getobjectSubImage(final Bitmap image, List<FirebaseVisionObject> firebaseVisionObjects){
        for (FirebaseVisionObject object : firebaseVisionObjects) {
            int lineWidth = 10;
            final Paint orange = new Paint();
            orange.setColor(Color.rgb(255,127,0));
            orange.setStyle(Paint.Style.STROKE);
            orange.setStrokeWidth(lineWidth);

            final Paint redApple = new Paint();
            redApple.setColor(Color.RED);
            redApple.setStyle(Paint.Style.STROKE);
            redApple.setStrokeWidth(lineWidth);

            final Paint greenApple = new Paint();
            greenApple.setColor(Color.GREEN);
            greenApple.setStyle(Paint.Style.STROKE);
            greenApple.setStrokeWidth(lineWidth);

            final Paint banana = new Paint();
            banana.setColor(Color.rgb(255,255,0));
            banana.setStyle(Paint.Style.STROKE);
            banana.setStrokeWidth(lineWidth);

            Paint passion_fruit = new Paint();
            passion_fruit.setColor(Color.rgb(128,0,128));
            passion_fruit.setStyle(Paint.Style.STROKE);
            passion_fruit.setStrokeWidth(lineWidth);
            final Paint text = new Paint();
            text.setColor(Color.BLACK);
            text.setTextSize(100);
            text.setTextAlign(Paint.Align.CENTER);
            final Rect rect = object.getBoundingBox();
            Bitmap resultBmp = Bitmap.createBitmap(image,rect.left,rect.top,rect.right-rect.left, rect.bottom-rect.top);
            FirebaseVisionImageLabeler foodLabeler = FirebaseVision.getInstance().getCloudImageLabeler();
            FirebaseVisionImage fireImage = FirebaseVisionImage.fromBitmap(resultBmp);
            foodLabeler.processImage(fireImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>(){
                @Override
                public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels) {
                    for (FirebaseVisionImageLabel label : firebaseVisionImageLabels) {
                        Canvas canvas = new Canvas(image);
                        Log.d("Fruit ",label.getText()+" "+label.getConfidence());
                      if(label.getText().equals("Banana")){
                          Log.d("Banana Detected",label.getText());
                          canvas.drawRect(rect.left,rect.top,rect.right,rect.bottom,banana);
                          canvas.drawText("Banana",(rect.right+rect.left)/2,(rect.top+rect.bottom)/2,text);
                      }
                      else if(label.getText().equals("Orange")){
                            canvas.drawRect(rect.left,rect.top,rect.right,rect.bottom,orange);
                            canvas.drawText("Orange",(rect.right+rect.left)/2,(rect.top+rect.bottom)/2,text);
                      }
                      else if(label.getText().equals("Granny smith")){
                          canvas.drawRect(rect.left,rect.top,rect.right,rect.bottom,greenApple);
                          canvas.drawText("Green Apple",(rect.right+rect.left)/2,(rect.top+rect.bottom)/2,text);
                      }
                      else if(label.getText().equals("Apple")){
                          canvas.drawRect(rect.left,rect.top,rect.right,rect.bottom,redApple);
                          canvas.drawText("Red Apple",(rect.right+rect.left)/2,(rect.top+rect.bottom)/2,text);
                      }
                    }
                    Log.d("Exit"," ");
                    imageView.setImageBitmap(image);
                }
            });
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
