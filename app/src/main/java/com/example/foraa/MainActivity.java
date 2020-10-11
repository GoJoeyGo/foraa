package com.example.foraa;

import android.Manifest;
import android.content.Context;
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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.io.IOException;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.M)

public class MainActivity extends AppCompatActivity {
    int height = 1;
    private Camera mCamera;
    private int camFlipFlop = 0;
    private int imageFlipFlop = 0;
    private ImageView imageView;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            mCamera.release();
            imageView.setVisibility(View.VISIBLE);
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            imageBitmap = RotateBitmap(imageBitmap, -90);
            if (camFlipFlop == 1) {
                imageBitmap = createFlippedBitmap(imageBitmap);
            } else {
                imageBitmap = RotateBitmap(imageBitmap, 180);
            }
            imageView.setImageBitmap(imageBitmap);
            processImage(imageBitmap);
        }
    };

    private static Camera getCameraInstance(int cam) {
        Camera c = null;
        try {
            c = Camera.open(cam); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("Camera Error", "Camera is not available (in use or does not exist)");
        }
        return c; // returns null if camera is unavailable
    }

    private static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static Bitmap createFlippedBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postScale(-1, 1, source.getWidth() / 2f, source.getHeight() / 2f);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        createCamPreview(camFlipFlop);
    }

    protected void onDestroy() {
        super.onDestroy();
        mCamera.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button pictureButton = findViewById(R.id.pictureButton);
        Button flipCamera = findViewById(R.id.flipCamera);
        Button selectPicture = findViewById(R.id.cameraRollButton);
        imageView = findViewById(R.id.imageView2);

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        if (!isNetworkAvailable()){
            Toast.makeText(this, "network error", Toast.LENGTH_LONG).show();

        }
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
                                                 if (imageFlipFlop == 1) {
                                                     imageFlipFlop = 0;
                                                     mCamera.takePicture(null, null, mPicture);
                                                 } else {
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
                Intent c = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(c, 1);
            }
        });
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            Uri uri = data.getData();
            imageView.setImageURI(uri);
            Bitmap imageBitmap;
            try {
                imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                Bitmap image = imageBitmap.copy(imageBitmap.getConfig(), true);
                processImage(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createCamPreview(int cam) {
        mCamera = getCameraInstance(cam);
        CameraPreview mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        height = preview.getHeight();
    }

    private void processImage(Bitmap imageBitmap) {
        final FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(imageBitmap);
        //noinspection deprecation
        FirebaseVisionObjectDetectorOptions option = new FirebaseVisionObjectDetectorOptions.Builder()
                .setDetectorMode(FirebaseVisionObjectDetectorOptions.SINGLE_IMAGE_MODE)
                .enableMultipleObjects()
                .enableClassification().build();
        //noinspection deprecation
        FirebaseVisionObjectDetector objectDetector = FirebaseVision.getInstance().getOnDeviceObjectDetector(option);
        final Bitmap finalImageBitmap = imageBitmap;
        //noinspection deprecation
        objectDetector.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionObject>>() {
            @Override
            public void onSuccess(List<FirebaseVisionObject> firebaseVisionObjects) {
                getObjectSubImage(finalImageBitmap, firebaseVisionObjects);
            }
        });
    }

    private void getObjectSubImage(final Bitmap image, List<FirebaseVisionObject> firebaseVisionObjects) {
        //noinspection deprecation
        for (FirebaseVisionObject object : firebaseVisionObjects) {
            int lineWidth = 10;
            //Fruit colors
            final Paint orange = new Paint();
            orange.setColor(Color.rgb(255, 127, 0));
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
            banana.setColor(Color.rgb(255, 255, 0));
            banana.setStyle(Paint.Style.STROKE);
            banana.setStrokeWidth(lineWidth);

            //I could not get the app to detect passion Fruits
            final Paint lemon = new Paint();
            lemon.setColor(Color.rgb(128, 0, 128));
            lemon.setStyle(Paint.Style.STROKE);
            lemon.setStrokeWidth(lineWidth);

            final Paint text = new Paint();
            text.setColor(Color.BLACK);
            text.setTextSize(150);
            text.setTextAlign(Paint.Align.CENTER);

            final Rect rect = object.getBoundingBox();
            Bitmap resultBmp = Bitmap.createBitmap(image, rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top);
            FirebaseVisionImageLabeler foodLabeler = FirebaseVision.getInstance().getCloudImageLabeler();
            FirebaseVisionImage fireImage = FirebaseVisionImage.fromBitmap(resultBmp);
            foodLabeler.processImage(fireImage).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                @Override
                public void onSuccess(List<FirebaseVisionImageLabel> firebaseVisionImageLabels) {
                    for (FirebaseVisionImageLabel label : firebaseVisionImageLabels) {
                        Canvas canvas = new Canvas(image);
                        Log.d("Fruit ", label.getText() + " " + label.getConfidence());
                        if (label.getText().contains("Banana")) {
                            drawLabel(canvas, rect, "Banana", banana, text);
                            break;
                        } else if (label.getText().contains("Lemon")) {
                            drawLabel(canvas, rect, "Lemon", lemon, text);
                            break;
                        } else if (label.getText().equals("Orange")) {
                            drawLabel(canvas, rect, "Orange", orange, text);
                            break;
                        } else if (label.getText().equals("Granny smith")) {
                            drawLabel(canvas, rect, "Green Apple", greenApple, text);
                            break;
                        } else if (label.getText().equals("Apple")) {
                            drawLabel(canvas, rect, "Red Apple", redApple, text);
                            break;
                        }
                    }
                    Log.d("Exit", " ");
                    imageView.setImageBitmap(image);
                }
            });
        }
    }
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    private void drawLabel(Canvas canvas, Rect rect, String fruit, Paint fruitPaint, Paint textPaint) {
        canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, fruitPaint);
        canvas.drawText(fruit, (float) ((rect.right + rect.left) / 2), (float) (rect.top + rect.bottom) / 2, textPaint);
    }
}
