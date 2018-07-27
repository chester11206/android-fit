package com.example.chester11206.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


public class ViewFragment1 extends Fragment {

    Activity context;
    public static LinearLayout my_root;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context=getActivity();


        final View rootView = inflater.inflate(R.layout.fragment_view_fragment1, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.txt_label);
        textView.setText("ViewFragment1");
//        Button bt = (Button) rootView.findViewById(R.id.button1);
//        bt.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View view){
//
//                //new com.example.chester11206.testapp.SensorsAPI();
//                new com.example.chester11206.testapp.Sensors().start((MainActivity) getActivity());
//                //sensors.onCreate();
//                //create an Intent object
//                //Intent intent = new Intent(rootView.getContext(), SensorsAPI.class);
//                //add data to the Intent object
//                //intent.putExtra("name",);
//                //start the second activity
//                //((MainActivity) getActivity()).startActivity(intent);
//            }
//
//        });

        return rootView;


    }

    public void onStart(){
        super.onStart();


    }

}
