package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
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

    // the same, one for list and one for array
    private static ArrayList<DataType> sensors_list;
    private static DataType [] sensors_array;

    private static int step_interval;
    private static float distance_interval;
    private static int user_id;

    //private DatabaseReference mDatabase;

    public void start(Activity activity, List<String> sensors_list){

        // initiate
        this.context = activity;
        this.sensors_list = new ArrayList<DataType>();
        step_interval = 0;
        distance_interval = 0;
        user_id = 1;

        txvResult = (TextView) this.context.findViewById(R.id.txvResult1);
        txvResult.setMovementMethod(new ScrollingMovementMethod());

        List<String> existType = new ArrayList<String>();
        for (String key : sensors_list) {
            if(!existType.contains(key)) {
                this.sensors_list.add(MainActivity.datatype_map.get(key));
                existType.add(key);
            }
        }
        this.sensors_array = new DataType[this.sensors_list.size()];
        this.sensors_array = this.sensors_list.toArray(this.sensors_array);

        //mDatabase = FirebaseDatabase.getInstance().getReference("FitActivity");

        initializeLogging();
        findFitnessDataSources();
    }

    public class FitActivity {

        public String activity_confidence;
        public int Steps;
        public float Distance;
        public String Time;

        public FitActivity() {
            // Default constructor required for calls to DataSnapshot.getValue(User.class)
        }

        public FitActivity(String activity_confidence, int steps, float distance, String time) {
            this.activity_confidence = activity_confidence;
            this.Steps = steps;
            this.Distance = distance;
            this.Time = time;
        }

    }

    /** Finds available data sources and attempts to register on a specific {@link DataType}. */
    private void findFitnessDataSources() {
        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.

        final List<DataType> existType = new ArrayList<DataType>();
            Fitness.getSensorsClient(context, GoogleSignIn.getLastSignedInAccount(context))
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(sensors_array
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
                                             sensors_list.contains(dataSource.getDataType()) && !existType.contains(dataSource.getDataType())
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
                    @Override
                    public void onDataPoint(DataPoint dataPoint) {
                        Log.i(TAG, "Detecting...");
                        //txvResult.append("Detecting...");

                        Calendar cal = Calendar.getInstance();
                        Date now = new Date();
                        cal.setTime(now);
                        long TimeNow = cal.getTimeInMillis();
                        DateFormat dateFormat = getDateTimeInstance();
                        Log.i(TAG, "Listen Time: " + dateFormat.format(TimeNow));
                        txvResult.append("\n\n" + dateFormat.format(TimeNow));

                        for (Field field : dataPoint.getDataType().getFields()) {
                            Value val = dataPoint.getValue(field);
//                            if (field.equals(Field.FIELD_STEPS)) {
//                                step_interval += val.asInt();
//                            }
//                            else if (field.equals(Field.FIELD_DISTANCE)) {
//                                distance_interval += val.asFloat();
//                            }
//                            else if (field.equals(Field.FIELD_ACTIVITY_CONFIDENCE)) {
//                                txvResult.append("\nListen Time: " + dateFormat.format(TimeNow));
//                                txvResult.append("\n" + field.getName() + ": " + val);
//                                txvResult.append("\nStep: " + step_interval);
//                                txvResult.append("\nDistance: " + distance_interval);
//
////                                FitActivity fitActivity = new FitActivity(val.toString(), step_interval, distance_interval, dateFormat.format(TimeNow));
////                                FirebaseDatabase database = FirebaseDatabase.getInstance();
////                                DatabaseReference mDatabase = database.getReference("FitActivity");
////                                mDatabase.child(Integer.toString(user_id)).setValue(fitActivity);
////                                user_id ++;
//
//                                step_interval = 0;
//                                distance_interval = 0;
//                            }
                            Log.i(TAG, "Detected DataPoint field: " + field.getName());
                            Log.i(TAG, "Detected DataPoint value: " + val);
                            //txvResult.append("\nDetected DataPoint field: " + field.getName());
                            //txvResult.append("\nDetected DataPoint value: " + val);
                            txvResult.append("\n" + field.getName() + ": " + val);
                        }
                    }
                };

        Fitness.getSensorsClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .add(
                        new SensorRequest.Builder()
                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                .setDataType(dataType) // Can't be omitted.
                                .setSamplingRate(1, TimeUnit.SECONDS)
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
