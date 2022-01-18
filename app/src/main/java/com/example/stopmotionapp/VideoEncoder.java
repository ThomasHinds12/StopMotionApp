/*Used sources https://www.sisik.eu/blog/android/media/images-to-video,
 https://developpaper.com/android-uses-mediacodec-to-encode-the-video-captured-by-the-camera-as-h264/
 and https://developer.android.com/reference/android/media/MediaCodec as references for this page */

package com.example.stopmotionapp;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;

public class VideoEncoder {

    private MediaCodec encoder;
    int width;
    int height;
    int framerate;

    //constructor creates and configures the MediaCodec encoder
    VideoEncoder(int width, int height, int framerate) {
        this.width = width;
        this.height = height;
        this.framerate = framerate;

    }

    public void setUp(){
        try{
            encoder = MediaCodec.createEncoderByType("video/avc");
            Log.d("EncoderCreated", "Video Encoder created successfully");
        }catch (IOException e){
            Log.e("MediaCodecCreateEncoderFail", e.getMessage());
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width*height*5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);

        try{
            encoder.configure(mediaFormat,null,null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            Log.d("EncoderConfigured", "MediaCodec successfully configured");
        }
        catch (MediaCodec.CodecException e){
            Log.e("MediaCodecConfigFail", e.getDiagnosticInfo());
        }
    }
}

