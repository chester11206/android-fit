package com.example.chester11206.testapp;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
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
    LinearLayout rl;

    public static final Map<Integer, TextView> textview_map = new HashMap<Integer, TextView>();
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
        rl = (LinearLayout) context.findViewById(R.id.multisensors_view);
//        stepView = (TextView) context.findViewById(R.id.stepView);
//        acceView = (TextView) context.findViewById(R.id.acceView);
//        stepView.setMovementMethod(new ScrollingMovementMethod());
//        acceView.setMovementMethod(new ScrollingMovementMethod());

        this.mSensorManager = SensorManager;
        for (String sensor : sensors_list) {
            mSensorManager.registerListener(mSensorEventListener, mSensorManager.getDefaultSensor(sensorstype_map.get(sensor)), SensorManager.SENSOR_DELAY_FASTEST);
            TextView txv = new TextView(context);
            //txv.setText(sensor);
            LinearLayout.LayoutParams layoutParams=
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            txv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            txv.setMovementMethod(new ScrollingMovementMethod());
            txv.setLayoutParams(layoutParams);
            rl.addView(txv);
            textview_map.put(sensorstype_map.get(sensor), txv);
        }

    }

    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        private float mStepOffset;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            TextView txv = textview_map.get(event.sensor.getType());
            switch (event.sensor.getType()) {
                case Sensor.TYPE_STEP_COUNTER:
//                    if (mStepOffset == 0) {
//                        mStepOffset = event.values[0];
//                        txv.append("\nSteps: " + (event.values[0] - mStepOffset));
//                    }
                    txv.append("\nSteps: " + event.values[0]);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    txv.setText("\nAccelerometer X: " + event.values[0]
                     + "\nAccelerometer Y: " + event.values[1]
                     + "\nAccelerometer Z: " + event.values[2]);
                    break;

            }


        }
    };

}
