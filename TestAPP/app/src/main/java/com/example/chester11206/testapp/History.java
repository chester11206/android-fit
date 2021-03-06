package com.example.chester11206.testapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fit.samples.common.logger.LogView;
import com.google.android.gms.fit.samples.common.logger.LogWrapper;
import com.google.android.gms.fit.samples.common.logger.MessageOnlyLogFilter;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataDeleteRequest;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.request.DataUpdateRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.text.DateFormat.getDateInstance;
import static java.text.DateFormat.getDateTimeInstance;
import static java.text.DateFormat.getTimeInstance;

public class History {
    public static final String TAG = "BasicHistoryApi";
    // Identifier to identify the sign in activity.

    public static TextView txvResult;

    public Activity context;

    public void start(Activity activity){

        this.context = activity;

        txvResult = (TextView) this.context.findViewById(R.id.txvResult2);
        txvResult.setMovementMethod(new ScrollingMovementMethod());

        initializeLogging();
        insertAndReadData();
    }


    /**
     * Inserts and reads data by chaining {@link Task} from {@link #insertData()} and {@link
     * #readHistoryData()}.
     */
    private void insertAndReadData() {
        insertData()
                .continueWithTask(
                        new Continuation<Void, Task<DataReadResponse>>() {
                            @Override
                            public Task<DataReadResponse> then(@NonNull Task<Void> task) throws Exception {
                                return readHistoryData();
                            }
                        });
    }

