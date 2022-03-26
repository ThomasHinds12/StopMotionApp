package com.example.stopmotionapp;

import android.annotation.SuppressLint;
import android.media.Image;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;

import java.util.LinkedList;
import java.util.List;

    /*
    https://developers.google.com/ml-kit/vision/object-detection/android#java and
    https://developer.android.com/training/camerax/analyze#java
    Used as sources */

public class ImageAnalyzer implements ImageAnalysis.Analyzer {
    public static final int LIST_SIZE = 15; // Used to define length of numOfObjectsHistory, higher value means more accuracy but slower response time

    private final MainActivity mainActivity; // Allows the takePhoto() method in MainActivity to be used

    // Variables used for object detection and hand tracking
    ObjectDetector detector;
    LinkedList<Integer> numOfObjectsHistory;
    private boolean handsInFrame;


    public ImageAnalyzer(MainActivity mainActivity) {
        ObjectDetectorOptions options = new ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).enableMultipleObjects().build();
        detector = ObjectDetection.getClient(options);
        numOfObjectsHistory = new LinkedList<>();
        handsInFrame = false;
        this.mainActivity = mainActivity;
    }


    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    public void analyze(@NonNull ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError") Image image = imageProxy.getImage();
        if (image != null) {
            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());

            detector.process(inputImage).addOnSuccessListener(
                    this::lookForHands
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


    // Uses the list of objects returned by detector to track user's hands
    // Calls MainActivity.takePhoto() to capture image when hands removed from frame
    @RequiresApi(api = Build.VERSION_CODES.R)
    private void lookForHands(List<DetectedObject> detectedObjects) {
        int numOfObjects = detectedObjects.size();

        numOfObjectsHistory.addFirst(numOfObjects);
        if (numOfObjectsHistory.size() > LIST_SIZE) {
            numOfObjectsHistory.removeLast();
        }

        if (numOfObjectsHistory.size() == LIST_SIZE) {
            if (!handsInFrame) {
                //check if values in obj list are changing, if they are then the hands are in frame so set to true
                for (Integer i : numOfObjectsHistory) {
                    if ((i <= numOfObjects - 2 || i >= numOfObjects + 2) && i != 0) {
                        handsInFrame = true;
                        break;
                    }
                }
            } else {
                //check if values stabilize, if they do then set to false and capture image
                boolean unchanged = true;
                for (Integer i : numOfObjectsHistory) {
                    if (!i.equals(numOfObjects)) {
                        unchanged = false;
                        break;
                    }
                }
                if (unchanged) {
                    handsInFrame = false;
                    mainActivity.takePhoto();
                }
            }
        }
    }
}

