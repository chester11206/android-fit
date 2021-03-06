package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.result.DataSourcesResult;
import com.google.android.gms.fitness.FitnessOptions;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.text.DateFormat.getDateTimeInstance;

public class Sensors {
    public static final String TAG = "BasicSensorsApi";

    // [START mListener_variable_reference]
    // Need to hold a reference to this listener, as it's passed into the "unregister"
    // method in order to stop all sensors from sending data to this listener.
    private OnDataPointListener mListener;
    // [END mListener_variable_reference]
    TextView txvResult;

    public Activity context;
    private TimeThread overtime = new TimeThread();

    // the same, one for list and one for array
    private static ArrayList<DataType> sensors_list;
    private static DataType [] sensors_array;

    public static String real_activity;
    String [] activityItems = null;

    private int step_interval;
    private float distance_interval;
    private float longitude;
    private float latitude;
    private float accuracy;
    private float altitude;
    private long start_time;
    private long start_time2;
    private Map<DataType, Integer> to_predict = new HashMap<DataType, Integer>();

    private boolean startListen;
    private static final int msgKey1 = 1;

    private DatabaseReference mDatabase;
    public class FitActivity {

        public List<Float> activity_confidence = new ArrayList<Float>();
        public int steps;
        public float distance;
//        public float longitude;
//        public float latitude;
//        public float accuracy;
//        public float altitude;
        public long time;
        public int ground_truth;

        public FitActivity() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public FitActivity(Value activity_confidence, int steps, float distance, long time, String ground_truth) {
            Float [] temp_activity = new Float[8];
            for (String key : MainActivity.activity_map.keySet()) {
                if (activity_confidence.getKeyValue(key) == null) {
                    activity_confidence.setKeyValue(key, 0);
                }
                temp_activity[MainActivity.activity_map.get(key)] = activity_confidence.getKeyValue(key);

                //this.activity_confidence.set(MainActivity.activity_map.get(key), activity_confidence.getKeyValue(key));
            }
            this.activity_confidence = Arrays.asList(temp_activity);

            this.steps = steps;
            this.distance = distance;
//            this.longitude = longitude;
//            this.latitude = latitude;
//            this.accuracy = accuracy;
//            this.altitude = altitude;
            this.time = time;
            this.ground_truth = MainActivity.activity_map.get(ground_truth);
        }

    }

    public class TimeThread extends Thread {
        @Override
        public void run () {
            do {
                try {
                    Thread.sleep(20000);
                    Message msg = new Message();
                    msg.what = msgKey1;
                    mHandler.sendMessage(msg);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while(true);
        }
    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage (Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case msgKey1:
                    overTime();
                    break;
                default:
                    break;
            }
        }
    };
    private void overTime(){
        if(startListen && !topredict(to_predict)) {
            unregisterFitnessDataListener();
            step_interval = 0;
            distance_interval = 0;
            for (DataType key : to_predict.keySet()) {
                to_predict.put(key, 0);
            }

            //overtime.interrupt();
            sensorStart();
            txvResult.append("OverTime!");
        }
    }

    public void start(Activity activity, List<String> sensors_list){

        // initiate
        context = activity;
        this.sensors_list = new ArrayList<DataType>();
        activityItems = context.getResources().getStringArray(R.array.activity);

        step_interval = 0;
        distance_interval = 0;
        longitude = 0;
        latitude = 0;
        accuracy = 0;
        altitude = 0;

        txvResult = (TextView) this.context.findViewById(R.id.txvResult1);
        txvResult.setMovementMethod(new ScrollingMovementMethod());

        List<String> existType = new ArrayList<String>();
        for (String key : sensors_list) {
            if(!existType.contains(key)) {
                this.sensors_list.add(MainActivity.datatype_map.get(key));
                if (!MainActivity.datatype_map.get(key).equals(DataType.TYPE_ACTIVITY_SAMPLES)){
                    to_predict.put(MainActivity.datatype_map.get(key), 0);
                }
                existType.add(key);
            }
        }
        this.sensors_array = new DataType[this.sensors_list.size()];
        this.sensors_array = this.sensors_list.toArray(this.sensors_array);
//        to_predict = new int[this.sensors_list.size() - 1];
//        Arrays.fill(to_predict, 1);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeLogging();
        sensorStart();
        //overtime.start();
        //findFitnessDataSources();
    }

    private void sensorStart() {
        startListen = true;
        start_time = MainActivity.timeNow;
        start_time2 = MainActivity.timeNow;

        for (DataType sensor : sensors_array) {
            findFitnessDataSources(sensor);
            //break;
        }
        //findFitnessDataSources(DataType.TYPE_ACTIVITY_SAMPLES);
    }