    /** Creates a {@link DataSet} and inserts it into user's Google Fit history. */
    private Task<Void> insertData() {
        // Create a new dataset and insertion request.
        DataSet dataSet = insertFitnessData();

        // Then, invoke the History API to insert the data.
        Log.i(TAG, "Inserting the dataset in the History API.");
        //txvResult.append("Inserting the dataset in the History API.");
        return Fitness.getHistoryClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .insertData(dataSet)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // At this point, the data has been inserted and can be read.
                                    Log.i(TAG, "Data insert was successful!");
                                    //txvResult.append("Data insert was successful!");
                                } else {
                                    Log.e(TAG, "There was a problem inserting the dataset.", task.getException());
                                    //txvResult.append("There was a problem inserting the dataset." + task.getException().toString());
                                }
                            }
                        });
    }

    /**
     * Asynchronous task to read the history data. When the task succeeds, it will print out the data.
     */
    private Task<DataReadResponse> readHistoryData() {
        // Begin by creating the query.
        DataReadRequest readRequest = queryFitnessData();

        // Invoke the History API to fetch the data with the query
        return Fitness.getHistoryClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .readData(readRequest)
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                // For the sake of the sample, we'll print the data so we can see what we just
                                // added. In general, logging fitness information should be avoided for privacy
                                // reasons.
                                printData(dataReadResponse);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "There was a problem reading the data.", e);
                                //txvResult.append("There was a problem reading the data." + e.toString());
                            }
                        });
    }

    /**
     * Creates and returns a {@link DataSet} of step count data for insertion using the History API.
     */
    private DataSet insertFitnessData() {
        Log.i(TAG, "Creating a new data insert request.");
        //txvResult.append("Creating a new data insert request.");

        // [START build_insert_data_request]
        // Set a start and end time for our data, using a start time of 1 hour before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        long startTime = cal.getTimeInMillis();

        // Create a data source
        DataSource dataSource =
                new DataSource.Builder()
                        .setAppPackageName(this.context)
                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .setStreamName(TAG + " - step count")
                        .setType(DataSource.TYPE_RAW)
                        .build();

        // Create a data set
        int stepCountDelta = 950;
        DataSet dataSet = DataSet.create(dataSource);
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint =
                dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta);
        dataSet.add(dataPoint);
        // [END build_insert_data_request]

        return dataSet;
    }

    /** Returns a {@link DataReadRequest} for all step count changes in the past week. */
    public static DataReadRequest queryFitnessData() {
        // [START build_read_data_request]
        // Setting a start and end date using a range of 1 week before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        java.text.DateFormat dateFormat = getDateInstance();
        Log.i(TAG, "Range Start: " + dateFormat.format(startTime));
        Log.i(TAG, "Range End: " + dateFormat.format(endTime));
        txvResult.append("\nRange Start: " + dateFormat.format(startTime));
        txvResult.append("\nRange End: " + dateFormat.format(endTime));

        DataReadRequest readRequest =
                new DataReadRequest.Builder()
                        // The data request can specify multiple data types to return, effectively
                        // combining multiple data queries into one call.
                        // In this example, it's very unlikely that the request is for several hundred
                        // datapoints each consisting of a few steps and a timestamp.  The more likely
                        // scenario is wanting to see how many steps were walked per day, for 7 days.
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                        .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();
        // [END build_read_data_request]

        return readRequest;
    }

    /**
     * Logs a record of the query result. It's possible to get more constrained data sets by
     * specifying a data source or data type, but for demonstrative purposes here's how one would dump
     * all the data. In this sample, logging also prints to the device screen, so we can see what the
     * query returns, but your app should not log fitness information as a privacy consideration. A
     * better option would be to dump the data you receive to a local data directory to avoid exposing
     * it to other applications.
     */
    public static void printData(DataReadResponse dataReadResult) {
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            txvResult.append("\nNumber of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            int bucket_idx = 0;
            for (Bucket bucket : dataReadResult.getBuckets()) {
                bucket_idx++;
                txvResult.append("\nBucket " + bucket_idx);
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            txvResult.append("\nNumber of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        // [END parse_read_data_result]
    }

    // [START parse_dataset]
    private static void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        txvResult.append("\nData returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getDateTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            txvResult.append("\nData point:");
            txvResult.append("\nType: " + dp.getDataType().getName());
            txvResult.append("\nStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            txvResult.append("\nEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));

            for (Field field : dp.getDataType().getFields()) {
                if (field.equals(Field.FIELD_ACTIVITY)) {
                    Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field).asActivity());
                    txvResult.append("\nField: " + field.getName() + " Value: " + dp.getValue(field).asActivity());
                }
                else {
                    Log.i(TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
                    txvResult.append("\nField: " + field.getName() + " Value: " + dp.getValue(field));
                }
                writetxt(dp, field);
            }
        }
    }

    private static void writetxt(DataPoint dp, Field field) {
        DateFormat dateFormat = getTimeInstance();
        String filename = "/HistoryAPI.txt";
        // 存放檔案位置在 內部空間/Download/
        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filepath = path.toString() + filename;
        File file = new File(filepath);
        FileOutputStream Output = null;
        //Log.i(TAG, "\tPathName: " + file.toString());
        try
        {
            // 第二個參數為是否 append
            // 若為 true，則新加入的文字會接續寫在文字檔的最後

            Output = new FileOutputStream(file, true);

            String outputdata = "\tType: " + dp.getDataType().getName() +
                    "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) +
                    "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) +
                    "\tField: " + field.getName() + " Value: " + dp.getValue(field) + "\n";
//          String dateformat = "yyyyMMdd kk:mm:ss";
//          SimpleDateFormat df = new SimpleDateFormat(dateformat);
//          df.applyPattern(dateformat);
//          String string =  df.format(new Date()) + " : " + EditText.getText()  + "\n";
            Output.write(outputdata.getBytes());
            Output.close();

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    // [END parse_dataset]

    /**
     * Deletes a {@link DataSet} from the History API. In this example, we delete all step count data
     * for the past 24 hours.
     */
    void deleteData() {
        Log.i(TAG, "Deleting today's step count data.");
        txvResult.append("Deleting today's step count data.");

        // [START delete_dataset]
        // Set a start and end time for our data, using a start time of 1 day before this moment.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        long startTime = cal.getTimeInMillis();

        //  Create a delete request object, providing a data type and a time interval
        DataDeleteRequest request =
                new DataDeleteRequest.Builder()
                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                        .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .build();

        // Invoke the History API with the HistoryClient object and delete request, and then
        // specify a callback that will check the result.
        Fitness.getHistoryClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .deleteData(request)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.i(TAG, "Successfully deleted today's step count data.");
                                    txvResult.append("Successfully deleted today's step count data.");
                                } else {
                                    Log.e(TAG, "Failed to delete today's step count data.", task.getException());
                                    txvResult.append("Failed to delete today's step count data." + task.getException().toString());
                                }
                            }
                        });
    }

    /**
     * Updates and reads data by chaning {@link Task} from {@link #updateData()} and {@link
     * #readHistoryData()}.
     */
    void updateAndReadData() {
        updateData()
                .continueWithTask(
                        new Continuation<Void, Task<DataReadResponse>>() {
                            @Override
                            public Task<DataReadResponse> then(@NonNull Task<Void> task) throws Exception {
                                return readHistoryData();
                            }
                        });
    }

    /**
     * Creates a {@link DataSet},then makes a {@link DataUpdateRequest} to update step data. Then
     * invokes the History API with the HistoryClient object and update request.
     */
    private Task<Void> updateData() {
        // Create a new dataset and update request.
        DataSet dataSet = updateFitnessData();
        long startTime = 0;
        long endTime = 0;

        // Get the start and end times from the dataset.
        for (DataPoint dataPoint : dataSet.getDataPoints()) {
            startTime = dataPoint.getStartTime(TimeUnit.MILLISECONDS);
            endTime = dataPoint.getEndTime(TimeUnit.MILLISECONDS);
        }

        // [START update_data_request]
        Log.i(TAG, "Updating the dataset in the History API.");
        txvResult.append("\nUpdating the dataset in the History API.");

        DataUpdateRequest request =
                new DataUpdateRequest.Builder()
                        .setDataSet(dataSet)
                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                        .build();

        // Invoke the History API to update data.
        return Fitness.getHistoryClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .updateData(request)
                .addOnCompleteListener(
                        new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // At this point the data has been updated and can be read.
                                    Log.i(TAG, "Data update was successful.");
                                    txvResult.append("\nData update was successful.");
                                } else {
                                    Log.e(TAG, "There was a problem updating the dataset.", task.getException());
                                    txvResult.append("\nThere was a problem updating the dataset." + task.getException().toString());
                                }
                            }
                        });
    }

    /** Creates and returns a {@link DataSet} of step count data to update. */
    private DataSet updateFitnessData() {
        Log.i(TAG, "Creating a new data update request.");
        txvResult.append("\nCreating a new data update request.");

        // [START build_update_data_request]
        // Set a start and end time for the data that fits within the time range
        // of the original insertion.
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        cal.add(Calendar.MINUTE, 0);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.MINUTE, -50);
        long startTime = cal.getTimeInMillis();

        // Create a data source
        DataSource dataSource =
                new DataSource.Builder()
                        .setAppPackageName(this.context)
                        .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                        .setStreamName(TAG + " - step count")
                        .setType(DataSource.TYPE_RAW)
                        .build();

        // Create a data set
        int stepCountDelta = 1000;
        DataSet dataSet = DataSet.create(dataSource);
        // For each data point, specify a start time, end time, and the data value -- in this case,
        // the number of new steps.
        DataPoint dataPoint =
                dataSet.createDataPoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        dataPoint.getValue(Field.FIELD_STEPS).setInt(stepCountDelta);
        dataSet.add(dataPoint);
        // [END build_update_data_request]

        return dataSet;
    }

    /** Clears all the logging message in the LogView. */
    void clearTextView() {
        txvResult.setText("");
    }

    /** Initializes a custom log class that outputs both to in-app targets and logcat. */
    private void initializeLogging() {
//        // Wraps Android's native log framework.
//        LogWrapper logWrapper = new LogWrapper();
//        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
//        Log.setLogNode(logWrapper);
//        // Filter strips out everything except the message text.
//        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
//        logWrapper.setNext(msgFilter);
//        // On screen logging via a customized TextView.
//        LogView logView = (LogView) this.context.findViewById(R.id.txvResult2);
//
//        // Fixing this lint error adds logic without benefit.
//        // noinspection AndroidLintDeprecation
//        logView.setTextAppearance(R.style.Log);
//
//        logView.setBackgroundColor(Color.WHITE);
//        msgFilter.setNext(logView);
        Log.i(TAG, "Ready.");
        txvResult.append("Ready.");
    }
}
