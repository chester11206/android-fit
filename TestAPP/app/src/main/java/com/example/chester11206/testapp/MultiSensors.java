package com.example.chester11206.testapp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import com.google.android.gms.fitness.data.DataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiSensors {
    public Activity context;
    private SensorManager mSensorManager;
    private List<Sensor> mSensor;

    TextView stepView;
    TextView acceView;

    public static final Map<String, Integer> sensorstype_map = createSensorsTypeMap();
    private static Map<String, Integer> createSensorsTypeMap()
    {
        Map<String, Integer> myMap = new HashMap<String, Integer>();
        myMap.put("Step", Sensor.TYPE_STEP_COUNTER);
        myMap.put("Accelerometer", Sensor.TYPE_ACCELEROMETER);
        return myMap;
    }

    public void start(Activity activity, SensorManager SensorManager, List<String> sensors_list) {
        context = activity;
        stepView = (TextView) context.findViewById(R.id.stepView);
        acceView = (TextView) context.findViewById(R.id.acceView);
        stepView.setMovementMethod(new ScrollingMovementMethod());
        acceView.setMovementMethod(new ScrollingMovementMethod());

        this.mSensorManager = SensorManager;
        for (String sensor : sensors_list) {
            mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(sensorstype_map.get(sensor)), SensorManager.SENSOR_DELAY_FASTEST);
        }

    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        private float mStepOffset;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_STEP_COUNTER:
                    if (mStepOffset == 0) {
                        mStepOffset = event.values[0];
                        stepView.append("\nSteps: " + (event.values[0] - mStepOffset));
                    }
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    acceView.setText("\nAccelerometer X: " + event.values[0]
                     + "\nAccelerometer Y: " + event.values[1]
                     + "\nAccelerometer Z: " + event.values[2]);
                    break;

            }


        }
    };

}