    /** Finds available data sources and attempts to register on a specific {@link DataType}. */
    private void findFitnessDataSources(DataType sensors_item) {
        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.

        final List<DataType> existType = new ArrayList<DataType>();
            Fitness.getSensorsClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(sensors_item
                                        //DataType.TYPE_CYCLING_WHEEL_REVOLUTION
//                                        DataType.TYPE_LOCATION_SAMPLE,
//                                        DataType.TYPE_STEP_COUNT_DELTA,
//                                        DataType.TYPE_DISTANCE_DELTA,
//                                        DataType.TYPE_HEIGHT,
//                                        DataType.TYPE_SPEED,
                                        //DataType.TYPE_ACTIVITY_SAMPLES
                                )
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
                                    if (true){
                                        for (DataType key : to_predict.keySet()) {
                                            to_predict.put(key, 0);
                                        }
                                        long end_time = MainActivity.timeNow;
                                        long time_interval = (end_time - start_time) / 1000;
                                        start_time = MainActivity.timeNow;
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
                                        txvResult.append("\n" + time_interval + " " + val + " " + step_interval + " " + distance_interval);

                                        showChooseDialog(val, time_interval);
                                    }
                                }
                                else {
                                    if (dataPoint.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                                        step_interval += val.asInt();
                                    } else if (dataPoint.getDataType().equals(DataType.TYPE_DISTANCE_DELTA)) {
                                        distance_interval += val.asFloat();
                                    } else if (dataPoint.getDataType().equals(DataType.TYPE_LOCATION_SAMPLE)) {
                                        if (field.equals(Field.FIELD_LONGITUDE)) {
                                            longitude = val.asFloat();
                                        } else if (field.equals(Field.FIELD_LATITUDE)) {
                                            latitude = val.asFloat();
                                        } else if (field.equals(Field.FIELD_ACCURACY)) {
                                            accuracy = val.asFloat();
                                        } else if (field.equals(Field.FIELD_ALTITUDE)) {
                                            altitude = val.asFloat();
                                        }
                                    }
//                                    long end_time = MainActivity.timeNow;
//                                    long time_interval = (end_time - start_time2) / 1000;
//                                    start_time2 = MainActivity.timeNow;
//                                    txvResult.append("\n" + time_interval + " " + val);
                                    //txvResult.append("\nDataType: " + dataPoint.getDataType());

                                    to_predict.put(dataPoint.getDataType(), to_predict.get(dataPoint.getDataType()) + 1);
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
                            //txvResult.append("\n" + field.getName() + ": " + val);
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

    private void showChooseDialog(final Value val, final long time_interval) {
        final Vibrator vibrator = (Vibrator)context.getSystemService(context.VIBRATOR_SERVICE);
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
                txvResult.append("\nGround Truth: " + real_activity);
                txvResult.append("\n-------------------\n");

                for (DataType dataType : sensors_list) {
                    if (dataType.equals(DataType.TYPE_STEP_COUNT_DELTA)) {
                        FitActivity fitActivity = new FitActivity(val, step_interval, -1, time_interval, real_activity);
                        mDatabase.child("fitActivity").push().setValue(fitActivity);
                        break;
                    }
                    else if (dataType.equals(DataType.TYPE_DISTANCE_DELTA)) {
                        FitActivity fitActivity = new FitActivity(val, -1, distance_interval, time_interval, real_activity);
                        mDatabase.child("fitActivity").push().setValue(fitActivity);
                        break;
                    }
                }

                step_interval = 0;
                distance_interval = 0;
                //FitActivity fitActivity = new FitActivity(val, step_interval, time_interval, real_activity);
                //FirebaseDatabase database = FirebaseDatabase.getInstance();
                //DatabaseReference mDatabase = database.getReference();

                startListen = true;
                start_time = MainActivity.timeNow;
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

    /** Initializes a custom log class that outputs both to in-app targets and logcat. */
    private void initializeLogging() {
        // Wraps Android's native log framework.
//        LogWrapper logWrapper = new LogWrapper();
//        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
//        Log.setLogNode(logWrapper);
//        // Filter strips out everything except the message text.
//        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
//        logWrapper.setNext(msgFilter);
//        // On screen logging via a customized TextView.
//        LogView logView = (LogView) this.context.findViewById(R.id.txvResult1);
//
//        // Fixing this lint errors adds logic without benefit.
//        // noinspection AndroidLintDeprecation
//        logView.setTextAppearance(R.style.Log);
//
//        logView.setBackgroundColor(Color.WHITE);
//        msgFilter.setNext(logView);
        Log.i(TAG, "Ready.");
        txvResult.append("Ready.");
    }


    /** Callback received when a permissions request has been completed. */
//    @Override
//    public void onRequestPermissionsResult(
//            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        Log.i(TAG, "onRequestPermissionResult");
//        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
//            if (grantResults.length <= 0) {
//                // If user interaction was interrupted, the permission request is cancelled and you
//                // receive empty arrays.
//                Log.i(TAG, "User interaction was cancelled.");
//            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Permission was granted.
//                findFitnessDataSourcesWrapper();
//            } else {
//                // Permission denied.
//
//                // In this Activity we've chosen to notify the user that they
//                // have rejected a core permission for the app since it makes the Activity useless.
//                // We're communicating this message in a Snackbar since this is a sample app, but
//                // core permissions would typically be best requested during a welcome-screen flow.
//
//                // Additionally, it is important to remember that a permission might have been
//                // rejected without asking the user for permission (device policy or "Never ask
//                // again" prompts). Therefore, a user interface affordance is typically implemented
//                // when permissions are denied. Otherwise, your app could appear unresponsive to
//                // touches or interactions which have required permissions.
//                Snackbar.make(
//                        this.context.findViewById(R.id.sensors_view),
//                        R.string.permission_denied_explanation,
//                        Snackbar.LENGTH_INDEFINITE)
//                        .setAction(
//                                R.string.settings,
//                                new View.OnClickListener() {
//                                    @Override
//                                    public void onClick(View view) {
//                                        // Build intent that displays the App settings screen.
//                                        Intent intent = new Intent();
//                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
//                                        Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
//                                        intent.setData(uri);
//                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                                        context.setActivity(intent);
//                                    }
//                                })
//                        .show();
//            }
//        }
//    }
}
