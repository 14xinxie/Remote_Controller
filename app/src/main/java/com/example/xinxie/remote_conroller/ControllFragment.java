package com.example.xinxie.remote_conroller;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.xinxie.remote_conroller.util.MyApplication;
import com.example.xinxie.remote_conroller.view.TempControlView;

/**
 * Created by 14292 on 2017-11-27.
 */
public class ControllFragment extends Fragment {

    private String TAG="ControllFragment";

    //自定义温度显示控件
    private TempControlView tempControl;

    //Fragment中的view
    private View view;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.controll_fragment,container,false);


        tempControl = (TempControlView)view.findViewById(R.id.temp_control);
        Log.d(TAG,"onCreateView...");

        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);




        // 设置三格代表温度1度
        tempControl.setAngleRate(3);

        //设置温度表中的最大温度,最小温度和当前温度
        tempControl.setTemp(15, 40, 15);

        tempControl.setOnTempChangeListener(new TempControlView.OnTempChangeListener() {
            @Override
            public void change(int temp) {
                //Toast.makeText(MainActivity.this, temp + "°", Toast.LENGTH_SHORT).show();
            }
        });

        tempControl.setOnClickListener(new TempControlView.OnClickListener() {
            @Override
            public void onClick(int temp) {
                Toast.makeText(MyApplication.getContext(), temp + "°", Toast.LENGTH_SHORT).show();
            }
        });


        Log.d(TAG,"onActivityCreated...");
    }
}
