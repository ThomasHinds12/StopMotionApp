package com.example.stopmotionapp;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class VideoActivity extends AppCompatActivity {
    private VideoEncoder videoEncoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Intent intent = getIntent();
        videoEncoder = new VideoEncoder(3840, 2160, 5);
    }
}