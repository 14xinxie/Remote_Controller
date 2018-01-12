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
import android.view.KeyEvent;
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

    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    //自带下拉刷新功能的控件
    public SwipeRefreshLayout swipeRefresh;

    //主菜单按钮
    private Button btn_nav;

    //侧滑菜单控件
    public DrawerLayout drawerLayout;

    //定位按钮
    private Button btn_locate;

    //自定义Fragment
    private WeatherFragment weatherFragment;

    //当前天气信息的代号
    private String mWeatherId;

    //标题栏控件
    private Toolbar mainToolbar;

    //退出时的时间
    private long mExitTime;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

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
    private void initData(){
        sp = getSharedPreferences("config", 0);
        editor = sp.edit();

    }

    /**
     * 初始化视图
     */
    private void initView(){

        mainToolbar=(Toolbar) findViewById(R.id.main_toolbar);
        //将Toolbar标题栏中的标题内容设置为空
        mainToolbar.setTitle("");
        //使用ToolBar控件替代ActionBar控件
        setSupportActionBar(mainToolbar);

        //在你要接受EventBus的界面注册
        EventBus.getDefault().register(this);

        weatherFragment=(WeatherFragment)getSupportFragmentManager().findFragmentById(R.id.weather_fragment);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);

        swipeRefresh.setColorSchemeResources(R.color.colorPrimary);

        btn_nav = (Button) findViewById(R.id.nav_button);

        btn_locate = (Button)findViewById(R.id.locate_button);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        btn_nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //这里设置的方向应该跟下面xml文件里面的layout_gravity方向相同
                //从左往右滑出侧滑菜单
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        btn_locate.setOnClickListener(new View.OnClickListener(){

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
     * 重写home键的监听事件
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {

            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //退出程序
    private void exit() {
        if ((System.currentTimeMillis() - mExitTime) > 2000) {
            PromptUtil.showShortToast("再按一次退出程序");
            mExitTime = System.currentTimeMillis();
        } else {
            finish();//结束当前Activity

        }
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
            case R.id.about:
                //跳转至AboutActivity
                Intent intent=new Intent(MainActivity.this,AboutActivity.class);
                startActivity(intent);
                finish();//结束当前Activity
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * 接收活动结果，响应startActivityForResult()
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
    }

    //Activity销毁时调用
    protected void onDestroy(){
        super.onDestroy();
        //在界面销毁的地方要解绑
        EventBus.getDefault().unregister(this);
    }

}
