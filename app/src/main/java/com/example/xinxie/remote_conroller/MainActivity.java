package com.example.xinxie.remote_conroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;

import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;

import android.widget.Button;

import android.widget.Toast;

import com.example.xinxie.remote_conroller.util.Utility;
import com.example.xinxie.remote_conroller.view.TempControlView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    //private GestureDetector gestureDetector;


    private static final String TAG="MainActivity";
    private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备标志

    private final static int REQUEST_GPS_ON=2; //宏定义请求开启GPS标志

    private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号

    private InputStream is;    //输入流，用来接收蓝牙数据
    //private EditText edit0;    //发送数据输入句柄
    private String smsg = "";    //显示用数据缓存
    //public String filename=""; //用来保存存储的文件名
    private BluetoothDevice _device = null;     //蓝牙设备
    private BluetoothSocket _socket = null;      //蓝牙通信socket
    //boolean _discoveryFinished = false;
    private boolean bRun = true;
    private boolean bThread = false;  //接收数据线程运行状态标志位

    //private ActionBar actionBar;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String ifSwitch ;

    public SwipeRefreshLayout swipeRefresh;

    //主菜单按钮
    private Button navButton;

    //侧滑菜单控件
    public DrawerLayout drawerLayout;

   //private Button bu_stop;

    //当前城市
    //private TextView currentCity;

    //定位按钮
    private Button locate;


    //获取本地蓝牙适配器，即蓝牙设备
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();

    //private String cityName;

    //自定义温度显示控件
    private TempControlView tempControl;

    private WeatherFragment weatherFragment;

    private String weatherId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //界面初始化
        super.onCreate(savedInstanceState);
        //设置画面为主画面 main.xml
        setContentView(R.layout.main);
        Toolbar toolbar=(Toolbar) findViewById(R.id.toolbar);
        //将Toolbar标题栏中的标题内容设置为空
        toolbar.setTitle("");
        //使用ToolBar控件替代ActionBar控件
        setSupportActionBar(toolbar);

        weatherFragment=(WeatherFragment)getSupportFragmentManager().findFragmentById(R.id.weather_fragment);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        //currentCity = (TextView)findViewById(R.id.title_city);

        navButton = (Button) findViewById(R.id.nav_button);

        locate = (Button)findViewById(R.id.locate);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        tempControl = (TempControlView) findViewById(R.id.temp_control);
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
                Toast.makeText(MainActivity.this, temp + "°", Toast.LENGTH_SHORT).show();
            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //这里设置的方向应该跟下面xml文件里面的layout_gravity方向相同
                //从左往右滑出侧滑菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        locate.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

                //重置WeatherFragment中的标志位
                WeatherFragment.locaWeatherFlag=false;
            }
        });

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                //调用WeatherFragment中的requestWeather(weatherId)方法
                //重新请求天气信息，达到刷新的效果
                weatherFragment.requestWeather(weatherId);
            }
        });
        /*
        当使用AppCompatActivity或其他support包中的基类，那么获得ActionBar实例需要用另一个相应的方法，
        那就是getSupportActionBar(),ActionBar也要使用相应support包下的。
         */

        //actionBar = getSupportActionBar();
        //bu_stop=(Button)findViewById(R.id.stop);
        //currentCity=(TextView)findViewById(R.id.cityName);

        sp = getSharedPreferences("config", 0);
        editor = sp.edit();
        ifSwitch=sp.getString("switch",null);
        if(TextUtils.isEmpty(ifSwitch)) {
            editor.putString("switch", "on");
            editor.apply();
        }

        /*
        当Sdk版本大于23时,手动检查权限是否已经声明
        如果Sdk版本大于23,而并没有在AndroidManifest.xml文件中声明相关的权限，
        则弹出Toast提示用户
        */

        //判断当前手机Android操作系统对应的SDK版本是否大于23
        if(Build.VERSION.SDK_INT >= 23){
            //如果当前手机Android操作系统对应的SDK版本大于23
            //则进行下面的权限判断
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Utility.ShowShortToast("请声明相关的权限！");
                return;
            }
        }

        //如果用户手机中GPS定位和网络定位都没开启
        //则弹出询问对话框，询问用户是否开启定位服务
