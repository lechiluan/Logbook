package com.example.logbook;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
// https://iotes.lk/2020/12/13/camerax-android-in-java/#google_vignette

public class MainActivity extends AppCompatActivity {

    Button btnAdd, btnPrev, btnNext, btnClear, btnCamera;
    EditText inputURL;
    ImageView imageView;
    TextView label;

    String regex = "(https?:\\/\\/.*\\.(?:png|jpg|gif|jpeg))"; //regex for image url

    ArrayList<String> imageURLs = new ArrayList<>(); // list of saved URLs

    int currentIndex = 0; // index of current image
    // -1 means no image is currently displayed
    private static final String FILE_NAME = "URLofImage.txt"; // file name to save the list of URLs
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // retrieve all ui elements on the form
        findAllElements();
        try {
            loadURLs();
        } catch (IOException e) {
            e.printStackTrace();
            displayMessage("Read file error, file = " + FILE_NAME);
        }
        // load URL into ImageView by using Glide
        setImage();
        whenClickNext();
        whenClickPrevious();
        whenClickAdd();
        whenClickReset();
        whenClickCamera();
    }

    private void whenClickCamera() {
        btnCamera.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CameraActivity.class));
        });
    }

    private void whenClickReset() {
        btnClear.setOnClickListener(v -> {
            imageURLs.clear();
            imageView.setImageResource(0);
            removeFile();
            currentIndex = 0;
            displayMessage("Data reset completed");
        });
    }

    private void removeFile() {
        getApplicationContext().deleteFile(FILE_NAME);
    }

    private void whenClickAdd() {
        btnAdd.setOnClickListener(v -> {
            String URL = inputURL.getText().toString().trim();
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(URL);
            // check if the URL is empty
            if (URL.isEmpty()) {
                inputURL.setError("Please enter a URL");
                inputURL.requestFocus();
            }
            else {
                if(m.matches()){
                    imageURLs.add(URL);
                    try {
                        saveToFile(URL);
                        displayMessage("URL added successfully");
                        Glide.with(this)
                                .load(URL)
                                .into(imageView);
                        inputURL.setText("");
                        currentIndex = imageURLs.indexOf(URL);
                    } catch (IOException e) {
                        e.printStackTrace();
                        displayMessage("Save file error");
                    }
                }
                else{
                    inputURL.setError("Please enter a valid URL");
                    inputURL.requestFocus();
                }
            }
        });
    }

    private void displayMessage(String message) {
        label.setText(message);
    }


    private void saveToFile(String url) throws IOException {
        FileOutputStream fileOutputStream = getApplicationContext().openFileOutput(FILE_NAME, Context.MODE_APPEND);
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream);
        BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
        bufferedWriter.write(url);
        bufferedWriter.newLine();
        bufferedWriter.flush();
        bufferedWriter.close();
        outputStreamWriter.close();
    }

    private void whenClickPrevious() {
        btnPrev.setOnClickListener(v -> {
            currentIndex--;
            setImage();
        });
    }

    private void whenClickNext() {
        btnNext.setOnClickListener(v -> {
            currentIndex++;
            setImage();
            displayMessage("Current index = " + currentIndex);
        });
    }

    private void loadURLs() throws IOException {
        FileInputStream fileInputStream = getApplicationContext().openFileInput(FILE_NAME);
        if (fileInputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String lineData =
                    bufferedReader.readLine();
            while (lineData != null) {
                imageURLs.add(lineData);
                lineData = bufferedReader.readLine();
            }
        }
    }

    private void setImage() {
        int size = imageURLs.size();
        if (currentIndex >= size) {
            currentIndex = 0;
        } else if (currentIndex < 0) {
            currentIndex = size - 1;
        }
        if (size > 0) {
            Glide.with(this)
                    .load(imageURLs.get(currentIndex))
                    .into(imageView);
        }
    }
    private void findAllElements() {
        // findViewById() for all the views
        btnAdd = findViewById(R.id.buttonAdd);
        btnPrev = findViewById(R.id.buttonPrevious);
        btnNext = findViewById(R.id.buttonNext);
        btnClear = findViewById(R.id.buttonClear);
        btnCamera = findViewById(R.id.buttonCamera);
        inputURL = findViewById(R.id.editTextURL);
        imageView = findViewById(R.id.imageDisplay);
        label = findViewById(R.id.textNameImage);
    }
}
