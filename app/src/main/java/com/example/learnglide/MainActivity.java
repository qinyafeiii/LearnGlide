package com.example.learnglide;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;

import android.util.Log;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyImageCache.getInstance(this).init(Environment.getExternalStorageDirectory() + "/testCache");
        ListView listView = findViewById(R.id.listView);
        listView.setAdapter(new MyAdapter(this));

    }

}
