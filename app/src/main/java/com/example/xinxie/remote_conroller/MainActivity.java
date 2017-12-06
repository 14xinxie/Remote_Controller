package com.example.xinxie.remote_conroller;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PersistableBundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xinxie.remote_conroller.util.MyApplication;
import com.example.xinxie.remote_conroller.util.PromptUtil;
import com.example.xinxie.remote_conroller.view.TempControlView;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private String TAG="MainActivity";

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

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private String ifSwitch ;

    //自带下拉刷新功能的控件
    public SwipeRefreshLayout swipeRefresh;

    //主菜单按钮
    private Button navButton;

    //侧滑菜单控件
    public DrawerLayout drawerLayout;

    //定位按钮
    private Button locate;

    //获取本地蓝牙适配器，即蓝牙设备
    private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();


    //自定义Fragment
    private WeatherFragment weatherFragment;

    //当前天气信息的代号
    private String mWeatherId;

    //标题栏控件
    private Toolbar toolbar;


    //关于对话框中的标题
    private String aboutTitle;

    //对话框中的view
    private View dialogView;

    //联系电话
    private TextView phoneTv;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //界面初始化

        //设置画面为主画面 activity_main.xml
        setContentView(R.layout.activity_main);

        initView();

        //判断当前手机Android操作系统对应的SDK版本是否大于23
        if(Build.VERSION.SDK_INT >= 23){

            //如果当前手机Android操作系统对应的SDK版本大于23
            //则进行下面的权限判断
            List<String> permissionList = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_PHONE_STATE);
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CALL_PHONE);
            }


            //如果permissionList不为空，则将这些未声明的权限一次性申请
            if (!permissionList.isEmpty()) {
                String[] permissions = permissionList.toArray(new String[permissionList.size()]);
                ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
            } else {

                //如果permissionList为空，则上述运行时权限都已经在AndroidManifest.xml文件中声明
                //则程序继续运行

                initData();
            }
        }

        initData();


        Log.e(TAG,"onCreate()...");

    }


    /**
     * 对权限申请结果的逻辑处理
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, String permissions[],
                int[] grantResults) {
        switch (requestCode) {
            case 1: {

                //grantResults记录你申请的权限的结果
                if (grantResults.length > 0) {

                    //通过一个for循环将每个权限的申请的结果进行判断
                    //如果任何一个权限被拒绝，那么就直接结束程序
                    for(int result:grantResults){

                        if(result!= PackageManager.PERMISSION_GRANTED){
                            PromptUtil.showShortToast("必须同意所有权限才能使用本程序");
                            finish();
                            return;
                        }
                    }

                    initData();

                } else {

                    PromptUtil.showShortToast("发生未知错误");
                    finish();
                }

                break;

            }
            default:
        }
    }

    /**
     * 初始化数据
     */
    public void initData(){

        sp = getSharedPreferences("config", 0);
        editor = sp.edit();
        ifSwitch=sp.getString("switch",null);
        if(TextUtils.isEmpty(ifSwitch)) {
            editor.putString("switch", "on");
            editor.apply();
        }

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

    }

    /**
     * 初始化视图
     */
    public void initView(){

        dialogView=getLayoutInflater().inflate(R.layout.dialog_about,null);

        toolbar=(Toolbar) findViewById(R.id.main_toolbar);
        //将Toolbar标题栏中的标题内容设置为空
        toolbar.setTitle("");
        //使用ToolBar控件替代ActionBar控件
        setSupportActionBar(toolbar);

        //在你要接受EventBus的界面注册
        EventBus.getDefault().register(this);

        weatherFragment=(WeatherFragment)getSupportFragmentManager().findFragmentById(R.id.weather_fragment);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        navButton = (Button) findViewById(R.id.nav_button);

        locate = (Button)findViewById(R.id.locate_button);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

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

                //重新请求天气信息，达到刷新的效果
                weatherFragment.requestWeather(mWeatherId);
            }
        });


    }


    /**
     * 该方法是使用EventBus在Fragment和Activity之间传递参数
     *
     * 该方法需使用@Subscribe注解,参数类型可自定义
     * 但要根据传递的参数类型设定
     * 通过这个方法接收从Fragment中传过来的天气信息代号mWeatherId
     * @param mWeatherId
     */
    @Subscribe
    public void getEventBus(String mWeatherId) {

        this.mWeatherId=mWeatherId;
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
        inflater.inflate(R.menu.option, menu);
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
            case R.id.home:   //用户按下Home键
                editor.putBoolean("isConnected", false);
                editor.apply();
                finish();//结束当前Activity
                break;
            case R.id.about:

                aboutTitle="关于温控风扇";



                //弹出介绍该软件的对话框
                PromptUtil.showDialog(dialogView,aboutTitle,MainActivity.this);


                phoneTv = (TextView) dialogView.findViewById(R.id.phone_text);



                phoneTv.setOnClickListener(new View.OnClickListener(){

                    @Override
                    public void onClick(View v) {

                        //获取联系电话
                        String phone=phoneTv.getText().toString();

                        //实现拨打电话
                        Intent intent=new Intent(Intent.ACTION_DIAL);

                        intent.setData(Uri.parse("tel:"+phone));

                        startActivity(intent);
                    }
                });


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
                PromptUtil.showShortToast("设置GPS完成！");
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
        //在界面销毁的地方要解绑
        EventBus.getDefault().unregister(this);
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


}
