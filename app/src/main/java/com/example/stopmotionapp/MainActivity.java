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
    public static final int MAX_STREAMS = 1; // Constant used to build soundPool

    // Variables used to access the camera and for the CameraX use cases
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Preview preview;

    private boolean automaticCaptureModeOn; // Used to facilitate the toggle of automatic capture mode

    private File lastPhoto; // Address of the last file taken by the camera

    // Variables used to create the camera shutter sound effect
    private SoundPool soundPool;
    private float volume;
    private int cameraSoundId;
    private boolean soundPoolLoaded;


    // Called when MainActivity is created, effectively used as a constructor and calls methods to set up camera and sound effect
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


    // Loads the camera sound to soundPool so that sound can be played
    // source: https://o7planning.org/10523/android-soundpool
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


    // Handles camera permissions, accesses camera, calls methods to set up CameraX use cases and automatic capture mode toggle
    // source: https://developer.android.com/training/camerax/preview#java
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

    // Sets up and binds the CameraX uses case objects to application lifecycle
    // sources: https://developer.android.com/training/camerax/preview#java, https://developer.android.com/training/camerax/take-photo and https://developer.android.com/training/camerax/analyze#java
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


    // Binds the image analyzer to the application lifecycle
    private void bindAnalyzer(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
    }


    // Sets up toggle button for the automatic capture, binds when checked, unbinds when unchecked
    // source: https://developer.android.com/guide/topics/ui/controls/togglebutton
    private void setUpAutomaticModeToggle(@NonNull ProcessCameraProvider cameraProvider, CameraSelector cameraSelector) {
        ToggleButton automaticModeToggle = (ToggleButton) findViewById(R.id.captureModeToggle);
        automaticModeToggle.setChecked(automaticCaptureModeOn);

        automaticModeToggle.setOnCheckedChangeListener(((buttonView, isChecked) -> {
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


    // Captures and saves an image to external storage, used by takePhotoButton() and the ImageAnalyzer object
    // sources: https://developer.android.com/training/camerax/take-photo and https://developer.android.com/codelabs/camerax-getting-started#4
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhoto() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK);
        File photoFile = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), dateFormat.format(new Date()) + ".jpg");
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        playCameraSound();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            public void onError(@NonNull ImageCaptureException e) {
                String msg = "Failed to save photo";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }

            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = Uri.fromFile(photoFile);
                lastPhoto = photoFile;
                String msg = "Photo capture succeeded:" + savedUri;
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                Log.d("Image", String.valueOf(savedUri));
            }
        });
    }


    // Called when the take photo button is pressed, means takePhoto() can be used without passing a View object
    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePhotoButton(View view) {
        takePhoto();
    }


    // Uses soundPool to play camera shutter sound effect out load, called when an image is captured
    // source: https://o7planning.org/10523/android-soundpool
    private void playCameraSound() {
        if (soundPoolLoaded) {
            float leftVolume = volume;
            float rightVolume = volume;
            soundPool.play(cameraSoundId, leftVolume, rightVolume, 1, 0, 1f);
        }
    }


    // Deletes the file stored in lastPhoto, called when the deleteLastButton is pressed
    public void deleteLastPhoto(View view) {
        if (lastPhoto != null) {
            if (lastPhoto.exists()) {
                boolean deleted = lastPhoto.delete();
                String msg;
                if (deleted) {
                    msg = "Last photo deleted successfully";
                } else {
                    msg = "Failed to delete last image";
                }
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            } else {
                String msg = "No image to delete";
                Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
            }
        }
    }
}