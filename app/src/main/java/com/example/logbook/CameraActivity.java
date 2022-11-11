package com.example.logbook;

import static android.Manifest.*;
import static androidx.constraintlayout.motion.widget.Debug.getLocation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraActivity extends AppCompatActivity {

    Button buttonTakePhoto;
    Button btnNext;
    Button btnPrev;
    ImageView imageCamera;
    TextView txtLocation;
    private ImageCapture imageCapture;

    private PreviewView previewView;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"}; //, "android.permission.ACCESS_FINE_LOCATION"};

    private static final String FILE_NAME = "Camera.txt"; //File name for the text file
    private int currentIndex = -1; //Index of the current image


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        findAllElements();

        if (allPermissionsGranted()) {
            startCamera();
        }
        else {
            int REQUEST_CODE_PERMISSIONS = 101;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        buttonTakePhoto.setOnClickListener(v -> takePhoto());

        btnNext.setOnClickListener(v -> {
            setAnimationLeftToRight();
            setImageFromStorage();
        });
        btnPrev.setOnClickListener(v -> {
            setAnimationRightToLeft();
            setImageFromStorage();
        });
    }


    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // check location permission here
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                // Image capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle((this), cameraSelector, preview, imageCapture);
            } catch (InterruptedException | ExecutionException e) {
                // handle InterruptedException.
                Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private List<String> getImageAbsolutePaths() {
        final List<String> paths = new ArrayList();
        final Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = { MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME };
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC";
        final Cursor cursor = this.getContentResolver().query(uri, projection, null,
                null, orderBy);

        final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            final String absolutePathOfImage = cursor.getString(column_index_data);
            paths.add(absolutePathOfImage);
        }
        cursor.close();
        return paths;
    }
    private void takePhoto() {
        long timestamp = System.currentTimeMillis();
        // get current location
        getLocation();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Image_" + timestamp);
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "Image_" + timestamp);
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Image captured by camera");
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, timestamp);
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, timestamp);
        contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, timestamp);
        // put current location in the image
//        contentValues.put(MediaStore.Images.Media.LATITUDE, locationClient.getLastLocation().getResult().getLatitude());
//        contentValues.put(MediaStore.Images.Media.LONGITUDE, locationClient.getLastLocation().getResult().getLongitude());



        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();

        imageCapture.takePicture(outputFileOptions, getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {

                        List<String> imageAbsolutePaths = getImageAbsolutePaths();
                        // display recent captured photo
                        Glide.with(CameraActivity.this).load(imageAbsolutePaths.get(0))
                                .centerCrop()
                                .into(imageCamera);
                        txtLocation.setText(outputFileResults.getSavedUri().getPath());
                        Toast.makeText(CameraActivity.this, "Photo has been saved successfully. "+imageAbsolutePaths.size()+"@"+ outputFileResults.getSavedUri().getPath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Toast.makeText(CameraActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    private void setImageFromStorage(){
        List<String> imageAbsolutePaths = getImageAbsolutePaths();
        final int count = imageAbsolutePaths.size();

        if (count ==0 ) {
            Toast.makeText(CameraActivity.this, "No photo found", Toast.LENGTH_SHORT).show();
        }
        else {
            currentIndex++;
            if (currentIndex == count) {
                currentIndex = 0;
            }
            final String imagePath = imageAbsolutePaths.get(currentIndex);
            txtLocation.setText(imagePath);
            Glide.with(this).load(imagePath)
                    .centerCrop()
                    .into(imageCamera);
        }
    }
    private void setAnimationLeftToRight() {
        // Animation using pre-defined android Slide Left-to-Right
        Animation in_left = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        imageCamera.setAnimation(in_left);
    }
    private void setAnimationRightToLeft() {
        // Animation using pre-defined android Slide Right-to-Left
        Animation in_right = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        imageCamera.setAnimation(in_right);
    }
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }

    private void findAllElements() {
        previewView = findViewById(R.id.previewView);
        buttonTakePhoto= findViewById(R.id.buttonTakePhoto);
        btnNext = findViewById(R.id.buttonNext);
        btnPrev = findViewById(R.id.buttonPrevious);
        imageCamera = findViewById(R.id.imageCamera);
        txtLocation = findViewById(R.id.txtLocation);
    }
}