package com.example.stopmotionapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
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
    public static final int MAX_STREAMS = 1;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Preview preview;
    private boolean automaticCaptureModeOn;

    File lastPhoto;

    private SoundPool soundPool;
    private float volume;
    private int cameraSoundId;
    private boolean soundPoolLoaded;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        automaticCaptureModeOn = false;
        lastPhoto = null;

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        });
        setUpCameraSound();
        setUpCamera(requestPermissionLauncher);
    }


    //source: https://o7planning.org/10523/android-soundpool
    private void setUpCameraSound() {
        int streamType = AudioManager.STREAM_MUSIC;
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        float currentVolumeIndex = (float) audioManager.getStreamVolume(streamType);
        float maxVolumeIndex = (float) audioManager.getStreamMaxVolume(streamType);
        volume = currentVolumeIndex / maxVolumeIndex;
        setVolumeControlStream(streamType);

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        SoundPool.Builder builder = new SoundPool.Builder();
        builder.setAudioAttributes(audioAttributes).setMaxStreams(MAX_STREAMS);

        soundPool = builder.build();
        soundPool.setOnLoadCompleteListener((soundPool, i, i1) -> soundPoolLoaded = true);

        cameraSoundId = soundPool.load(this, R.raw.camera_shutter, 1);
    }


    ////source: https://developer.android.com/training/camerax/preview#java
    private void setUpCamera(ActivityResultLauncher<String> requestPermissionLauncher) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            cameraProviderFuture.addListener(() -> {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
                    setUpPreviewCaptureAndAnalysis(cameraProvider, cameraSelector);
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
    private void setUpPreviewCaptureAndAnalysis(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        preview = new Preview.Builder().build();
        PreviewView previewView = findViewById(R.id.previewView);
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder().build();
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        ImageAnalyzer imageAnalyzer = new ImageAnalyzer(this);
        imageAnalysis = new ImageAnalysis.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), imageAnalyzer);
        if (automaticCaptureModeOn) {
            bindAnalyzer(cameraProvider, cameraSelector);
        }
    }


    private void bindAnalyzer(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }


    //source: https://developer.android.com/guide/topics/ui/controls/togglebutton
    private void setUpAutomaticModeToggle(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        ToggleButton automaticModeToggle = (ToggleButton) findViewById(R.id.captureModeToggle);
        automaticModeToggle.setChecked(automaticCaptureModeOn);

        automaticModeToggle.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            Log.d("ModeChanged", "Automatic capture mode set to " + isChecked);
            automaticCaptureModeOn = isChecked;
            if (isChecked) {
                bindAnalyzer(cameraProvider, cameraSelector);
                String msg = "Automatic capture mode turned on";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            } else {
                cameraProvider.unbind(imageAnalysis);
                String msg = "Automatic capture mode turned off";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }));
    }


    //sources: https://developer.android.com/training/camerax/take-photo and https://developer.android.com/codelabs/camerax-getting-started#4
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhoto() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), dateFormat.format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        playCameraSound();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            public void onError(@NonNull ImageCaptureException e) {
                Log.e("TakePhotoFail", "The photo did not save due to " + e);
            }

            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                lastPhoto = photoFile;
                String msg = "Photo capture succeeded:" + savedUri;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d("ImageSaved", msg);
            }
        });
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhotoButton(View view){
        takePhoto();
    }


    //source: https://o7planning.org/10523/android-soundpool
    private void playCameraSound() {
        Log.d("PlaySound", "Sound should have been made. Volume = " + volume + " soundPoolLoaded = " + soundPoolLoaded);
        if (soundPoolLoaded) {
            float leftVolume = volume;
            float rightVolume = volume;
            soundPool.play(cameraSoundId, leftVolume, rightVolume, 1, 0, 1f);
        }
    }


    public void deleteLastPhoto(View view){
        if (lastPhoto != null){

            if (lastPhoto.exists()){
                boolean deleted = lastPhoto.delete();

                if (deleted){
                    Log.d("ImageDeletion", "File " + lastPhoto + " has been deleted");
                    String msg = "Last photo deleted successfully";
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }else{
                    Log.d("ImageDeletion", "Failed to delete image " + lastPhoto);
                    String msg = "Failed to delete last image";
                    Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }
            }else{
                Log.d("ImageDeletion", "File does not exist");
            }
        }else{
            String msg = "No image to delete";
            Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }
}