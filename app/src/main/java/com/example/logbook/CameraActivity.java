package com.example.logbook;

import static android.Manifest.*;

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


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
//References
// https://iotes.lk/2020/12/13/camerax-android-in-java/#google_vignette

public class CameraActivity extends AppCompatActivity {

    Button buttonTakePhoto;
    Button btnNext;
    Button btnPrev;
    ImageView imageCamera;
    TextView txtLocation, txtName;
    private ImageCapture imageCapture;
    public double latitude;
    public double longitude;
    private FusedLocationProviderClient locationClient; // Location client
    private PreviewView previewView; //Camera Preview
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"}; //, "android.permission.ACCESS_FINE_LOCATION"};

    private int currentIndex = 0; //Index of the current image

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_delete){
            deleteAllImage();
        }
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void deleteAllImage() {
        // confirm delete all images
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete All Camera Images");
        builder.setMessage("Are you sure you want to delete all camera images?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            // delete all images
            getContentResolver().delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null);
            Toast.makeText(this, "All images deleted", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("No", (dialog, which) -> {
            // cancel
            dialog.dismiss();
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        
        locationClient = LocationServices.getFusedLocationProviderClient(this); // Get the location client

        findAllElements(); //Find all the elements on the form

        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { //Check if the location permission is granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { //Check if the version is greater than or equal to Marshmallow
                int REQUEST_PERMISSION_FINE_LOCATION = 1; //Request code for the permission
                requestPermissions(new String[]{permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_FINE_LOCATION); //Request the permission
            }
        }
        else {
            showLocation(); //  Show the location
        }
        if (allPermissionsGranted()) {
            startCamera(); //Start the camera
        }
        else {
            int REQUEST_CODE_PERMISSIONS = 101; // Request code for the permissions
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS); //Request the permissions
        }

        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        btnNext.setOnClickListener(v -> {
            setAnimationLeftToRight(); // Set the animation for the next button
            setImageFromStorage(); // Set the image from the storage
        });
        btnPrev.setOnClickListener(v -> {
            setAnimationRightToLeft(); // Set the animation for the previous button
            setImageFromStorage(); // Set the image from the storage
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) { //Check if the permission is granted
        // check location permission here
        super.onRequestPermissionsResult(requestCode, permissions, grantResults); //Call the super method
        if(allPermissionsGranted()) { //Check if all the permissions are granted
            startCamera(); //Start the camera
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show(); //Show a toast message
            finish();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) { //Loop through the required permissions
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) { //Check if the permission is granted
                return false;
            }
        }
        return true;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this); // Get the camera provider

        cameraProviderFuture.addListener(() -> { // Add a listener to the camera provider
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
        }, ContextCompat.getMainExecutor(this)); // Get the main executor
    }

    private List<String> getImageAbsolutePaths() {
        final List<String> paths = new ArrayList<>(); // Create a list of image paths
        final Uri uri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI; // Get the URI of the images
        final String[] projection = { MediaStore.MediaColumns.DATA, // Get the data of the images
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME }; // Get the bucket display name of the images
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN + " DESC"; // Order the images by the date taken
        final Cursor cursor = this.getContentResolver().query(uri, projection, null, // Get the cursor
                null, orderBy); // Order the images by the date taken

        final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA); // Get the column index of the data
        while (cursor.moveToNext()) { // Loop through the cursor
            final String absolutePathOfImage = cursor.getString(column_index_data); // Get the absolute path of the image
            paths.add(absolutePathOfImage); // Add the absolute path of the image to the list
        }
        cursor.close(); // Close the cursor
        return paths; // Return the list of image paths
    }

    private void takePhoto() {
        long timestamp = System.currentTimeMillis(); // Get the timestamp
        ContentValues contentValues = new ContentValues(); // Create a new content values
        contentValues.put(MediaStore.Images.Media.TITLE, "Image_" + timestamp); // Add the title to the content values
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, "Image_" + timestamp); // Add the display name to the content values
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg"); // Add the mime type of image to the content values
        contentValues.put(MediaStore.Images.Media.DATE_TAKEN, timestamp); // Add the date taken image to the content values
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, timestamp); // Add the date added image to the content values
        contentValues.put(MediaStore.Images.Media.DATE_MODIFIED, timestamp); // Add the date modified image to the content values

        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(getContentResolver(), // Get the content resolver
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // Get the URI of the images
                        contentValues).build(); // Build the output file options
        imageCapture.takePicture(outputFileOptions, getExecutor(), // Take the picture
                new ImageCapture.OnImageSavedCallback() { // Add a callback to the image capture
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) { // On image saved

                        List<String> imageAbsolutePaths = getImageAbsolutePaths(); // Get the list of image paths
                        // display recent captured photo
                        Glide.with(CameraActivity.this).load(imageAbsolutePaths.get(0)).centerCrop().into(imageCamera); // Load the image into the image view
                        txtName.setText(Objects.requireNonNull(outputFileResults.getSavedUri()).getPath()); // Set the text of the text view
                        // Show a toast message
                        Toast.makeText(CameraActivity.this, "Photo has been saved successfully. "+imageAbsolutePaths.size()+"@"+ outputFileResults.getSavedUri().getPath(), Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) { // On error
                        Toast.makeText(CameraActivity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }


    private void setImageFromStorage(){
        List<String> imageAbsolutePaths = getImageAbsolutePaths(); // Get the list of image paths
        final int count = imageAbsolutePaths.size(); // Get the size of the list
        if (count ==0 ) {
            Toast.makeText(CameraActivity.this, "No photo found", Toast.LENGTH_SHORT).show(); // Show a toast message
        }
        else {
            currentIndex++;
            if (currentIndex == count) {
                currentIndex = 0; // Reset the current index
            }
            final String imagePath = imageAbsolutePaths.get(currentIndex); // Get the image path
            txtName.setText(imagePath); // Set the text of the text view
            Glide.with(this).load(imagePath).centerCrop().into(imageCamera); // Set the image view
        }
    }
    private void setAnimationLeftToRight() { // Set the animation from left to right
        // Animation using pre-defined android Slide Left-to-Right
        Animation in_left = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        imageCamera.setAnimation(in_left);
    }
    private void setAnimationRightToLeft() { // Set the animation from right to left
        // Animation using pre-defined android Slide Right-to-Left
        Animation in_right = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        imageCamera.setAnimation(in_right);
    }
    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this); // Get the main executor
    }

    // Show location on the map
    @SuppressLint({"MissingPermission", "SetTextI18n"})
    public void showLocation() {
        // Get the last location
// Set the text of the text view
        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) { // If the location is not null
                latitude = location.getLatitude(); // Get the latitude
                longitude = location.getLongitude(); // Get the longitude
                txtLocation.setText("Location is Lat:" + latitude + " Lon: " + longitude);
            }
        });
        locationClient.getLastLocation().addOnCompleteListener(this, task -> {
            if(task.isSuccessful()) {
                Location location = task.getResult();
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    txtLocation.setText(String.format("Location is Lat:%s Lon: %s", latitude, longitude));
                }
                else {
                    txtLocation.setText("Problem getting the location");
                }
            }
        });
    }

    // Get the list of image paths
    private void findAllElements() {
        previewView = findViewById(R.id.previewView);
        buttonTakePhoto= findViewById(R.id.buttonTakePhoto);
        btnNext = findViewById(R.id.buttonNext);
        btnPrev = findViewById(R.id.buttonPrevious);
        imageCamera = findViewById(R.id.imageCamera);
        txtLocation = findViewById(R.id.txtLocation);
        txtName = findViewById(R.id.txtName);
    }
}