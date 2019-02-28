package com.example.android.tflitecamerademo;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.Interpreter;

public class MoveClassifier {

    // Options for configuring the Interpreter
    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    // The loaded TensorFlow Lite model
    private MappedByteBuffer tfliteModel;

    // An instance of the driver class to run model inference with Tensorflow Lite
    public Interpreter tflite;

    // Labels holding the output of the model
    private List<String> labelList;

    // An array to hold data of a single row to be fed into Tensorflow Lite as inputs
    protected float[] rowData;

    MoveClassifier(Activity activity) throws IOException {
        tfliteModel = loadModelFile(activity);
        tflite = new Interpreter(tfliteModel, tfliteOptions);
    }

    private String getModelPath(){
        return "movenet_graph.tflite";
    }

    private String getLabelPath(){
        return "labels.txt";
    }

    // Memory-map the model file in Assets.
    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(getModelPath());
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    // Reads label list from Assets
    public List<String> loadLabelList(Activity activity) throws IOException {
        List<String> labelList = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(activity.getAssets().open(getLabelPath())));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }
}
