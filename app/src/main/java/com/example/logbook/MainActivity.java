package com.example.logbook;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.*;

import com.bumptech.glide.Glide;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

//References
// https://developer.android.com/codelabs/basic-android-kotlin-training-internet-images
// https://github.com/bumptech/glide

public class MainActivity extends AppCompatActivity {

    // UI elements
    Button btnAdd, btnPrev, btnNext, btnReset, btnCamera;
    EditText inputURL;
    ImageView imageView;
    TextView txtImageName;

    String regex = "https?://(?:[a-z0-9\\-]+\\.)+[a-z]{2,6}(?:/[^/#?]+)+\\.(?:png|jpg|gif|jpeg)"; //regex for checking if the input is a valid URL
    ArrayList<String> imageURLs = new ArrayList<>(); // list of saved URLs

    int currentIndex = 0; // index of current image

    private static final String FILE_NAME = "URLImage.txt"; // file name to save the list of URLs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findAllElements(); // retrieve all ui elements on the form
        loadImage(); // load image from file
//        setImage(); // load URL into ImageView by using Glide
        whenClickNext(); // when click next button
        whenClickPrevious(); // when click previous button
        whenClickAdd(); // when click add button
        whenClickReset(); // when click reset button
        whenClickCamera(); // when click camera button
    }

    private void loadImage() {
        try {
            loadURLs(); // load URLs from file
        } catch (IOException e) {
            e.printStackTrace(); // print error messages
            Toast.makeText(this, "No image to display!", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("SetTextI18n")
    private void whenClickReset() {
        // when click reset button
        btnReset.setOnClickListener(v -> {
            if (imageURLs.size() > 0) { // if there is at least one image
                AlertDialog.Builder builder = new AlertDialog.Builder(this); // create a new alert dialog
                builder.setTitle("Confirm Delete"); // set title
                builder.setMessage("Are you sure you want to clear all images?"); // set message
                builder.setPositiveButton("Yes", (dialogInterface, i) -> {
                    // check if image is empty
                    if (imageURLs.size() == 0) {
                        Toast.makeText(this, "No image to reset!", Toast.LENGTH_SHORT).show();
                    } else {
                        imageURLs.clear(); // clear the list of URLs
                        imageView.setImageResource(R.drawable.image_preview); // clear the image
                        removeFile(); // remove the file
                        currentIndex = 0; // reset the index
                        Toast.makeText(this, "All images have been deleted", Toast.LENGTH_SHORT).show();
                        txtImageName.setText("No Image"); // set text to No Image
                    }
                });
                builder.setNegativeButton("No", (dialogInterface, i) -> {
                    //Do nothing
                });
                builder.create().show(); // show the alert dialog
            }
        });
    }

    private void whenClickCamera() {
        btnCamera.setOnClickListener(v -> { // when click camera button
            startActivity(new Intent(MainActivity.this, CameraActivity.class)); // start camera activity
        });
    }

    private void removeFile() {
        getApplicationContext().deleteFile(FILE_NAME); // remove the file
    }

    private void whenClickAdd() {
        btnAdd.setOnClickListener(v -> {
            String URL = inputURL.getText().toString().trim(); // get the URL from the input box and trim the spaces
            Pattern p = Pattern.compile(regex); // create a pattern object using the regex
            Matcher m = p.matcher(URL); // create a matcher object to match the URL with the pattern

            if (URL.isEmpty()) {
                inputURL.setError("Please enter a URL");
                inputURL.requestFocus();
            } else {
                if (m.matches()) { // check if the URL matches the regex
                    imageURLs.add(URL); // add the URL to the list of URLs
                    ProgressDialog progressDialog = new ProgressDialog(this); // create a progress dialog
                    progressDialog.setMessage("Downloading..."); // set message
                    progressDialog.show(); // show the progress dialog
                    new android.os.Handler().postDelayed(() -> {
                        progressDialog.dismiss(); // dismiss the progress dialog
                        // check if the URL is empty
                        try {
                            saveToFile(URL); // save the URL to the file
                            Toast.makeText(this, "URL added successfully!", Toast.LENGTH_SHORT).show(); // display message
                            Glide.with(this).load(URL).into(imageView); // load the image into the image view
                            inputURL.setText(""); // clear the input box
                            currentIndex = imageURLs.indexOf(URL); // set the index to the last added URL
                            txtImageName.setText(String.format("Image from URL: %s", URL)); // display the index
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Error saving URL", Toast.LENGTH_SHORT).show(); // display message
                        }
                    }, 1000); // delay the progress dialog for 3 seconds

                } else {
                    inputURL.setError("Please enter a valid URL"); // display error message
                    inputURL.requestFocus();
                    inputURL.setText(""); // clear the input box
                }
            }
        });
    }

    private void saveToFile(String url) throws IOException {
        FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND); // open the file in append mode
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream); // create an output stream writer object
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter); // create a buffered writer object to write to the file
        bufferedWriter.write(url); // write the URL to the file
        bufferedWriter.newLine(); // write a new line
        bufferedWriter.flush(); // flush the buffer
        bufferedWriter.close(); // close the buffered writer
        outputStreamWriter.close(); // close the output stream writer
    }

    @SuppressLint("SetTextI18n")
    private void whenClickPrevious() {
        btnPrev.setOnClickListener(v -> {
            if (imageURLs.size() > 1) {
                currentIndex--;
                setImage(); // load the image
            } else {
                Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show(); // display message
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void whenClickNext() {
        btnNext.setOnClickListener(v -> {
            // size must be greater than 0 and the index must be less than the size of the list
            if (imageURLs.size() > 1) {
                currentIndex++;
                setImage(); // load the image
            } else {
                Toast.makeText(this, "No image to display", Toast.LENGTH_SHORT).show(); // display message
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void loadURLs() throws IOException {
        FileInputStream fileInputStream = getApplicationContext().openFileInput(FILE_NAME); // open the file
        if (fileInputStream != null) { // check if the file is not null
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream); // create an input stream reader object
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);  // create a buffered reader object to read the file
            String lineData = bufferedReader.readLine(); // read the first line
            while (lineData != null) { // check if the line is not null
                imageURLs.add(lineData); // add the URL to the list
                lineData = bufferedReader.readLine(); // read the next line
                currentIndex = imageURLs.size() - 1; // set the index to the last added URL
                Glide.with(this).load(imageURLs.get(currentIndex)).into(imageView); // load the image into the image view
                txtImageName.setText("Image from URL: " + imageURLs.get(currentIndex)); // display the index
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void setImage() {
        int size = imageURLs.size();
        if (currentIndex >= size) {
            currentIndex = 0;
        } else if (currentIndex < 0) {
            currentIndex = size - 1;
        }
        if (size > 0) {
            Glide.with(this).load(imageURLs.get(currentIndex)).into(imageView); // load the image into the image view
            txtImageName.setText("Image from URL: " + imageURLs.get(currentIndex)); // display the index
        }
    }

    private void findAllElements() {
        // findViewById() for all the views
        btnAdd = findViewById(R.id.buttonAdd);
        btnPrev = findViewById(R.id.buttonPrevious);
        btnNext = findViewById(R.id.buttonNext);
        btnReset = findViewById(R.id.buttonReset);
        btnCamera = findViewById(R.id.buttonCamera);
        inputURL = findViewById(R.id.editTextURL);
        imageView = findViewById(R.id.imageDisplay);
        txtImageName = findViewById(R.id.txtImageName);
    }
}
