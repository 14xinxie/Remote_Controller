package com.example.xinxie.remote_conroller;


import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.bumptech.glide.Glide;
import com.example.xinxie.remote_conroller.gson.Weather;
import com.example.xinxie.remote_conroller.service.AutoUpdateService;
import com.example.xinxie.remote_conroller.util.HttpUtil;
import com.example.xinxie.remote_conroller.util.MyApplication;
import com.example.xinxie.remote_conroller.util.JsonUtil;
import com.example.xinxie.remote_conroller.util.PromptUtil;
import org.greenrobot.eventbus.EventBus;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 14292 on 2017-11-27.
 */
public class WeatherFragment extends Fragment {

    private String TAG="WeatherFragment";

    private View view;

    //当前城市名
    private TextView titleCity;

    //天气信息更新时间
    private TextView titleUpdateTime;

    //实时温度
    private TextView degreeText;

    //实时天气状况
    private TextView weatherInfoText;

    //缓存的天气信息的天气ID
    private String mWeatherId;

    //位置监听实例
    private MyLocationListener myLocationListener;

    private LocationClient mLocationClient;

    //获取当前位置天气信息的标志位
    //static boolean得默认值为false
    public static boolean locaWeatherFlag;

    //实时天气状况图片
    private ImageView weatherPicImg;

    //暂存当前天气信息的Id的中间变量
    private String temWeatherId=null;

