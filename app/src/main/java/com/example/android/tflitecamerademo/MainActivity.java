package com.example.android.tflitecamerademo;

import android.content.Context;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView xTvw, yTvw, zTvw, resultTvw, resultTvw2, resultTvw3, resultTvw4, resultTvw5;
    private Sensor accSensor;
    private SensorManager senManager;
    private Vibrator vib;
    private File file;
    private String filename;
    private long endTime;
    private long startTime;
    private long currentTime;
    private MoveClassifier moveClassifier;
    private List<String> labelList;
    private int[] outputs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // create sensor manager
        senManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        // create accelerometer sensor, ignore influence of gravity
        accSensor = senManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);

        // initialize vibrator
        vib = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        // create file object for storing recordings
        filename = "movement_log.csv";
        file = new File(getApplicationContext().getFilesDir(), filename);

        // assign TextViews to allow for streaming of values to screen
        xTvw = (TextView)findViewById(R.id.xTvw);
        yTvw = (TextView)findViewById(R.id.yTvw);
        zTvw = (TextView)findViewById(R.id.zTvw);

        // assign TextViews to allow for display of results
        resultTvw = (TextView)findViewById(R.id.resultTvw);
        resultTvw2 = (TextView)findViewById(R.id.resultTvw2);
        resultTvw3 = (TextView)findViewById(R.id.resultTvw3);
        resultTvw4 = (TextView)findViewById(R.id.resultTvw4);
        resultTvw5 = (TextView)findViewById(R.id.resultTvw5);

        // set left button functionality on click
        Button magicBtn = (Button)findViewById(R.id.magicBtn);
        magicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // clear old recording file if it exists;
                if (file.exists()){
                    file.delete();
                }

                // reset result TextViews
                resultTvw.setText("0-2s:");
                resultTvw2.setText("2-4s:");
                resultTvw3.setText("4-6s:");
                resultTvw4.setText("6-8s:");
                resultTvw5.setText("8-10s:");

                // notify user about start of recording
                Toast.makeText(getApplicationContext(), R.string.toast_message_1 , Toast.LENGTH_LONG).show();

                // record 10.5 seconds to have at least 50 data points!
                endTime = System.currentTimeMillis() + 12500;
                startTime = System.currentTimeMillis() + 2000;

                // define the SensorEventListener
                final SensorEventListener accSensorEventListener = new SensorEventListener() {

                    private int instance_count = 1;
                    private String entry;

                    // called when there is a new sensor event
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        // update current timestamp
                        currentTime = System.currentTimeMillis();
                        // check if 10s are over, give user 2s to start walking or running
                        if (currentTime > startTime && currentTime < endTime) {

                            // streaming sensor values to screen for the user
                            xTvw.setText(Float.toString(event.values[0]));
                            yTvw.setText(Float.toString(event.values[1]));
                            zTvw.setText(Float.toString(event.values[2]));

                            // writing the sensor values to a row in the csv file, create new row every 10 instances (every 2s)
                            if((instance_count % 10) != 0){
                                entry = xTvw.getText().toString() + "," + yTvw.getText().toString() + "," + zTvw.getText().toString() + ",";
                            } else {
                                entry = xTvw.getText().toString() + "," + yTvw.getText().toString() + "," + zTvw.getText().toString() + "\n";
                            }
                            FileOutputStream f = null;
                            try {
                                f = openFileOutput(filename, MODE_APPEND);
                                f.write(entry.getBytes());
                                f.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            instance_count = instance_count + 1;
                        } else if (currentTime > endTime) {
                            // notify the user that recording has stopped and unregister listener
                            Toast.makeText(getApplicationContext(), R.string.toast_message_2, Toast.LENGTH_LONG).show();
                            vib.vibrate(200);
                            senManager.unregisterListener(this, accSensor);
                        }
                    }
                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {
                        // not used
                    }
                };
                // register sensor listener, SENSOR_DELAY_NORMAL yields around 5Hz recording frequency which matches the kaggle dataset
                senManager.registerListener(accSensorEventListener, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }
        });
        // set second button functionality
        Button magicBtn2 = (Button)findViewById(R.id.magicBtn2);
        magicBtn2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                // only infer if recorded data is available, TODO: check for amount of available data
                if (file.exists()){

                    // initialize list for csv readout and read into list
                    List list = null;
                    CSVFile csvFile = new CSVFile(getApplicationContext().getFilesDir() + "/" + filename);
                    try {
                        list = csvFile.read();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                    // convert list to float array
                    float[][] inputArr = new float[list.size()][];
                    for(int row = 0; row<list.size(); row++){
                        String[] thisRowStrings = (String[]) list.get(row);
                        float[] thisRowFloats = new float[thisRowStrings.length];
                        for(int c=0; c<thisRowStrings.length; c++){
                            thisRowFloats[c]=Float.parseFloat(thisRowStrings[c]);
                        }
                        inputArr[row]=thisRowFloats;
                    }

                    // initialize classifier
                    try {
                        moveClassifier = new MoveClassifier(MainActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.i("Classifier", "Classifier not initialized");
                    }

                    // initialize outputs array
                    outputs = new int[5];

                    // run inference for 5 rows, each resembling about 10 datapoints (every 2s)
                    for (int i = 0; i < 5; i++){
                        long [] output = new long[1];

                        // convert row to ByteBuffer
                        ByteBuffer byteBuf = ByteBuffer.allocateDirect(120);
                        byteBuf.order(ByteOrder.nativeOrder());
                        for (int j = 0; j < inputArr[0].length; j++){
                            byteBuf.putFloat(inputArr[i][j]);
                        }

                        // run inference on row with interpreter tflite and append result to outputs array
                        moveClassifier.tflite.run(byteBuf, output);
                        outputs[i] = (int)output[0];
                    }
                    // close interpreter
                    moveClassifier.tflite.close();

                    // get labelList
                    try {
                        labelList = moveClassifier.loadLabelList(MainActivity.this);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    // display results
                    resultTvw.setText("0-2s:" + labelList.get(outputs[0]));
                    resultTvw2.setText("2-4s:" + labelList.get(outputs[1]));
                    resultTvw3.setText("4-6s:" + labelList.get(outputs[2]));
                    resultTvw4.setText("6-8s:" + labelList.get(outputs[3]));
                    resultTvw5.setText("8-10s" + labelList.get(outputs[4]));
                }
            }
        });
    }
}
