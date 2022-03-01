package com.example.stopmotionapp;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;


import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;


/*https://developers.google.com/ml-kit/vision/object-detection/android#java and https://developer.android.com/training/camerax/analyze#java
Used as sources*/
public class ImageAnalyzer implements  ImageAnalysis.Analyzer{
    ObjectDetector detector;


    public ImageAnalyzer(){
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).enableMultipleObjects().build();
        detector = ObjectDetection.getClient(options);
    }


    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") Image image = imageProxy.getImage();
        if (image != null){
            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());

            detector.process(inputImage).addOnSuccessListener(
                    (detectedObjects) -> Log.d("DetectedObjects", String.valueOf(detectedObjects.size()))
                ).addOnFailureListener(
                        e -> Log.e("Detection failed", e.toString())
                ).addOnCompleteListener(
                        task -> {
                            image.close();
                            imageProxy.close();
                        }
                );
        }
    }
}

