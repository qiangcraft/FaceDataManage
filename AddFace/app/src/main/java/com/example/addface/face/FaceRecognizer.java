package com.example.addface.face;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.util.Log;
import android.util.Pair;

import com.example.addface.env.Device;
import com.example.addface.env.TFliteTemplate;

import java.util.Map;


public class FaceRecognizer extends TFliteTemplate {
    private static final int IMAGE_SIZE = 112;
    private static final int NUM_Bytes_Per_Channel = 4;
    public static final int EMBEDDING_SIZE = 256;
    private static final float FACE_DISTANCE_THRESHOLD = 1.1f;  // 人脸距离阈值

    /**
     * An array to hold inference results, to be feed into Tensorflow Lite as outputs. This isn't part
     * of the super class, because we need a primitive array here.
     */
    private float[][] faceEmbeddingsArray = new float[1][EMBEDDING_SIZE];


    /** Initializes a {@code Classifier}. */
    public FaceRecognizer(Activity activity, Device device, int numThreads){
        super(activity, device, numThreads);
    }

    public float[] getFaceEmbeddings(Bitmap bitmap){
        convertBitmapToByteBuffer(bitmap);
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Log.d("TimeCost","run face recognize inference: " + (endTime - startTime));

        return normalizer(faceEmbeddingsArray[0]);
    }

    /** Runs inference and returns the classification results. */
    public Pair<String,Float> recognizeImage(Bitmap bitmap, Map<String, float[]> NameFacesDBMap) {

        return recognizePerson(getFaceEmbeddings(bitmap),NameFacesDBMap);
    }

    private Pair<String,Float> recognizePerson(float[] embeddings, Map<String, float[]> NameFacesDBMap){
        return FaceProcess.search(embeddings, NameFacesDBMap, FACE_DISTANCE_THRESHOLD);
    }

    private float[] normalizer(float[] embeddings){
        float[] normalizeEmbeddings = new float[EMBEDDING_SIZE];
        float sum = 0.f;
        for(int i=0;i<embeddings.length;i++){
            sum += Math.pow(embeddings[i],2);
        }
        sum = (float)Math.sqrt(sum);
        for(int i=0;i<embeddings.length;i++){
            normalizeEmbeddings[i] = embeddings[i]/sum;
        }
        return normalizeEmbeddings;
    }

    @Override
//    protected String getModelPath() {
//        return "insightface_r50.tflite";
//    }
    protected String getModelPath() {
        return "mobilefacenet256.tflite";
    }

    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat(((pixelValue >> 16) & 0xFF));
        imgData.putFloat(((pixelValue >> 8) & 0xFF));
        imgData.putFloat((pixelValue & 0xFF));
    }

    @Override
    protected void runInference() {
        tflite.run(imgData, faceEmbeddingsArray);
    }

    @Override
    protected int getNumBytesPerChannel() {
        return NUM_Bytes_Per_Channel;
    }

    @Override
    public int getImageSizeX() {
        return IMAGE_SIZE;
    }

    @Override
    public int getImageSizeY() {
        return IMAGE_SIZE;
    }


}
