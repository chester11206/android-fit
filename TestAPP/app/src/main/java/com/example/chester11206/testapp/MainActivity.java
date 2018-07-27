package com.example.chester11206.testapp;

import android.Manifest;
import android.app.Activity;
import android.app.LocalActivityManager;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private ViewPager mViewPager;
    private TabLayout mTabLayout;
    private List<View> views;
    private View view1,view2,view3;
    public static int lastPosition = 0;
    public Activity context = this;
    private OkHttpClient mOkHttpClient;
    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("text/x-markdown; charset=utf-8");

    public static final String TAG = "MyFit";

    private static final int REQUEST_OAUTH_REQUEST_CODE = 1;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mViewPager = (ViewPager) findViewById(R.id.container);

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
            @Override
            public void onPageSelected(int position) {
                lastPosition = position;
            }
            @Override
            public void onPageScrollStateChanged(int state) {}
        });

        setupViewPager(mViewPager);

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
        mTabLayout.setupWithViewPager(mViewPager);

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


    private void setupViewPager(ViewPager viewPager) {
//        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
//        adapter.addFragment(new ViewFragment1(), "Sensors");
//        adapter.addFragment(new ViewFragment2(), "History");
//        adapter.addFragment(new ViewFragment3(), "Recording");

        MyViewPagerAdapter adapter = new MyViewPagerAdapter();
        LayoutInflater inflater=getLayoutInflater();
        view1=inflater.inflate(R.layout.fragment_view_fragment1, null);
        view2=inflater.inflate(R.layout.fragment_view_fragment2, null);
        view3=inflater.inflate(R.layout.fragment_view_fragment3, null);
        adapter.add(view1, "Sensors");
        adapter.add(view2, "History");
        adapter.add(view3, "Recording");

        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

    public class MyViewPagerAdapter extends PagerAdapter {
        private final List<View> mListViews = new ArrayList<>();;
        private final List<String> mListTitles = new ArrayList<>();;

//        public MyViewPagerAdapter(List<View> mListViews) {
//            this.mListViews = mListViews;
//        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) 	{
            container.removeView(mListViews.get(position));
        }


        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Log.d(TAG, String.valueOf(position));
            container.addView(mListViews.get(position), 0);
            if(position < 2) {
                Button sensorsbtn = (Button) findViewById(R.id.sensorsbtn);
                sensorsbtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View view) {
                        new com.example.chester11206.testapp.Sensors().start(context);

                    }

                });
            }
            else if (position == 2){
                Button historybtn = (Button) findViewById(R.id.historybtn);
                historybtn.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View view){
                        new com.example.chester11206.testapp.History().start(context);
                    }

                });
            }
            else {
                Button recordingbtn = (Button) findViewById(R.id.recordingbtn);
                recordingbtn.setOnClickListener(new View.OnClickListener(){
                    public void onClick(View view){
                        new com.example.chester11206.testapp.Recording().start(context);
                    }

                });
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
}
