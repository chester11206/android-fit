package com.example.chester11206.testapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class ViewFragment3 extends Fragment {

    Activity context;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        context=getActivity();

        View rootView = inflater.inflate(R.layout.fragment_view_fragment3, container, false);
        TextView textView = (TextView) rootView.findViewById(R.id.txt_label);
        textView.setText("ViewFragment3");
        return rootView;
    }

    public void onStart(){
        super.onStart();
//        Button bt=(Button)context.findViewById(R.id.recordingbtn);
//        bt.setOnClickListener(new View.OnClickListener(){
//            public void onClick(View view){
//
//                //create an Intent object
//                Intent intent = new Intent(context, RecordingAPI.class);
//                //add data to the Intent object
//                //intent.putExtra("name",);
//                //start the second activity
//                ((MainActivity) getActivity()).startActivity(intent);
//            }
//
//        });
    }
}
