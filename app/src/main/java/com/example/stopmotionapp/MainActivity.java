package com.example.stopmotionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;


public class MainActivity extends AppCompatActivity {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    ImageCapture imageCapture;
    ImageAnalysis imageAnalysis;
    ImageAnalyzer imageAnalyzer;
    Preview preview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {});
        setUpCamera(requestPermissionLauncher);
    }


    ////source: https://developer.android.com/training/camerax/preview#java
    private void setUpCamera(ActivityResultLauncher<String> requestPermissionLauncher) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                    startPreviewCaptureAndAnalysis(cameraProvider, cameraSelector);
                    setUpAutomaticModeToggle(cameraProvider, cameraSelector);
                } catch (ExecutionException | InterruptedException e) {
                    //this should never be reached
                }
            }, ContextCompat.getMainExecutor(this));
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            setUpCamera(requestPermissionLauncher);
        }
    }


    //sources: https://developer.android.com/training/camerax/preview#java, https://developer.android.com/training/camerax/take-photo and https://developer.android.com/training/camerax/analyze#java
    private void startPreviewCaptureAndAnalysis(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector){
        preview = new Preview.Builder().build();
        PreviewView previewView = findViewById(R.id.previewView);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        imageAnalyzer = new ImageAnalyzer();
        imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(new Size(4032, 3024)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer);
    }


    private void bindAnalyzer(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }


    //source: https://developer.android.com/guide/topics/ui/controls/togglebutton
    private void setUpAutomaticModeToggle(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector){
        ToggleButton automaticModeToggle = (ToggleButton) findViewById(R.id.captureModeToggle);
        automaticModeToggle.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            Log.d("ModeChanged", "Automatic capture mode set to " + isChecked);
            if (isChecked){
                bindAnalyzer(cameraProvider, cameraSelector);
            }else{
                cameraProvider.unbind(imageAnalysis);
            }
        }));
    }


    //sources: https://developer.android.com/training/camerax/take-photo and https://developer.android.com/codelabs/camerax-getting-started#4
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhoto(View view){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), dateFormat.format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            public void onError(@NonNull ImageCaptureException e){
                Log.e("TakePhotoFail", "The photo did not save due to " + e);
            }

            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults){
                Uri savedUri = Uri.fromFile(photoFile);
                String msg = "Photo capture succeeded:" + savedUri;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d("ImageSaved", msg);
            }
        });
    }
}