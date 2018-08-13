package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LocalActivityManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
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
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.text.DateFormat.getDateTimeInstance;

public class MainActivity extends AppCompatActivity {

    public Activity context = this;

    private Sensors sensorsapi;
    private History historyapi;
    private Recording recordingapi;

    private MultiSensors multiSensorsapi;
    private SensorManager mSensorManager;

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private View sensorsView, histroyView, recordingView, multiSensorsView;
    public static int lastPosition = 0;

    boolean[] flags = new boolean[]{};//init multichoice = false
    String[] sensorItems = null;
    private List<String> sensors_list = new ArrayList<String>();
    public static final Map<String, DataType> datatype_map = createDataTypeMap();
    private static Map<String, DataType> createDataTypeMap()
    {
        Map<String, DataType> myMap = new HashMap<String, DataType>();
        myMap.put("Activity", DataType.TYPE_ACTIVITY_SAMPLES);
        myMap.put("Step", DataType.TYPE_STEP_COUNT_DELTA);
        myMap.put("Distance", DataType.TYPE_DISTANCE_DELTA);
        myMap.put("Location", DataType.TYPE_LOCATION_SAMPLE);
        myMap.put("Wheel RPM", DataType.TYPE_CYCLING_WHEEL_RPM);
        myMap.put("Wheel Revolution", DataType.TYPE_CYCLING_WHEEL_REVOLUTION);
        myMap.put("Speed", DataType.TYPE_SPEED);
        myMap.put("Weight", DataType.TYPE_WEIGHT);
        myMap.put("Nutrition", DataType.TYPE_NUTRITION);
        myMap.put("Heart Rate BPM", DataType.TYPE_HEART_RATE_BPM);
        myMap.put("Calories", DataType.TYPE_CALORIES_EXPENDED);
        return myMap;
    }

    public static final Map<String, Integer> activity_map = createActivityMap();
    private static Map<String, Integer> createActivityMap()
    {
        Map<String, Integer> myMap = new HashMap<String, Integer>();
        myMap.put("still", 0);
        myMap.put("walking", 1);
        myMap.put("running", 2);
        myMap.put("biking", 3);
        myMap.put("in vehicle", 4);
        myMap.put("on foot", 5);
        myMap.put("tilting", 6);
        myMap.put("unknown", 7);
        return myMap;
    }

    private static final int msgKey1 = 1;
    private TextView mTime;
    public static long timeNow = 0;

    public static final String TAG = "MyFit";
    TextView txvResult;

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTime = (TextView) findViewById(R.id.mytime);
        new TimeThread().start();

        /** init three api */
        sensorsapi = new com.example.chester11206.testapp.Sensors();
        historyapi = new com.example.chester11206.testapp.History();
        recordingapi = new com.example.chester11206.testapp.Recording();
        multiSensorsapi = new com.example.chester11206.testapp.MultiSensors();

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
                Log.i(TAG, "selected page = " + position);
                lastPosition = position;
//                switch (position) {
//                    case 2:
//                        Button historybtn = (Button) findViewById(R.id.historybtn);
//                        historybtn.setOnClickListener(new View.OnClickListener(){
//                            public void onClick(View view){
//                                historyapi.start(context);
//                            }
//
//                        });
//                        break;
//                    case 3:
//                        Button recordingbtn = (Button) findViewById(R.id.recordingbtn);
//                        recordingbtn.setOnClickListener(new View.OnClickListener(){
//                            public void onClick(View view){
//                                recordingapi.start(context);
//                            }
//
//                        });
//                        break;
//                    case 4:
//                        Button multisensorsbtn = (Button) findViewById(R.id.multisensorsbtn);
//                        multisensorsbtn.setOnClickListener(new View.OnClickListener() {
//                            public void onClick(View view) {
//                                txvResult = (TextView) findViewById(R.id.stepView);
//                                txvResult.setMovementMethod(new ScrollingMovementMethod());
//                                if (sensors_list.size() > 0){
//                                    txvResult.setText(sensors_list.toString());
//                                    mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//                                    multiSensorsapi.start(context, mSensorManager, sensors_list);
//                                }
//                                else {
//                                    txvResult.setText("You haven't choose the sensors!");
//                                }
//
//
//                            }
//                        });
//                        break;
//                }
            }
        });
        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

        /** set multichoice dialog */

        sensorItems = getResources().getStringArray(R.array.sensors);
        flags = new boolean[sensorItems.length];
        Arrays.fill(flags, false);


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

