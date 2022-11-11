package com.example.logbook;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.util.regex.*;

import com.bumptech.glide.Glide;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    Button btnAdd, btnPrev, btnNext, btnClear, btnCamera;
    EditText inputURL;
    ImageView imageView;
    TextView label;

    ArrayList<String> savedList; // list of saved URLs

    int index = -1;
    // -1 means no image is currently displayed

    String fileName = "file.txt"; // file name to save the list of URLs

    // Regex pattern for URL validation
    String regex = "(https?:\\/\\/.*\\.(?:png|jpg))";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize the views
        btnAdd = findViewById(R.id.buttonAdd);
        btnPrev = findViewById(R.id.buttonPrevious);
        btnNext = findViewById(R.id.buttonNext);
        btnClear = findViewById(R.id.buttonClear);
        btnCamera = findViewById(R.id.buttonCamera);
        inputURL = findViewById(R.id.editTextURL);
        imageView = findViewById(R.id.imageDisplay);
        label = findViewById(R.id.textNameImage);

        // Sava list old data
        savedList = readFile(fileName);

        if (savedList.size() != 0) {
            Glide.with(this).load(savedList.get(0)).into(imageView);
        }

        // Switch to Camera Activity
        btnCamera.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, CameraActivity.class)));

        // Clear the list
        btnClear.setOnClickListener(v -> {
            savedList.clear();
            index = -1;
            label.setText("");
            imageView.setImageResource(0);
            saveFile(fileName, savedList);
        });

        btnAdd.setOnClickListener(v -> {
            if (!inputURL.getText().toString().trim().isEmpty()) {
                String imageURL = inputURL.getText().toString().trim();

                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(imageURL);

                if (matcher.matches()) {
                    index = savedList.indexOf(imageURL);
                    Glide.with(this).load(imageURL).into(imageView);

                    savedList.add(imageURL);
                    inputURL.setText("");
                    saveFile(fileName, savedList);
                    savedList = readFile(fileName);
                    Toast.makeText(getBaseContext(), "Add Image Successfully", Toast.LENGTH_LONG).show();

                } else {
                    inputURL.setError("Invalid Image URL");
                    inputURL.requestFocus();
                }
            } else {
                inputURL.setError("Please enter image URL");
                inputURL.requestFocus();
            }
        });

        btnNext.setOnClickListener(v -> {
            int count = savedList.size();
            if (count > 0 && count != 1) {
                index++;
                if (index == count) {
                    index = 0;
                }
                Glide.with(this).load(savedList.get(index)).into(imageView);
                Animation right = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
                imageView.startAnimation(right);
            } else {
                Toast.makeText(getBaseContext(), "No image", Toast.LENGTH_LONG).show();
            }
        });

        btnPrev.setOnClickListener(v -> {
            int count = savedList.size();
            if (count > 0 && count != 1) {
                index--;
                if (index < 0) {
                    index = count - 1;
                }
                Glide.with(this).load(savedList.get(index)).into(imageView);
                Animation left = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
                imageView.startAnimation(left);
            } else {
                Toast.makeText(getBaseContext(), "No image", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void saveFile(String file, ArrayList<String> text) {
        try {
            FileOutputStream fos = openFileOutput(file, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);

            oos.writeObject(text);
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file", Toast.LENGTH_SHORT).show();
        }
    }

    public ArrayList<String> readFile(String file) {
        ArrayList<String> text = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput(file); // open file
            ObjectInputStream ois = new ObjectInputStream(fis); // read object
            text = (ArrayList<String>) ois.readObject(); // cast to ArrayList<String>
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
        }
        return text;
    }
}
