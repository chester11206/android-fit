package com.example.chester11206.testapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Subscription;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.List;

public class Recording {
    public static final String TAG = "BasicRecordingApi";

    TextView txvResult;

    public Activity context;

    public void start(Activity activity){

        this.context = activity;

        txvResult = (TextView) this.context.findViewById(R.id.txvResult3);
        txvResult.setMovementMethod(new ScrollingMovementMethod());

        initializeLogging();
        subscribe();
    }

    /**
     * Subscribes to an available {@link DataType}. Subscriptions can exist across application
     * instances (so data is recorded even after the application closes down).  When creating
     * a new subscription, it may already exist from a previous invocation of this app.  If
     * the subscription already exists, the method is a no-op.  However, you can check this with
     * a special success code.
     */
    public void subscribe() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        // [START subscribe_to_datatype]
        Fitness.getRecordingClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .subscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully subscribed!");
                        txvResult.append("Successfully subscribed!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.i(TAG, "There was a problem subscribing.");
                        txvResult.append("There was a problem subscribing.");
                    }
                });
        // [END subscribe_to_datatype]
    }

    /**
     * Fetches a list of all active subscriptions and log it. Since the logger for this sample
     * also prints to the screen, we can see what is happening in this way.
     */
    void dumpSubscriptionsList() {
        // [START list_current_subscriptions]
        Fitness.getRecordingClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .listSubscriptions(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<List<Subscription>>() {
                    @Override
                    public void onSuccess(List<Subscription> subscriptions) {
                        for (Subscription sc : subscriptions) {
                            DataType dt = sc.getDataType();
                            Log.i(TAG, "Active subscription for data type: " + dt.getName());
                            txvResult.append("Active subscription for data type: " + dt.getName());
                        }
                    }
                });
        // [END list_current_subscriptions]
    }

    /**
     * Cancels the ACTIVITY_SAMPLE subscription by calling unsubscribe on that {@link DataType}.
     */
    void cancelSubscription() {
        final String dataTypeStr = DataType.TYPE_ACTIVITY_SAMPLES.toString();
        Log.i(TAG, "Unsubscribing from data type: " + dataTypeStr);

        // Invoke the Recording API to unsubscribe from the data type and specify a callback that
        // will check the result.
        // [START unsubscribe_from_datatype]
        Fitness.getRecordingClient(this.context, GoogleSignIn.getLastSignedInAccount(this.context))
                .unsubscribe(DataType.TYPE_ACTIVITY_SAMPLES)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.i(TAG, "Successfully unsubscribed for data type: " + dataTypeStr);
                        txvResult.append("Successfully unsubscribed for data type: " + dataTypeStr);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Subscription not removed
                        Log.i(TAG, "Failed to unsubscribe for data type: " + dataTypeStr);
                        txvResult.append("Failed to unsubscribe for data type: " + dataTypeStr);
                    }
                });
        // [END unsubscribe_from_datatype]
    }

    /**
     *  Initializes a custom log class that outputs both to in-app targets and logcat.
     */
    private void initializeLogging() {
//        // Wraps Android's native log framework.
//        LogWrapper logWrapper = new LogWrapper();
//        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
//        Log.setLogNode(logWrapper);
//        // Filter strips out everything except the message text.
//        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
//        logWrapper.setNext(msgFilter);
//        // On screen logging via a customized TextView.
//        LogView logView = (LogView) this.context.findViewById(R.id.txvResult3);
//
//        // Fixing this lint error adds logic without benefit.
//        //noinspection AndroidLintDeprecation
//        logView.setTextAppearance(this.context, R.style.Log);
//
//        logView.setBackgroundColor(Color.WHITE);
//        msgFilter.setNext(logView);
        Log.i(TAG, "Ready");
        txvResult.append("Ready");
    }
}