/** Show Now Time */
    public class TimeThread extends Thread {
        @Override
        public void run () {
            do {
                try {
                    Thread.sleep(1000);
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
                    getTime();
                    break;
                default:
                    break;
            }
        }
    };
    public void getTime(){
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long TimeNow = cal.getTimeInMillis();
        timeNow = TimeNow;
        DateFormat dateFormat = getDateTimeInstance();
        mTime.setText(dateFormat.format(TimeNow));
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
        FitnessOptions.Builder fit = FitnessOptions.builder();
        for (String key : datatype_map.keySet()) {
            fit.addDataType(datatype_map.get(key), FitnessOptions.ACCESS_WRITE);
            fit.addDataType(datatype_map.get(key), FitnessOptions.ACCESS_READ);
        }
        return
                fit.build();
//                FitnessOptions.builder()
//                .addDataType(DataType.TYPE_LOCATION_SAMPLE,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_LOCATION_SAMPLE,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_STEP_COUNT_DELTA,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_DISTANCE_DELTA,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_DISTANCE_DELTA,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_SPEED,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_SPEED,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_ACTIVITY_SAMPLES,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_ACTIVITY_SAMPLES,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_WEIGHT,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_WEIGHT,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_HEART_RATE_BPM,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_HEART_RATE_BPM,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_CYCLING_WHEEL_REVOLUTION,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_CYCLING_WHEEL_REVOLUTION,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_CYCLING_WHEEL_RPM,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_CYCLING_WHEEL_RPM,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_NUTRITION,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_NUTRITION,FitnessOptions.ACCESS_READ)
//                .addDataType(DataType.TYPE_CALORIES_EXPENDED,FitnessOptions.ACCESS_WRITE)
//                .addDataType(DataType.TYPE_CALORIES_EXPENDED,FitnessOptions.ACCESS_READ)
//                .build();
    }

    /** Returns the current state of the permissions needed. */
    private boolean hasRuntimePermissions() {
        int permissionLocation =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int permissionBody =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS);
        return (permissionLocation == PackageManager.PERMISSION_GRANTED)
                && (permissionBody == PackageManager.PERMISSION_GRANTED)
                ;
    }

    private void requestRuntimePermissions() {
        boolean permissionLocation =
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED);
        boolean permissionBody =
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS)
                        == PackageManager.PERMISSION_GRANTED);
        if (!permissionLocation) {
            if (!permissionBody) {
                ActivityCompat.requestPermissions(
                        context,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BODY_SENSORS},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
            else {
                ActivityCompat.requestPermissions(
                        context,
                        new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_PERMISSIONS_REQUEST_CODE);
            }
        }
        else if (!permissionBody) {
            ActivityCompat.requestPermissions(
                    context,
                    new String[] {Manifest.permission.BODY_SENSORS},
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
        multiSensorsView = inflater.inflate(R.layout.multisensors_view, null);
        adapter.add(sensorsView, "Sensors");
        adapter.add(histroyView, "History");
        adapter.add(recordingView, "Recording");
        adapter.add(multiSensorsView, "MultiSensors");

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
            switch (position) {
                case 0:
                    Button sensorsbtn = (Button) findViewById(R.id.sensorsbtn);
                    sensorsbtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            txvResult = (TextView) findViewById(R.id.txvResult1);
                            txvResult.setMovementMethod(new ScrollingMovementMethod());
                            if (sensors_list.size() > 0){
                                txvResult.setText(sensors_list.toString());
                                sensorsapi.start(context, sensors_list);
                            }
                            else {
                                txvResult.setText("You haven't choose the sensors!");
                            }

                        }
                    });
                    break;
                case 2:
                    Button historybtn = (Button) findViewById(R.id.historybtn);
                    historybtn.setOnClickListener(new View.OnClickListener(){
                        public void onClick(View view){
                            historyapi.start(context);
                        }

                    });
                    break;
                case 3:
                    Button recordingbtn = (Button) findViewById(R.id.recordingbtn);
                    recordingbtn.setOnClickListener(new View.OnClickListener(){
                        public void onClick(View view){
                            recordingapi.start(context);
                        }

                    });
                    break;
                case 4:
                    Button multisensorsbtn = (Button) findViewById(R.id.multisensorsbtn);
                    multisensorsbtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View view) {
                            txvResult = (TextView) findViewById(R.id.acceView);
                            txvResult.setMovementMethod(new ScrollingMovementMethod());
                            if (sensors_list.size() > 0){
                                txvResult.setText(sensors_list.toString());
                                mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                                multiSensorsapi.start(context, mSensorManager, sensors_list);
                            }
                            else {
                                txvResult.setText("You haven't choose the sensors!");
                            }


                        }
                    });
                    break;
            }

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
            case 3:
                getMenuInflater().inflate(R.menu.multisensors_menu, menu);
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

/** create dialog */
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        final boolean[] tempFlags = flags.clone();
        switch (id) {
            case 1:
                AlertDialog.Builder builderSensor = new AlertDialog.Builder(this);
                builderSensor.setTitle("Choose Sensors");
                builderSensor.setMultiChoiceItems(sensorItems, flags, new DialogInterface.OnMultiChoiceClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        tempFlags[which] = isChecked;
                    }
                });
                builderSensor.setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
                                    result.add(sensorItems[i]);
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
                builderSensor.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                dialog = builderSensor.create();
                break;

            default:
                break;
        }
        return dialog;
    }
}
