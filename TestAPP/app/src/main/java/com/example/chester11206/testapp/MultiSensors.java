package com.example.chester11206.testapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.data.Value;
import com.google.android.gms.fitness.request.DataSourcesRequest;
import com.google.android.gms.fitness.request.OnDataPointListener;
import com.google.android.gms.fitness.request.SensorRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateTimeInstance;

public class MultiSensors {
    public Activity context;
    public static final String TAG = "MultiSensorsApi";
    TextView txvResult;
    private OnDataPointListener mListener;

    private SensorManager mSensorManager;
    private List<Sensor> mSensor;

    private String [] activityItems = null;
    private String real_activity;

    private long start_time;
    private boolean startListen = true;
    private long time_interval = 0;
    private float distance_interval = 0;
    private float step_interval = 0;
    private float stepStop = 0;
    private float stepStart = 0;

    private Map<DataType, Integer> to_predict = new HashMap<DataType, Integer>();

    private Handler handler = new Handler( );
    private Runnable runnable = new Runnable( ) {
        public void run ( ) {
            getActivity();
            handler.postDelayed(this,5000);
        }
    };

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
        txvResult = (TextView) this.context.findViewById(R.id.multisensorstxView);
        txvResult.setMovementMethod(new ScrollingMovementMethod());
        rl = (LinearLayout) context.findViewById(R.id.multisensors_view);
        activityItems = context.getResources().getStringArray(R.array.activity);

        this.mSensorManager = SensorManager;
        for (String sensor : sensors_list) {

            TextView txv = new TextView(context);
            //txv.setText(sensor);
            LinearLayout.LayoutParams layoutParams=
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            txv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
            txv.setMovementMethod(new ScrollingMovementMethod());
            txv.setLayoutParams(layoutParams);
            rl.addView(txv);
            textview_map.put(sensorstype_map.get(sensor), txv);

            mSensorManager.registerListener(mSensorEventListener,
                    mSensorManager.getDefaultSensor(sensorstype_map.get(sensor)),
                    SensorManager.SENSOR_DELAY_FASTEST);
        }

