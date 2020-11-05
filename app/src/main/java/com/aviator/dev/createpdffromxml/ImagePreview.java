package com.aviator.dev.createpdffromxml;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class ImagePreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);

        initViews();

        if(getIntent().hasExtra("IMG")){
            String path=getIntent().getStringExtra("IMG");
            decodeImg(path);
        }

    }

    private ImageView img;

    public void initViews() {
        img = findViewById(R.id.img);
    }


    private void decodeImg(String path) {
        try {
            BufferedInputStream inputStream=new BufferedInputStream(
                    new FileInputStream(new File(path))
            );
//            InputStream inputStream=new FileInputStream(new File(path));
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), false);

            img.setImageBitmap(resized);

        } catch (FileNotFoundException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