//        if(!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
//                &&!(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))){
//            //调用ShowGPSDialog()方法弹出询问GPS是否开启的对话框
//            ShowGPSDialog();
//        }

        //开启定位，获取当前的位置信息
        //startLocate();

        //如果打开本地蓝牙设备不成功，提示信息，结束程序
        if (_bluetooth == null){
            Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！",Toast.LENGTH_LONG).show();
            finish();//结束当前Activity
            return;
        }

        // 设置设备可以被搜索
        new Thread(){
            public void run()
            {
                //判断蓝牙是否已经打开
                if(_bluetooth.isEnabled()==false){
                    _bluetooth.enable();//打开蓝牙
                }
            }
        }.start();


        Log.e("描述：", TAG+"........onCreate........");
    }


    /**
     * 暂停按钮点击事件
     * @param view
     */

    public void stop(View view){

        if(sp.getBoolean("isConnected", false)==true)
        {
            if(sp.getString("switch", null).equals("on"))
            {
                sendCommand("7");
                editor.putString("switch", "off");
                editor.commit();
                //bu_stop.setBackgroundResource(R.drawable.start);
            }
            else
            {
                sendCommand("8");
                editor.putString("switch", "on");
                editor.commit();
                //bu_stop.setBackgroundResource(R.drawable.stop);
            }
        }else
        {

            Toast.makeText(getApplicationContext(), "请与服务端连接", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 左转按钮的点击事件
     */
    public void turn_left(View v)
    {

        if(sp.getBoolean("isConnected", false)==true)
            sendCommand("1");
        else
            Toast.makeText(getApplicationContext(), "请与服务端连接", Toast.LENGTH_SHORT).show();
    }


    /**
     * 创建菜单时调用此方法
     * @param menu
     * @return
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * 处理菜单项点击事件的方法
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.quit:
                editor.putBoolean("isConnected", false);
                editor.commit();
                finish();//结束当前Activity
                break;
//            case R.id.pin_stay:
//                item.setIcon(R.drawable.pin_goaway);
//                //actionBar.hide();
//                break;
            case R.id.home:   //用户按下Home键
                editor.putBoolean("isConnected", false);
                editor.apply();
                finish();//结束当前Activity
                break;
            case R.id.about:
                Intent intent = new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intent);//跳转至AboutActivity
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 利用蓝牙发送数据
     * 通过OutputStream 输出数据
     * @param command
     */
    public void sendCommand(String command){
        int i;
        int n = 0;
        if(_socket!=null) {
            try {
                OutputStream os = _socket.getOutputStream();
                //将String字符串转换为字节数组byte[]
                byte[] bos = command.getBytes();
                for(i=0;i<bos.length;i++){
                    if(bos[i]==0x0a)
                        n++;
                }
                byte[] bos_new = new byte[bos.length+n];
                n=0;
                for(i=0;i<bos.length;i++){ //手机中换行为0a,将其改为0d 0a后再发送
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
                Toast.makeText(getApplicationContext(), "发送数据成功！", Toast.LENGTH_SHORT).show();

            } catch (IOException e) {

                Toast.makeText(getApplicationContext(), "发送数据失败！", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "请与服务端连接", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 接收活动结果，响应startActivityForResult()
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
                // 响应返回结果
                if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
                    // MAC地址，由DeviceListActivity设置返回
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // 通过本地蓝牙设备得到远程蓝牙设备
                    _device = _bluetooth.getRemoteDevice(address);

                    try{
                        //根据UUID 创建并返回一个BluetoothSocket
                        _socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
                    }catch(IOException e){
                        Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                    }

                    Button btn = (Button)findViewById(R.id.Button03);
                    try{

                        //与远程服务端socket建立连接
                        _socket.connect();
                        Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
                        editor.putBoolean("isConnected", true);
                        editor.apply();
                        btn.setText("断开");

                    }catch(IOException e){

                        try{
                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                            _socket.close();
                            _socket = null;

                        }catch(IOException e1){

                            Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    //打开接收线程
                    try{
                        //得到蓝牙数据输入流
                        is = _socket.getInputStream();
                    }catch(IOException e){
                        Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(bThread==false){
                        ReadThread.start();
                        bThread=true;
                    }else{
                        bRun = true;
                    }
                }
                break;
            case REQUEST_GPS_ON:
                Utility.ShowShortToast("设置GPS完成！");
                break;
            default:break;
        }
    }


    //接收数据线程
    Thread ReadThread=new Thread(){
        public void run(){
            int num;
            byte[] buffer = new byte[1024];
            byte[] buffer_new = new byte[1024];
            int i;
            int n;
            bRun = true;
            //接收线程
            while(true){
                try{

                    //is.available()获得远程服务端响应的数据的字节数
                    //如果响应的字节数为0，即远程服务端数据已经发送完毕
                    while(is.available()==0){
                        while(bRun == false){

                        }
                    }

                    while(true){
                        num = is.read(buffer);         //读入数据
                        n=0;
                        //String s0 = new String(buffer,0,num);
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
                        smsg+=s;   //写入接收缓存
                        if(is.available()==0)
                            break;  //短时间没有数据才跳出进行显示
                    }
                }catch(IOException e){

                    e.printStackTrace();
                }
            }
        }
    };

    //消息处理队列
    Handler handler= new Handler(){
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            //tv_nowAngle.setText(smsg);
            if(smsg.length()==2){
                smsg+="";
            }
        }
    };

    //关闭程序掉用处理部分
    public void onDestroy(){
        super.onDestroy();
        if(_socket!=null)  //关闭连接socket
            try{
                _socket.close();
            }catch(IOException e){
                e.printStackTrace();
            }
        _bluetooth.disable();  //关闭蓝牙服务

    }


    /**
     * 连接按钮的按键响应函数
     * @param v
     */
    public void onConnectButtonClicked(View v){
        if(_bluetooth.isEnabled()==false){  //如果蓝牙服务不可用则提示
            Toast.makeText(getApplicationContext(),"打开蓝牙中...",Toast.LENGTH_LONG).show();
            return;
        }

        //如未连接设备则打开DeviceListActivity进行设备搜索
        Button btn = (Button)findViewById(R.id.Button03);
        if(_socket==null){
            Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
        }
        else{
            btn.setBackgroundResource(R.drawable.switch_off);
            //关闭连接socket
            try{
                //关闭资源
                is.close();//关闭输入流
                _socket.close();//关闭socket
                _socket = null;
                bRun = false;
                btn.setText("连接");
            }catch(IOException e){
                e.printStackTrace();
            }
            editor.putBoolean("isConnected", false);
            editor.apply();
        }
        return;
    }


    /**
     * 弹出询问GPS是否开启的对话框
     */
    private void ShowGPSDialog(){

        //弹出对话框
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("提醒：");
        dialog.setMessage("为了更好的为您服务,请您打开您的GPS!");

        //dialog弹出后会点击屏幕或物理返回键，dialog不消失
        dialog.setCancelable(false);
        //界面上左边按钮，及其监听
        dialog.setNeutralButton("确定",
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        // 转到手机设置界面，用户设置GPS
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, REQUEST_GPS_ON); // 设置完成后返回到原来的界面

                    }
                });
        //界面上右边按钮，及其监听
        dialog.setPositiveButton("取消", new android.content.DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.dismiss();//关闭对话框
            }
        } );
        dialog.show();

    }










}