        sensorStart();
        //handler.postDelayed(runnable,1000);

    }

    private void sensorStart() {
        startListen = true;
        start_time = MainActivity.timeNow;

        to_predict.put(DataType.TYPE_DISTANCE_DELTA, 0);
        findFitnessDataSources(DataType.TYPE_ACTIVITY_SAMPLES);
        findFitnessDataSources(DataType.TYPE_DISTANCE_DELTA);
    }

    private void getActivity(){

        handler.removeCallbacks(runnable);
        startListen = false;
        step_interval = stepStop - stepStart;
        //showChooseDialog();

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
                    if (stepStart == 0) {
                        stepStart = event.values[0];
                    }
                    if (startListen) {
                        stepStop = event.values[0];
                    }
                    else {
                        stepStart = event.values[0];
                    }
//                    if (mStepOffset == 0) {
//                        mStepOffset = event.values[0];
//                        txv.append("\nSteps: " + (event.values[0] - mStepOffset));
//                    }
                    //txv.append("\nSteps: " + event.values[0]);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    txv.setText("\nAccelerometer X: " + event.values[0]
                     + "\nAccelerometer Y: " + event.values[1]
                     + "\nAccelerometer Z: " + event.values[2]);
                    break;

            }


        }
    };

    private void findFitnessDataSources(DataType sensors_item) {
        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.

        final List<DataType> existType = new ArrayList<DataType>();
        Fitness.getSensorsClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(sensors_item)
                                .setDataSourceTypes(DataSource.TYPE_RAW, DataSource.TYPE_DERIVED)
                                .build())
                .addOnSuccessListener(
                        new OnSuccessListener<List<DataSource>>() {
                            @Override
                            public void onSuccess(List<DataSource> dataSources) {
                                for (DataSource dataSource : dataSources) {
                                    Log.i(TAG, "Data Source Found: ");
                                    Log.i(TAG, "\tData Source: " + dataSource.toString());
                                    Log.i(TAG, "\tData Source type: " + dataSource.getDataType().getName());
                                    //txvResult.append("Data Source Found: ");
                                    //txvResult.append("\nData Source: " + dataSource.toString());
                                    //txvResult.append("\nData Source type: " + dataSource.getDataType().getName());
                                    //Log.i(TAG, "\tData Source type equal: " + dataSource.getDataType().equals(DataType.TYPE_ACTIVITY_SAMPLES));
                                    //Log.i(TAG, "\tmListener null: " + (mListener == null));

                                    // Let's register a listener to receive Activity data!
                                    if (//dataSource.getDataType().equals(DataType.TYPE_LOCATION_SAMPLE) ||
//                                            dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA) ||
//                                                    dataSource.getDataType().equals(DataType.TYPE_DISTANCE_DELTA) ||
//                                                    dataSource.getDataType().equals(DataType.TYPE_HEIGHT) ||
//                                                    dataSource.getDataType().equals(DataType.TYPE_SPEED) ||
//                                                    dataSource.getDataType().equals(DataType.TYPE_ACTIVITY_SAMPLES)
                                            sensors_item.equals(dataSource.getDataType()) && !existType.contains(dataSource.getDataType())
                                        //&& mListener == null
                                            ) {

                                        Log.i(TAG, "\tData source for " + dataSource.getDataType().getName() + " found!  Registering.");
                                        txvResult.append("\nData source for " + dataSource.getDataType().getName() + " found!  Registering.");
                                        registerFitnessDataListener(dataSource, dataSource.getDataType());
                                        existType.add(dataSource.getDataType());
                                    }
                                }
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "failed", e);
                                txvResult.append("failed: " + e.toString());
                            }
                        });
        // [END find_data_sources]
    }

    /**
     * Registers a listener with the Sensors API for the provided {@link DataSource} and {@link
     * DataType} combo.
     */
    private void registerFitnessDataListener(DataSource dataSource, final DataType dataType) {
        // [START register_data_listener]
        mListener =
                new OnDataPointListener() {
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onDataPoint(DataPoint dataPoint) {
                        Log.i(TAG, "Detecting...");
                        //for (int predict : to_predict) txvResult.append(String.valueOf(predict));
                        //txvResult.append("Detecting...");

                        DateFormat dateFormat = getDateTimeInstance();
//                        Calendar cal = Calendar.getInstance();
//                        Date now = new Date();
//                        cal.setTime(now);
//                        long TimeNow = cal.getTimeInMillis();
//                        Log.i(TAG, "Listen Time: " + dateFormat.format(TimeNow));
//                        txvResult.append("\n\n" + dateFormat.format(TimeNow));
                        if (startListen) {
                            //unregisterFitnessDataListener();
                            for (Field field : dataPoint.getDataType().getFields()) {
                                Value val = dataPoint.getValue(field);
                                if (dataType.equals(DataType.TYPE_ACTIVITY_SAMPLES)) {
                                    if (topredict(to_predict)){
                                        txvResult.append("\n" + field.getName() + ": " + val);
                                        for (DataType key : to_predict.keySet()) {
                                            to_predict.put(key, 0);
                                        }
                                        long end_time = MainActivity.timeNow;
                                        time_interval = (end_time - start_time) / 1000;
                                        step_interval = stepStop - stepStart;
                                        startListen = false;

//                                        txvResult.append("\nListen Time: " + dateFormat.format(start_time) + "to" + dateFormat.format(end_time));
//                                        txvResult.append("\nTime_Interval(sec): " + time_interval);
//                                        for (String key : MainActivity.activity_map.keySet()) {
//                                            txvResult.append("\n" + key + ": " + val.getKeyValue(key));
//                                        }
//                                        txvResult.append("\n" + field.getName() + ": " + val);
//                                        txvResult.append("\nStep: " + step_interval);
//                                        txvResult.append("\nDistance: " + distance_interval);
//                                        txvResult.append("\nLongitude: " + longitude);
//                                        txvResult.append("\nLatitude: " + latitude);
//                                        txvResult.append("\nAccuracy: " + accuracy);
//                                        txvResult.append("\nAltitude: " + altitude);
                                        //txvResult.append("\n" + time_interval + " " + val + " " + step_interval + " " + distance_interval);

                                        showChooseDialog(val);
                                    }
                                }
                                else {
                                    if (dataPoint.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                                        step_interval += val.asInt();
                                    }
                                    else if (dataPoint.getDataType().equals(DataType.TYPE_DISTANCE_DELTA)) {
                                        distance_interval += val.asFloat();
                                    }
//                                    else if (dataPoint.getDataType().equals(DataType.TYPE_LOCATION_SAMPLE)) {
//                                        if (field.equals(Field.FIELD_LONGITUDE)) {
//                                            longitude = val.asFloat();
//                                        } else if (field.equals(Field.FIELD_LATITUDE)) {
//                                            latitude = val.asFloat();
//                                        } else if (field.equals(Field.FIELD_ACCURACY)) {
//                                            accuracy = val.asFloat();
//                                        } else if (field.equals(Field.FIELD_ALTITUDE)) {
//                                            altitude = val.asFloat();
//                                        }
//                                    }
//                                    long end_time = MainActivity.timeNow;
//                                    long time_interval = (end_time - start_time2) / 1000;
//                                    start_time2 = MainActivity.timeNow;
//                                    txvResult.append("\n" + time_interval + " " + val);
                                    //txvResult.append("\nDataType: " + dataPoint.getDataType());

                                    to_predict.put(dataPoint.getDataType(), to_predict.get(dataPoint.getDataType()) + 1);
                                    txvResult.append("\n" + field.getName() + ": " + val);
                                    txvResult.append("\nTo predict: " + topredict(to_predict));
//                                    if (topredict(to_predict)){
//                                        for (DataType key : to_predict.keySet()) {
//                                            to_predict.put(key, 0);
//                                        }
//                                        long end_time = MainActivity.timeNow;
//                                        long time_interval = (end_time - start_time) / 1000;
//                                        start_time = MainActivity.timeNow;
//                                        startListen = false;
//
////                                        txvResult.append("\nListen Time: " + dateFormat.format(start_time) + "to" + dateFormat.format(end_time));
////                                        txvResult.append("\nTime_Interval(sec): " + time_interval);
////                                        for (String key : MainActivity.activity_map.keySet()) {
////                                            txvResult.append("\n" + key + ": " + val.getKeyValue(key));
////                                        }
////                                        txvResult.append("\n" + field.getName() + ": " + val);
////                                        txvResult.append("\nStep: " + step_interval);
////                                        txvResult.append("\nDistance: " + distance_interval);
////                                        txvResult.append("\nLongitude: " + longitude);
////                                        txvResult.append("\nLatitude: " + latitude);
////                                        txvResult.append("\nAccuracy: " + accuracy);
////                                        txvResult.append("\nAltitude: " + altitude);
//                                        txvResult.append("\n" + time_interval + " " + step_interval + " " + distance_interval);
//
//                                        step_interval = 0;
//                                        distance_interval = 0;
//                                        showChooseDialog(val, time_interval);
//                                    }

//                                    if (topredict(to_predict)) {
//                                        for (DataType key : to_predict.keySet()) {
//                                            to_predict.put(key, 0);
//                                        }
//                                        findFitnessDataSources(DataType.TYPE_ACTIVITY_SAMPLES);
//                                    }
//                                    else if (!dataType.equals(DataType.TYPE_ACTIVITY_SAMPLES)){
//                                        for (DataType key : to_predict.keySet()) {
//                                            if (to_predict.get(key) == 0) {
//                                                findFitnessDataSources(key);
//                                                break;
//                                            }
//                                        }
//                                    }
                                }
                            }

//                            for (DataType key : to_predict.keySet()) {
//                                for (String key2 : MainActivity.datatype_map.keySet()) {
//                                    if (MainActivity.datatype_map.get(key2).equals(key)) {
//                                        txvResult.append("\n" + key2 + ": " + to_predict.get(key));
//                                        break;
//                                    }
//                                }
//                            }


                            //Log.i(TAG, "Detected DataPoint field: " + field.getName());
                            //Log.i(TAG, "Detected DataPoint value: " + val);
                            //txvResult.append("\nDetected DataPoint field: " + field.getName());
                            //txvResult.append("\nDetected DataPoint value: " + val);
                        }
                    }
                };

        Fitness.getSensorsClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .add(
                        new SensorRequest.Builder()
                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                .setDataType(dataType) // Can't be omitted.
                                .setSamplingRate(5, TimeUnit.SECONDS)
                                //.setFastestRate(10, TimeUnit.SECONDS)
                                //.setMaxDeliveryLatency(10, TimeUnit.SECONDS)
                                .build(),
                        mListener)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Listener registered: " + dataType.getName());
                                    txvResult.append("\nListener registered: " + dataType.getName());
                                } else {
                                    Log.e(TAG, "Listener not registered.", task.getException());
                                    txvResult.append("\nListener not registered." + task.getException().toString());
                                }
                            }
                        });
        // [END register_data_listener]
    }

    private boolean topredict(Map<DataType, Integer> to_predict) {
        boolean result = true;
        for (DataType key : to_predict.keySet()) {
            if (to_predict.get(key) == 0) {
                result = false;
                break;
            }
        }
        return result;
    }

    private void showChooseDialog(Value val) {

        final Vibrator vibrator = (Vibrator) context.getSystemService(context.VIBRATOR_SERVICE);
        long[] patter = {1000, 1000};
        vibrator.vibrate(patter, 0);

        AlertDialog.Builder builderActivity = new AlertDialog.Builder(context);
        builderActivity.setTitle("Real Activity");
        builderActivity.setSingleChoiceItems(activityItems, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                real_activity = activityItems[i];
            }
        });
        builderActivity.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                vibrator.cancel();
                txvResult.append("\nTime: " + time_interval);
                txvResult.append("\nSteps: " + step_interval);
                txvResult.append("\nDistance: " + distance_interval);
                txvResult.append("\nActivity: " + val);
                txvResult.append("\nGround Truth: " + real_activity);
                txvResult.append("\n-------------------\n");

