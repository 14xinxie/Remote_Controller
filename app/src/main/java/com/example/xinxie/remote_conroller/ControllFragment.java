package com.example.xinxie.remote_conroller;


import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.xinxie.remote_conroller.util.PromptUtil;
import com.example.xinxie.remote_conroller.view.TempControlView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by 14292 on 2017-11-27.
 */
public class ControllFragment extends Fragment {


    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备标志

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    private boolean readRun=false;//接收数据线程运行标志位

    private Handler receiveHandler;//处理接收数据的handler

    //自定义温度显示控件
    private TempControlView tempControl;

    //Fragment中的view
    private View view;

    //连接按钮
    private Button btn_connect;

    //加速按钮
    private Button btn_speed;

    //减速按钮
    private Button btn_slow;

    //开关按钮
    private Button btn_switch;
    private InputStream is;    //输入流，用来接收蓝牙数据

    //获取本地蓝牙适配器，即蓝牙设备
    private BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
    private BluetoothDevice device = null;     //蓝牙设备
    private BluetoothSocket socket = null;      //蓝牙通信socket

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String ifSwitch ;



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.controll_fragment,container,false);
        btn_connect=(Button)view.findViewById(R.id.connect_button);

        btn_speed =(Button)view.findViewById(R.id.speed_button);

        btn_slow =(Button)view.findViewById(R.id.slow_button);

        btn_switch=(Button)view.findViewById(R.id.switch_button);

        tempControl = (TempControlView)view.findViewById(R.id.temp_control);

        return view;
    }

    @Override
    public void onActivityCreated( Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sp = getActivity().getSharedPreferences("config", 0);
        editor = sp.edit();

        //获取标志位
        ifSwitch = sp.getString("switch", null);
        if (TextUtils.isEmpty(ifSwitch)) {
            editor.putString("switch", "on");
            editor.apply();
        }

        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (bluetooth == null) {
            PromptUtil.showShortToast("无法打开手机蓝牙，请确认手机是否有蓝牙功能！");
            getActivity().finish();//结束当前Activity
            return;
        }

        // 设置设备可以被搜索
        new Thread() {
            public void run() {
                //判断蓝牙是否已经打开
                if (bluetooth.isEnabled() == false) {
                    bluetooth.enable();//打开蓝牙
                }
            }
        }.start();

        // 设置三格代表温度1度
        tempControl.setAngleRate(3);

        //设置温度表中的最大温度,最小温度和当前温度(初始默认显示最小温度)

        tempControl.setTemp(10, 40, 10);

        tempControl.setOnTempChangeListener(new TempControlView.OnTempChangeListener() {
            @Override
            public void change(int temp) {

                //PromptUtil.showShortToast("温度改变中...");
            }
        });

        tempControl.setOnClickListener(new TempControlView.OnClickListener() {
            @Override
            public void onClick(int temp) {

                //PromptUtil.showShortToast(temp + "°");
            }
        });


        /**
         * 连接按钮的按键响应函数
         * @param v
         */
        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (bluetooth.isEnabled() == false) {  //如果蓝牙服务不可用则提示

                    PromptUtil.showShortToast("打开蓝牙中...");
                    return;
                }

                if (socket==null) {
                    Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class); //跳转程序设置
                    startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
                } else {


                    //关闭连接socket
                    try {
                        //关闭资源
                        is.close();//关闭输入流
                        socket.close();//关闭socket
                        socket=null;    //socket赋值为null,否则不能进行第二次蓝牙连接
                        readRun = false;//停止接收数据线程
                        btn_connect.setBackgroundResource(R.drawable.connect_on);
                        btn_connect.setText("连接");

                        btn_switch.setBackgroundResource(R.drawable.switch_on);

                        editor.putString("switch","on");

                        editor.putBoolean("isConnected", false);
                        editor.apply();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                return;
            }
        });


        /**
         * 开关按钮的点击事件
         */
        btn_switch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(sp.getBoolean("isConnected", false)==true){
                    if(sp.getString("switch", null).equals("on")){

                        editor.putString("switch", "off");
                        editor.apply();
                        btn_switch.setBackgroundResource(R.drawable.switch_off);
                        sendCommand("1");//发送"1"代表开启手动模式
                        PromptUtil.showShortToast("已开启手动模式");
                    }else{
                        editor.putString("switch", "on");
                        editor.apply();
                        btn_switch.setBackgroundResource(R.drawable.switch_on);
                        sendCommand("0");//发送"0"代表开启温控模式
                        PromptUtil.showShortToast("已开启温控模式");
                    }
                }else{

                    PromptUtil.showShortToast("请与服务端连接");
                }

            }
        });

        /**
         * 减速按钮点击事件
         */
        btn_slow.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(sp.getBoolean("isConnected", false)==true){
                    if(sp.getString("switch", null).equals("on")){

                        PromptUtil.showShortToast("请开启手动模式");

                    }else{

                        sendCommand("d");//发送"d"代表减速
                    }
                }else{

                    PromptUtil.showShortToast("请与服务端连接");
                }

            }
        });

        btn_speed.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                if(sp.getBoolean("isConnected", false)==true){
                    if(sp.getString("switch", null).equals("on")){

                        PromptUtil.showShortToast("请开启手动模式");

                    }else{

                        sendCommand("u");//发送"u"代表加速
                    }
                }else{

                    PromptUtil.showShortToast("请与服务端连接");
                }
            }
        });

        /**
         * 处理接收单片机蓝牙模块发送过来的数据
         */
        receiveHandler= new Handler(){
            public void handleMessage(Message msg){
                super.handleMessage(msg);

                //获取单片机端发送过来的温度值
                //将字符串转为浮点型，
                float value=Float.parseFloat(msg.obj.toString());
                //然后再强制转换为整型
                int temp=(int)value;

                //改变自定义温度控件上显示的温度
                //使其显示温度传感器的实时温度
                tempControl.setTemp(10, 40, temp);

                //PromptUtil.showShortToast("接收到的数据为："+msg.obj.toString());


            }
        };

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 通过本地蓝牙设备得到远程蓝牙设备
                    device = bluetooth.getRemoteDevice(address);

                    try {
                        //根据UUID 创建并返回一个BluetoothSocket
                        socket = device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    } catch (IOException e) {
                        PromptUtil.showShortToast("连接失败！");
                    }

                    try {

                        //与远程服务端socket建立连接
                        socket.connect();
                        PromptUtil.showShortToast("连接" + device.getName() + "成功！");
                        editor.putBoolean("isConnected", true);
                        editor.apply();
                        btn_connect.setText("断开");
                        btn_connect.setBackgroundResource(R.drawable.connect_off);

                    } catch (IOException e) {

                        try {

                            PromptUtil.showShortToast("连接失败！");
                            socket.close();

                        } catch (IOException e1) {

                            PromptUtil.showShortToast("连接失败！");
                        }
                        return;
                    }

                    //打开接收线程
                    try {
                        //得到蓝牙数据输入流
                        is = socket.getInputStream();
                    } catch (IOException e) {

                        PromptUtil.showShortToast("接收数据失败！");
                        return;
                    }

                    //如果接收数据线程未开启，则开启接收数据线程，
                    //并将接收数据线程标志位置为true
                    if(readRun==false){
                        readRun=true;   //置接收数据线程标志位为true
                        new ReadThread().start(); //创建线程实例，启动接收数据线程

                    }

                }
                break;
            default:
                break;
        }

    }


    //初始化读取数据线程
    class ReadThread extends Thread{

        /**
         * 线程的run()方法只会执行一次
         * 因此要想线程一直运行，就得在run()方法中加一个while()循环
         */
        public void run(){
            int num;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i ;
            int n ;
            //接收线程
            while(readRun){
                try{
                    num = is.read(buffer);         //读入数据
                    n=0;

                    //将服务器端发送过来的换行0x0d0a转换为手机识别的换行0a
                    //其中0x0d0a和0a均为字符的十六进制数表示
                    for(i=0;i<num;i++){
                        if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
                            buffer_new[n] = 0x0a;
                            i++;
                        }else{
                            buffer_new[n] = buffer[i];
                        }
                        n++;
                    }
                    String s = new String(buffer_new,0,n);

                    //如果短时间没有数据，
                    //则利用Handler发送消息通知UI线程显示接收的数据
                    if(s.length()==4){

                        //需要数据传递，用下面方法；
                        Message msg = new Message();
                        msg.obj = s;//可以是基本类型，可以是对象，可以是List、map等；
                        receiveHandler.sendMessage(msg);
                    }

                }catch(IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 利用蓝牙发送数据
     * 通过OutputStream 输出数据
     * @param command
     */
    private void sendCommand(String command){
        int i;
        int n = 0;
        if(socket!=null) {
            try {
                OutputStream os = socket.getOutputStream();
                //将String字符串转换为字节数组byte[]
                byte[] bos = command.getBytes();
                for(i=0;i<bos.length;i++){
                    if(bos[i]==0x0a)
                        n++;
                }
                byte[] bos_new = new byte[bos.length+n];
                n=0;
                for(i=0;i<bos.length;i++){ //手机中换行为0a,将其改为0d0a后再发送
                    if(bos[i]==0x0a){
                        bos_new[n]=0x0d;
                        n++;
                        bos_new[n]=0x0a;
                    }else{
                        bos_new[n]=bos[i];
                    }
                    n++;
                }

                //利用OutputStream的write()方法发送数据
                os.write(bos_new);

                PromptUtil.showShortToast("发送数据"+command+"成功！");

            } catch (IOException e) {

                PromptUtil.showShortToast("发送数据"+command+"失败！");
                e.printStackTrace();
            }
        }
        else {
            PromptUtil.showShortToast("请与服务端连接");
        }
    }


    /**
     * Fragment销毁时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if(socket!=null) { //关闭连接socket
            try{

                //当线程标志位置为false时，线程的while循环结束了，
                //即线程的run()方法结束了，此时线程实例已不存在了，
                //不可以使用start()方法启动线程，只能重新创建一个线程实例来启动线程
                readRun=false;//停止接收数据线程

                //退出时将标志位设置为初始值
                btn_connect.setBackgroundResource(R.drawable.connect_on);
                btn_connect.setText("连接");
                btn_switch.setBackgroundResource(R.drawable.switch_on);
                editor.putString("switch","on");

                editor.putBoolean("isConnected", false);
                editor.apply();

                is.close();
                socket.close();
                is=null;
                socket=null;

            }catch(IOException e){
                e.printStackTrace();
            }
        }
        bluetooth.disable();  //关闭蓝牙服务

    }
}
