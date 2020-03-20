package com.example.addface.face;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Build;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.example.addface.env.Device;
import com.example.addface.env.TFliteTemplate;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FaceDetection extends TFliteTemplate {
    private static final int IMAGE_SIZE = 300;
    private static final int NUM_Bytes_Per_Channel = 4;
    private static final int MAX_RESULT_NUM = 100;
    private static final int NUM_LANDMARKS = 10;
    private static final int NUM_BBOX_POINT = 4;
    private static final float FACE_PROB_THRESHOLD = 0.98f;

    private Object[] tfliteInputs = null;
    private Map<Integer, Object> tfliteOutputs = null;
    private FloatBuffer outputProbs;
    private FloatBuffer outputLandmarks;
    private FloatBuffer outputBoxes;


    /** Initializes a {@code Classifier}. */
    public FaceDetection(Activity activity, Device device, int numThreads){
        super(activity, device, numThreads);

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(MAX_RESULT_NUM * NUM_Bytes_Per_Channel);
        byteBuffer.order(ByteOrder.nativeOrder());
        outputProbs = byteBuffer.asFloatBuffer();
        outputLandmarks = ByteBuffer.allocateDirect(MAX_RESULT_NUM * NUM_Bytes_Per_Channel * NUM_LANDMARKS)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        outputBoxes = ByteBuffer.allocateDirect(MAX_RESULT_NUM * NUM_Bytes_Per_Channel * NUM_BBOX_POINT)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
    }

    /** Runs inference and returns the classification results. */
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public List<FaceAttr> recognizeImage(final Bitmap bitmap) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        convertBitmapToByteBuffer(bitmap);
        Trace.endSection();

        tfliteInputs = new Object[]{imgData};
        tfliteOutputs = new HashMap();
        tfliteOutputs.put(0, outputProbs);
        tfliteOutputs.put(1, outputLandmarks);
        tfliteOutputs.put(2, outputBoxes);

        // Run the inference call.
        Trace.beginSection("runInference");
        long startTime = SystemClock.uptimeMillis();
        runInference();
        long endTime = SystemClock.uptimeMillis();
        Trace.endSection();
        Log.d("TimeCost","run face Detection inference: " + (endTime - startTime));
        Trace.endSection();

        return getFaceAttr();
    }

    private List<FaceAttr> getFaceAttr(){
        outputProbs.flip();
        outputLandmarks.flip();
        outputBoxes.flip();

        int len = outputProbs.remaining();
        ArrayList<FaceAttr> faceAttrs = new ArrayList<>();
        FaceAttr detectedFaceTmp;

        for (int i = 0; i < len; i++) {
            float prob = outputProbs.get();
            if (prob < FACE_PROB_THRESHOLD){
                continue;
            }
            float top = outputBoxes.get();
            float left = outputBoxes.get();
            float bottom = outputBoxes.get();
            float right = outputBoxes.get();

            float[] landmarks = new float[NUM_LANDMARKS] ;
            for(int j = 0; j < NUM_LANDMARKS; j++){
                landmarks[j] = outputLandmarks.get();
            }
            detectedFaceTmp = new FaceAttr(prob, landmarks, new RectF(left, top, right, bottom));
            faceAttrs.add(detectedFaceTmp);
        }

        if (outputBoxes.hasRemaining())
            outputBoxes.position(outputBoxes.limit());

        outputProbs.compact();
        outputLandmarks.compact();
        outputBoxes.compact();

        return faceAttrs;
    }

    @Override
    protected String getModelPath() {
        return "mtcnn.tflite";
    }


    @Override
    protected void addPixelValue(int pixelValue) {
        imgData.putFloat(((pixelValue >> 16) & 0xFF));
        imgData.putFloat(((pixelValue >> 8) & 0xFF));
        imgData.putFloat((pixelValue & 0xFF));
    }
    @Override

    protected void runInference() {
        tflite.runForMultipleInputsOutputs(tfliteInputs, tfliteOutputs);
    }

    @Override
    public int getImageSizeX() {
        return IMAGE_SIZE;
    }

    @Override
    public int getImageSizeY() {
        return IMAGE_SIZE;
    }

    @Override
    protected int getNumBytesPerChannel() {
        return NUM_Bytes_Per_Channel;
    }

}