//                for (DataType dataType : sensors_list) {
//                    if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)) {
//                        Sensors.FitActivity fitActivity = new Sensors.FitActivity(val, step_interval, -1, time_interval, real_activity);
//                        mDatabase.child("fitActivity").push().setValue(fitActivity);
//                        break;
//                    }
//                    else if (dataType.equals(DataType.TYPE_DISTANCE_DELTA)) {
//                        Sensors.FitActivity fitActivity = new Sensors.FitActivity(val, -1, distance_interval, time_interval, real_activity);
//                        mDatabase.child("fitActivity").push().setValue(fitActivity);
//                        break;
//                    }
//                }

                step_interval = 0;
                //FitActivity fitActivity = new FitActivity(val, step_interval, time_interval, real_activity);
                //FirebaseDatabase database = FirebaseDatabase.getInstance();
                //DatabaseReference mDatabase = database.getReference()
                startListen = true;
                start_time = MainActivity.timeNow;
                //handler.postDelayed(runnable,5000);

                //unregisterFitnessDataListener();
                //sensorStart();
//                boolean hasChoose = false;
//                for (boolean flag : tempFlags) {
//                    if (flag) {
//                        hasChoose = true;
//                        break;
//                    }
//                }
//                if (hasChoose){
//                    List<String> result = new ArrayList<String>();
//                    flags = tempFlags.clone();
//                    for (int i = 0; i < flags.length; i++) {
//                        if(flags[i])
//                        {
//                            result.add(sensorItems[i]);
//                        }
//                    }
//                    sensors_list = new ArrayList<String>(result);
//                }
//                else {
//                    txvResult = (TextView) findViewById(R.id.txvResult1);
//                    txvResult.setMovementMethod(new ScrollingMovementMethod());
//                    txvResult.setText("");
//                    txvResult.setText("You haven't choose the sensors!");
//                }
            }
        });

        builderActivity.setNegativeButton("Stop", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                vibrator.cancel();
                txvResult.append("\nGround Truth: " + real_activity);
                txvResult.append("\n-------------------\n");

                step_interval = 0;
                distance_interval = 0;

                //FitActivity fitActivity = new FitActivity(val, step_interval, distance_interval, longitude, latitude, accuracy, altitude, time_interval, real_activity);
                //FirebaseDatabase database = FirebaseDatabase.getInstance();
                //DatabaseReference mDatabase = database.getReference();
                //mDatabase.child("fitActivity").push().setValue(fitActivity);

                unregisterFitnessDataListener();
            }
        });
        builderActivity.show();
    }

    /** Unregisters the listener with the Sensors API. */
    void unregisterFitnessDataListener() {
        if (mListener == null) {
            // This code only activates one listener at a time.  If there's no listener, there's
            // nothing to unregister.
            return;
        }

        // [START unregister_data_listener]
        // Waiting isn't actually necessary as the unregister call will complete regardless,
        // even if called from within onStop, but a callback can still be added in order to
        // inspect the results.
        Fitness.getSensorsClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .remove(mListener)
                .addOnCompleteListener(
                        new OnCompleteListener<Boolean>() {
                            @Override
                            public void onComplete(@NonNull Task<Boolean> task) {
                                if (task.isSuccessful() && task.getResult()) {
                                    Log.i(TAG, "Listener was removed!");
                                    txvResult.append("\nListener was removed!");
                                } else {
                                    Log.i(TAG, "Listener was not removed.");
                                    txvResult.append("\nListener was not removed!");
                                }
                            }
                        });
        // [END unregister_data_listener]
    }

}