    //判断是否切换城市的标志位
    private boolean isSwitch;
    /**
     * 每次创建、绘制该Fragment的View组件时回调该方法
     * Fragment将会显示该方法返回的View组件。
     * 主要给当前的Fragment绘制UI布局，尽量不要在这里做耗时操作
     * 可以使用线程更新UI
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

            view = inflater.inflate(R.layout.weather_fragment, container, false);
            titleCity = (TextView) view.findViewById(R.id.title_city);
            titleUpdateTime = (TextView) view.findViewById(R.id.title_update_time);
            weatherInfoText = (TextView) view.findViewById(R.id.weather_info_text);
            degreeText = (TextView) view.findViewById(R.id.degree_text);
            weatherPicImg = (ImageView) view.findViewById(R.id.weather_pic_img);

        Log.e(TAG,"onCreateView...");
        return view;
    }


    /**
     * 当Activity中的onCreate方法执行完后调用。
     * 这个方法主要是初始化 那些你需要你的父Activity或者Fragment的UI已经被完
     * 整初始化 才能初始化的元素。
     *
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String weatherString = prefs.getString("weather", null);
        startLocate();
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = JsonUtil.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            //发送mWeatherId给MainActivity
            EventBus.getDefault().post(mWeatherId);

            showWeatherInfo(weather);

        } else {

            //没有缓存时获取当前位置的天气信息
            //即设置定位标志位为false,开启定位
            locaWeatherFlag=false;

        }

        Log.e(TAG,"onActivityCreated...");

    }

    @Override
    public void onResume() {
        super.onResume();

        Log.e(TAG,"onResume...");

//        //getUserVisibleHint()方法判断当前Fragment对于用户是否可见
//        if(getUserVisibleHint()){
//            PromptUtil.showShortToast("可见");
//        }
    }

    /**
     * 根据天气id请求城市天气信息。
     * 由于MainActivity中还需调用此方法，所以不能将该方法的访问域定义为private
     */
    protected void requestWeather(final String weatherId) {

        final MainActivity mActivity=(MainActivity) getActivity();

        if(!weatherId.equals(temWeatherId)){
            //PromptUtil.showShortToast("切换城市成功");
            //暂存天气信息的Id
            temWeatherId=weatherId;
            isSwitch=true; //置切换城市标志位为true

        }else{
            isSwitch=false;
        }

        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        //getActivity()有可能为空，需要提前判断
        if(getActivity()==null){

            PromptUtil.showShortToast("服务器繁忙，请稍后重试！");
            return;
        }

        //使用okhttp的Http框架发出异步请求
        //当返回响应时才执行Callback中的onResponse方法
        //但是从发出异步请求到返回响应有一段时间
        //这一段时间程序继续向下执行
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = JsonUtil.handleWeatherResponse(responseText);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(getActivity());
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            if(sp.getString("weather",null)!=null){

                                //如果未切换城市
                                if(isSwitch==false){

                                    //如果获取的天气信息和之前存储的天气信息相同
                                    //则提示暂无更新
                                    if(sp.getString("weather",null).equals(responseText)){
                                        mWeatherId = weather.basic.weatherId;

                                        //发送mWeatherId给MainActivity
                                        EventBus.getDefault().post(mWeatherId);
                                        PromptUtil.showShortToast("暂无天气信息更新");
                                    }else{

                                        //如果获取的天气信息和之前存储的天气信息不同
                                        //则提示天气信息更新成功
                                        editor.putString("weather", responseText);
                                        editor.apply();
                                        mWeatherId = weather.basic.weatherId;

                                        //发送mWeatherId给MainActivity
                                        EventBus.getDefault().post(mWeatherId);

                                        PromptUtil.showShortToast("天气信息更新成功");
                                        showWeatherInfo(weather);
                                    }

                                }else{

                                    //如果已切换城市
                                    editor.putString("weather", responseText);
                                    editor.apply();
                                    mWeatherId = weather.basic.weatherId;
                                    //发送mWeatherId给MainActivity
                                    EventBus.getDefault().post(mWeatherId);
                                    showWeatherInfo(weather);

                                }

                            }else{
                                editor.putString("weather", responseText);
                                editor.apply();
                                mWeatherId = weather.basic.weatherId;

                                //发送mWeatherId给MainActivity
                                EventBus.getDefault().post(mWeatherId);

                                showWeatherInfo(weather);
                            }

                        } else {

                            PromptUtil.showShortToast("获取天气信息失败！");
                        }
                        mActivity.swipeRefresh.setRefreshing(false);
                    }
                });

            }

            @Override
            public void onFailure(Call call, IOException e) {


                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        PromptUtil.showShortToast("获取天气信息失败！");
                        mActivity.swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

    }


    /**
     * 根据经纬度请求天气信息
     * @param latitude
     * @param longitude
     */
    private void requestWeather(final double latitude,final double longitude) {


        //获取MainActivity实例
        final MainActivity mActivity=(MainActivity) getActivity();

        //getActivity()有可能为空，需要提前判断
        if(getActivity()==null){

            PromptUtil.showShortToast("服务器繁忙，请稍后重试！");
            return;
        }

        String weatherUrl = "http://guolin.tech/api/weather?cityid="+latitude+","+longitude+"&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = JsonUtil.handleWeatherResponse(responseText);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences sp=PreferenceManager.getDefaultSharedPreferences(getActivity());
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();

                            if(sp.getString("weather",null)==null){
                                editor.putString("weather", responseText);
                                editor.apply();
                                mWeatherId = weather.basic.weatherId;
                                PromptUtil.showShortToast("定位中...");
                                //发送mWeatherId给MainActivity
                                EventBus.getDefault().post(mWeatherId);

                                showWeatherInfo(weather);
                            }else if(!(sp.getString("weather",null).equals(responseText))){
                                editor.putString("weather", responseText);
                                editor.apply();
                                PromptUtil.showShortToast("定位中...");
                                mWeatherId = weather.basic.weatherId;

                                //发送mWeatherId给MainActivity
                                EventBus.getDefault().post(mWeatherId);

                                showWeatherInfo(weather);
                            }else{
                                PromptUtil.showShortToast("已定位到当前城市");
                            }
                            editor.putString("weather", responseText);
                            editor.apply();

                        } else {

                            PromptUtil.showShortToast("获取天气信息失败！");
                        }
                        mActivity.swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, final IOException e) {
                e.printStackTrace();

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        PromptUtil.showShortToast("获取天气信息失败！");
                        mActivity.swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

    }


    /**
     * 处理并展示Weather实体类中的数据。
     */
    private void showWeatherInfo(Weather weather) {

        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        String mWeatherPictureId=weather.now.more.pictureId;

        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        //网络图片的请求Url
        String requestWeatherPic = "https://www.heweather.com/files/images/cond_icon/"+mWeatherPictureId+".png";
        //使用OKHttp网络框架加载实时天气状况的图片
        HttpUtil.sendOkHttpRequest(requestWeatherPic, new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                if(response.isSuccessful()){

                    //获取响应的网络图片，并将其转换为二进制图片，即字节数组形式
                    byte[] bytes=response.body().bytes();

                    //将字节数组转换成位图
                    final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            weatherPicImg.setImageBitmap(bitmap);
                            ///PromptUtil.showShortToast("图片加载成功！");
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, final IOException e) {

                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //PromptUtil.showShortToast("图片加载失败！");
                    }
                });
            }
        });

        //开启自动更新服务
        Intent intent = new Intent(getActivity(), AutoUpdateService.class);
        getActivity().startService(intent);
    }

    /**
     * 开启定位
     */
    private void startLocate() {
        mLocationClient = new LocationClient(MyApplication.getContext());     //声明LocationClient类
        myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        initLocation();

        mLocationClient.start();//开启定位
    }

    /**
     * 初始化Location
     */
    private void initLocation() {

        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving);
        //可选，设置定位模式，默认高精度
        //LocationMode.Hight_Accuracy：高精度；
        //LocationMode. Battery_Saving：低功耗；
        //LocationMode. Device_Sensors：仅使用设备；
        option.setCoorType("bd09ll");
        //可选，设置返回经纬度坐标类型，默认gcj02
        //gcj02：国测局坐标；
        //bd09ll：百度经纬度坐标；
        //bd09：百度墨卡托坐标；
        //海外地区定位，无需设置坐标类型，统一返回wgs84类型坐标
        int span = 1000;
        option.setScanSpan(span);
        //可选，设置发起定位请求的间隔，int类型，单位ms
        //如果设置为0，则代表单次定位，即仅定位一次，默认为0
        //如果设置非0，需设置1000ms以上才有效
        option.setIsNeedAddress(true);
        //可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);
        //可选，设置是否使用gps，默认false
        //使用高精度和仅用设备两种定位模式的，参数必须设置为true
        option.setLocationNotify(true);
        //可选，设置是否当GPS有效时按照1S/1次频率输出GPS结果，默认false
        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要位置语义化结果，
        // 可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);
        //可选，默认false，设置是否需要POI结果，
        // 可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，
        // 设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
        //mLocationClient为第二步初始化过的LocationClient对象
        //需将配置好的LocationClientOption对象，
        // 通过setLocOption方法传递给LocationClient对象使用
    }

    /**
     * 实现内部类实现位置实时监听
     * 重写onReceiveLocation方法
     */
    class MyLocationListener extends BDAbstractLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            //使用标志位，设定为只获取一次当前位置的天气信息
            if(locaWeatherFlag==false) {

                requestWeather(location.getLatitude(), location.getLongitude());

                //PromptUtil.showShortToast("定位中...");
                locaWeatherFlag = true;

            }

        }
    }

    /**
     * Fragment被销毁时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}


