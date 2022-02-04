package com.example.stopmotionapp;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;


/*https://developers.google.com/ml-kit/vision/object-detection/android#java and https://developer.android.com/training/camerax/analyze#java
Used as sources*/
public class ImageAnalyzer implements  ImageAnalysis.Analyzer{
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") Image image = imageProxy.getImage();
        if (image != null){
            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
            //TODO pass image to object detector
            imageProxy.close();
        }
    }
}
