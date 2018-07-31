package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fit.samples.common.logger.Log;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    public Activity context = this;

    private Sensors sensorsapi;
    private History historyapi;
    private Recording recordingapi;

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private View sensorsView, histroyView, recordingView;
    public static int lastPosition = 0;

    boolean[] flags = new boolean[]{false,false,false};//init multichoice = false
    String[] items = null;
    private static List<String> sensors_list = new ArrayList<String>();

    public static final String TAG = "MyFit";
    TextView txvResult;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /** init three api */
        sensorsapi = new com.example.chester11206.testapp.Sensors();
        historyapi = new com.example.chester11206.testapp.History();
        recordingapi = new com.example.chester11206.testapp.Recording();

        /** set viewpage and layout */
        mViewPager = (ViewPager) findViewById(R.id.container);
        setupViewPager(mViewPager);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageScrollStateChanged(int state) {}
            @Override
            public void onPageSelected(int position) {
                lastPosition = position;
                switch (position) {
                    case 2:
                        Button historybtn = (Button) findViewById(R.id.historybtn);
                        historybtn.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View view){
                                historyapi.start(context);
                            }

                        });
                    case 3:
                        Button recordingbtn = (Button) findViewById(R.id.recordingbtn);
                        recordingbtn.setOnClickListener(new View.OnClickListener(){
                            public void onClick(View view){
                                recordingapi.start(context);
                            }

                        });
                }
            }
        });
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        /** set multichoice dialog */
        items = getResources().getStringArray(R.array.sensors);


        /** Log in */
        LogIn();
    }

/** General login */
    private void LogIn(){

        // When permissions are revoked the app is restarted so onCreate is sufficient to check for
        // permissions core to the Activity's functionality.
        if (hasRuntimePermissions()) {
            if (!hasOAuthPermission()) {
                requestOAuthPermission();
            }
        }
        else{
            requestRuntimePermissions();
        }

    }

/** Activity Result */
    @Override
    protected void onResume() {
        super.onResume();

        // This ensures that if the user denies the permissions then uses Settings to re-enable
        // them, the app will start working.
        LogIn();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                LogIn();
            }
        }
    }

/** Account Permission */
    /** Checks if user's account has OAuth permission to Fitness API. */
    private boolean hasOAuthPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }

    /** Launches the Google SignIn activity to request OAuth permission for the user. */
    private void requestOAuthPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();


        GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                fitnessOptions);

    }

    /** Gets the {@link FitnessOptions} in order to check or request OAuth permission for the user. */
    private FitnessOptions getFitnessSignInOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_LOCATION_SAMPLE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_ACTIVITY_SAMPLES)
                .build();
    }

    /** Returns the current state of the permissions needed. */
    private boolean hasRuntimePermissions() {
        int permissionState =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    private void requestRuntimePermissions() {
        boolean shouldProvideRationale =
                ActivityCompat.shouldShowRequestPermissionRationale(
                        this, Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.sensors_view),
                    R.string.permission_rationale,
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction(
                            R.string.ok,
                            new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    // Request permission
                                    ActivityCompat.requestPermissions(
                                            context,
                                            new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                                            REQUEST_PERMISSIONS_REQUEST_CODE);
                                }
                            })
                    .show();
        } else {
            Log.i(TAG, "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                    context,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

/** Viewpage Setting */
    private void setupViewPager(ViewPager viewPager) {

        MyViewPagerAdapter adapter = new MyViewPagerAdapter();
        LayoutInflater inflater=getLayoutInflater();
        sensorsView = inflater.inflate(R.layout.sensors_view, null);
        histroyView = inflater.inflate(R.layout.history_view, null);
        recordingView = inflater.inflate(R.layout.recording_view, null);
        adapter.add(sensorsView, "Sensors");
        adapter.add(histroyView, "History");
        adapter.add(recordingView, "Recording");

        viewPager.setAdapter(adapter);
    }

    public class MyViewPagerAdapter extends PagerAdapter {
        private final List<View> mListViews = new ArrayList<>();
        private final List<String> mListTitles = new ArrayList<>();

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) 	{
            container.removeView(mListViews.get(position));
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mListViews.get(position), 0);
            Button sensorsbtn = (Button) findViewById(R.id.sensorsbtn);
            sensorsbtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    txvResult = (TextView) findViewById(R.id.txvResult1);
                    txvResult.setMovementMethod(new ScrollingMovementMethod());
                    if (sensors_list.size() > 0){
                        txvResult.setText("");
                        sensorsapi.start(context, sensors_list);
                    }
                    else {
                        txvResult.setText("You haven't choose the sensors!");
                    }

                }
            });
            return mListViews.get(position);
        }

        @Override
        public int getCount() {
            return  mListViews.size();
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0==arg1;
        }

        public void add(View view, String title) {
            mListViews.add(view);
            mListTitles.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mListTitles.get(position);
        }
    }

/** menu setting */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        switch (mViewPager.getCurrentItem()) {
            case 0:
                getMenuInflater().inflate(R.menu.sensors_menu, menu);
                break;
            case 1:
                getMenuInflater().inflate(R.menu.history_menu, menu);
                break;
            case 2:
                getMenuInflater().inflate(R.menu.recording_menu, menu);
                break;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        invalidateOptionsMenu();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            showDialog(1);
        }
        else if (id == R.id.action_unregister_listener) {
            sensorsapi.unregisterFitnessDataListener();
            return true;
        }
        else if (id == R.id.action_delete_data) {
            historyapi.deleteData();
            return true;
        } else if (id == R.id.action_update_data) {
            historyapi.clearTextView();
            historyapi.updateAndReadData();
        }
        else if (id == R.id.action_cancel_subs) {
            recordingapi.cancelSubscription();
            return true;
        } else if (id == R.id.action_dump_subs) {
            recordingapi.dumpSubscriptionsList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

/** create dialog by id */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        final boolean[] tempFlags = flags.clone();
        switch (id) {
            case 1:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose Sensors");
                builder.setMultiChoiceItems(items, flags, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        tempFlags[which] = isChecked;
                    }
                });
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean hasChoose = false;
                        for (boolean flag : tempFlags) {
                            if (flag) {
                                hasChoose = true;
                                break;
                            }
                        }
                        if (hasChoose){
                            List<String> result = new ArrayList<String>();
                            flags = tempFlags.clone();
                            for (int i = 0; i < flags.length; i++) {
                                if(flags[i])
                                {
                                    result.add(items[i]);
                                }
                            }
                            sensors_list = new ArrayList<String>(result);
                        }
                        else {
                            txvResult = (TextView) findViewById(R.id.txvResult1);
                            txvResult.setMovementMethod(new ScrollingMovementMethod());
                            txvResult.setText("");
                            txvResult.setText("You haven't choose the sensors!");
                        }
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog = builder.create();
                break;

            default:
                break;
        }
        return dialog;
    }
}
