package com.example.stopmotionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    ImageCapture imageCapture;
    Preview preview;
    int imageCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageCount = 0;

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
        setUpCamera(requestPermissionLauncher);
    }


    //checks the permissions for camera and then sets up the camera if the permission is granted
    private void setUpCamera(ActivityResultLauncher<String> requestPermissionLauncher) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            //following code very closely based on https://developer.android.com/training/camerax/preview#java
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindPreviewAndImageCapture(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    //this should never be reached
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            setUpCamera(requestPermissionLauncher);
        }
    }


    //following code based on code from https://developer.android.com/training/camerax/preview#java
    //binds the preview use case to the activity lifecycle
    private void bindPreviewAndImageCapture(@NonNull ProcessCameraProvider cameraProvider){
        preview = new Preview.Builder().build();

        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        PreviewView previewView = (PreviewView) findViewById(R.id.previewView);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }


    //following code based on code from https://developer.android.com/training/camerax/take-photo and https://developer.android.com/codelabs/camerax-getting-started#4
    //method to take photo when the 'Take Photo' button is clicked
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhoto(View view){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), dateFormat.format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            public void onError(ImageCaptureException e){
                Log.e("TakePhotoFail", "The photo did not save due to " + e.toString());
            }

            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults){
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo capture succeeded:" + savedUri;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d("ImageSaved", msg);
            }
        });
    }

    /*public void createVideo(View view){
        //encodeVideo();
        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }

    private void encodeVideo(){
        VideoEncoder videoEncoder = new VideoEncoder(3840, 2160, 5);
        videoEncoder.setUp();
        videoEncoder.releaseEncoder();
    };*///
}