package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
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
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

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

    private OkHttpClient mOkHttpClient;

    public void start(Activity activity){

        this.context = activity;

        txvResult = (TextView) this.context.findViewById(R.id.txvResult1);
        txvResult.setMovementMethod(new ScrollingMovementMethod());





        initializeLogging();
        findFitnessDataSources();
        //postAsynFile();

//        try{
//            String filename = "/HistoryAPI.txt";
//            // 存放檔案位置在 內部空間/Download/
//            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
//            String filepath = path.toString() + filename;
//            File file = new File(filepath);
//            if(file.createNewFile()){
//                System.out.println("Create file successed");
//                Log.i(TAG, "Create file successed");
//                writetxt(filepath, txvResult.getText().toString());
//            }
//        }catch(Exception e){
//            System.out.println(e);
//            Log.i(TAG, e.toString());
//        }


    }

    private void postAsynFile() {
        mOkHttpClient=new OkHttpClient();
//        File file = new File("/sdcard/wangshu.txt");
//        Request request = new Request.Builder()
//                .url("https://api.github.com/markdown/raw")
//                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, file))
//                .build();
//
//        mOkHttpClient.newCall(request).enqueue(new Snackbar.Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                Log.i("wangshu",response.body().string());
//            }
//        });

        //OkHttpClient client = new OkHttpClient();

        RequestBody formBody = new FormBody.Builder()
                .add("message", "Your message")
                .build();
        Request request = new Request.Builder()
                .url("https://api.github.com/markdown/raw")
                .post(formBody)
                .build();


        try {
            Response response = mOkHttpClient.newCall(request).execute();

            // Do something with the response.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Finds available data sources and attempts to register on a specific {@link DataType}. */
    private void findFitnessDataSources() {
        // [START find_data_sources]
        // Note: Fitness.SensorsApi.findDataSources() requires the ACCESS_FINE_LOCATION permission.
        Fitness.getSensorsClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .findDataSources(
                        new DataSourcesRequest.Builder()
                                .setDataTypes(//DataType.TYPE_LOCATION_SAMPLE,
                                        DataType.TYPE_STEP_COUNT_DELTA,
                                        DataType.TYPE_DISTANCE_DELTA,
                                        DataType.TYPE_SPEED,
                                        DataType.TYPE_ACTIVITY_SAMPLES)
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
                                    txvResult.append("Data Source Found: ");
                                    txvResult.append("\nData Source: " + dataSource.toString());
                                    txvResult.append("\nData Source type: " + dataSource.getDataType().getName());
                                    //Log.i(TAG, "\tData Source type equal: " + dataSource.getDataType().equals(DataType.TYPE_ACTIVITY_SAMPLES));
                                    //Log.i(TAG, "\tmListener null: " + (mListener == null));

                                    // Let's register a listener to receive Activity data!
                                    if (//dataSource.getDataType().equals(DataType.TYPE_LOCATION_SAMPLE) ||
                                            dataSource.getDataType().equals(DataType.TYPE_STEP_COUNT_DELTA) ||
                                                    dataSource.getDataType().equals(DataType.TYPE_DISTANCE_DELTA) ||
                                                    dataSource.getDataType().equals(DataType.TYPE_SPEED) ||
                                                    dataSource.getDataType().equals(DataType.TYPE_ACTIVITY_SAMPLES)
                                        //&& mListener == null
                                            ) {
                                        Log.i(TAG, "\tData source for " + dataSource.getDataType().getName() + " found!  Registering.");
                                        txvResult.append("\tData source for " + dataSource.getDataType().getName() + " found!  Registering.");
                                        registerFitnessDataListener(dataSource, dataSource.getDataType());
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
                        txvResult.append("Detecting...");

                        Calendar cal = Calendar.getInstance();
                        Date now = new Date();
                        cal.setTime(now);
                        long TimeNow = cal.getTimeInMillis();
                        DateFormat dateFormat = getDateTimeInstance();
                        Log.i(TAG, "Listen Time: " + dateFormat.format(TimeNow));
                        txvResult.append("Listen Time: " + dateFormat.format(TimeNow));

                        for (Field field : dataPoint.getDataType().getFields()) {
                            Value val = dataPoint.getValue(field);
                            Log.i(TAG, "Detected DataPoint field: " + field.getName());
                            Log.i(TAG, "Detected DataPoint value: " + val);
                            txvResult.append("Detected DataPoint field: " + field.getName());
                            txvResult.append("Detected DataPoint value: " + val);
                        }
                    }
                };

        Fitness.getSensorsClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .add(
                        new SensorRequest.Builder()
                                .setDataSource(dataSource) // Optional but recommended for custom data sets.
                                .setDataType(dataType) // Can't be omitted.
                                .setSamplingRate(10, TimeUnit.SECONDS)
                                .build(),
                        mListener)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Listener registered: " + dataType.getName());
                                    txvResult.append("Listener registered: " + dataType.getName());
                                } else {
                                    Log.e(TAG, "Listener not registered.", task.getException());
                                    txvResult.append("Listener not registered." + task.getException().toString());
                                }
                            }
                        });
        // [END register_data_listener]
    }

    /** Unregisters the listener with the Sensors API. */
    private void unregisterFitnessDataListener() {
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
                                    txvResult.append("Listener was removed!");
                                } else {
                                    Log.i(TAG, "Listener was not removed.");
                                    txvResult.append("Listener was not removed!");
                                }
                            }
                        });
        // [END unregister_data_listener]
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_unregister_listener) {
//            unregisterFitnessDataListener();
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

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

    public static void writetxt(String file, String content) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
            out.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(out != null){
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
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
