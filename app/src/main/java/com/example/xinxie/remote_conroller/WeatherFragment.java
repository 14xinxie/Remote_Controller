package com.example.xinxie.remote_conroller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;

import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.example.xinxie.remote_conroller.db.City;
import com.example.xinxie.remote_conroller.db.County;
import com.example.xinxie.remote_conroller.db.Province;
import com.example.xinxie.remote_conroller.gson.Weather;
import com.example.xinxie.remote_conroller.service.AutoUpdateService;
import com.example.xinxie.remote_conroller.util.HttpUtil;
import com.example.xinxie.remote_conroller.util.MyApplication;
import com.example.xinxie.remote_conroller.util.ToastUtil;
import com.example.xinxie.remote_conroller.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 14292 on 2017-11-27.
 */
public class WeatherFragment extends Fragment {


    private static final String TAG = "WeatherFragment";

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

    //当前位置天气信息的天气ID
    private String currentWeatherId;

    //当前省名
    private String currentProvinceName;

    //当前城市名
    private String currentCityName;


    //当前区县名
    private String currentCountyName;

    private SwipeRefreshLayout swipeRefresh;
    //位置监听
    private MyLocationListener myLocationListener;

    private LocationClient mLocationClient = null;

    //省份列表
    private List<Province> proList;

    //城市列表
    private List<City> citList;

    //县区列表
    private List<County> couList;

    //ChooseAreaFragment实例
    private ChooseAreaFragment chooseAreaFragment;

    //计数变量
    public static int count = 0;


    public static BDLocation location;

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
        //navButton = (Button) view.findViewById(R.id.nav_button);
        titleCity = (TextView) view.findViewById(R.id.title_city);
        titleUpdateTime = (TextView) view.findViewById(R.id.title_update_time);
        weatherInfoText = (TextView) view.findViewById(R.id.weather_info_text);
        degreeText = (TextView) view.findViewById(R.id.degree_text);
        //titleCity.setText("上海");
        //swipeRefresh = (SwipeRefreshLayout) getActivity().findViewById(R.id.swipe_refresh);
        Log.e("描述：", TAG + "........onCreateView........");
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


        //通过id获取ChooseAreaFragment实例
        chooseAreaFragment = (ChooseAreaFragment) getFragmentManager().findFragmentById(R.id.choose_area_fragment);
        //chooseAreaFragment.initData();
        //执行耗时的定位方法
        startLocate();

        //titleCity.setText(currentCityName);

        //ToastUtil.ShowShortToast();


        //requestWeather(getCurrentWeatherId(currentProvinceName,currentCityName,currentCountryName));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String weatherString = prefs.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString,"id");
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            // 无缓存时去服务器查询天气
            //mWeatherId = getActivity().getIntent().getStringExtra("weather_id");
            //weatherLayout.setVisibility(View.INVISIBLE);
            //requestWeather(mWeatherId);
            count = 0;

        }


        //Looper.prepare();
//        ASyncUploadImage as = new ASyncUploadImage();
//        as.execute("江西","赣州","石城");
//        currentWeatherId=chooseAreaFragment.getCurrentWeatherId("江西","赣州","石城");
        // requestWeather(currentWeatherId);
        Log.e("描述：", TAG + "........onActivityCreated..........");

    }

    /**
     * 根据天气id请求城市天气信息。
     */
    public void requestWeather(final String weatherId) {

        final MainActivity activity = (MainActivity) getActivity();
        final Weather weather;
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
//        String weatherUrl = "https://api.heweather.com/s6/weather?cityid=" + weatherId + "&key=5d7f57103cd143649ff6713456e274d0";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText,"id");

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else {
                            //Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();

                            ToastUtil.ShowShortToast("获取天气信息失败！");
                        }
                        //swipeRefresh.setRefreshing(false);

                        activity.swipeRefresh.setRefreshing(false);
                        //getActivity().
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        ToastUtil.ShowShortToast("获取天气信息失败！");
                        activity.swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

        //loadBingPic();
    }


    /**
     * 根据经纬度请求天气信息
     * @param latitude
     * @param longitude
     */
    public void requestWeather(final double latitude,final double longitude) {

        final MainActivity activity = (MainActivity) getActivity();
        //String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=bc0418b57b2d4918819d3974ac1285d9";
        String weatherUrl="https://free-api.heweather.com/v5/weather?city="+latitude+","+longitude+"&key=5d7f57103cd143649ff6713456e274d0";
//        String weatherUrl = "https://api.heweather.com/s6/weather?cityid=" + weatherId + "&key=5d7f57103cd143649ff6713456e274d0";

        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText,"location");
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            mWeatherId = weather.basic.weatherId;
                            //mPictureUrl=weather.now.more.pictureId;
                            //weather.now.more.pictureId;

                            showWeatherInfo(weather);
                        } else {
                            //Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();

                            ToastUtil.ShowShortToast("获取天气信息失败！");
                        }
                        //swipeRefresh.setRefreshing(false);

                        activity.swipeRefresh.setRefreshing(false);
                        //getActivity().
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        ToastUtil.ShowShortToast("获取天气信息失败！");
                        activity.swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
        //loadBingPic();
    }



    /**
     * 处理并展示Weather实体类中的数据。
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        Intent intent = new Intent(getActivity(), AutoUpdateService.class);
        getActivity().startService(intent);
    }


    /**
     * 开启定位
     */
    public void startLocate() {
        mLocationClient = new LocationClient(MyApplication.getContext());     //声明LocationClient类
        myLocationListener = new MyLocationListener();
        //mLocationClient.registerLocationListener(myLocationListener);    //注册监听函数
        mLocationClient.registerLocationListener(myLocationListener);
        initLocation();
        mLocationClient.start();//开启定位
    }


    /**
     * 初始化Location
     */
    public void initLocation() {

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


        int i = 0;

        @Override
        public void onReceiveLocation(BDLocation location) {



            //location.getLatitude()
            //location.getLongitude()
            Log.e("描述：", TAG + "........onReceiveLocation........");

            count++;
            //i++;
            currentProvinceName = location.getProvince().split("省")[0];

            //通过字符串的截取来获取城市的名字，并显示在TextView中
            currentCityName = location.getCity().split("市")[0];

            //titleCity.setText(currentCityName);

            //获取当前县区名
            currentCountyName = location.getDistrict();


            //使用标志位，设定为只获取一次当前位置的天气信息




                try{

                    requestWeather(location.getLatitude(),location.getLongitude());
                }catch (Exception e){

                    e.printStackTrace();
                }

//                        //获取当前县区的代号
//                        currentWeatherId=chooseAreaFragment.getCurrentWeatherId(currentProvinceName,currentCityName,currentCountyName);
//
//                        //使用接口回调技术，将当前天气信息代号currentWeatherId
//                        //从当前Fragment传给其关联的Activity
//                        //callBackValue.SendMessageValue(currentWeatherId);
//
//                        //根据当前县区的代号获取当前县区的天气信息
//                        requestWeather(currentWeatherId);
                //Looper.prepare();
//                ASyncUploadImage as = new ASyncUploadImage();
//                as.execute(currentProvinceName,currentCityName,currentCountyName);

//

//            StringBuffer sb = new StringBuffer(256);
//            sb.append("time : ");
//            sb.append(location.getTime());
//            sb.append("\nerror code : ");
//            sb.append(location.getLocType());
//            sb.append("\nlatitude : ");
//            sb.append(location.getLatitude());
//            sb.append("\nlontitude : ");
//            sb.append(location.getLongitude());
//            sb.append("\nradius : ");
//            sb.append(location.getRadius());
//            if (location.getLocType() == BDLocation.TypeGpsLocation) {// GPS定位结果
//                sb.append("\nspeed : ");
//                sb.append(location.getSpeed());// 单位：公里每小时
//                sb.append("\nsatellite : ");
//                sb.append(location.getSatelliteNumber());
//                sb.append("\nheight : ");
//                sb.append(location.getAltitude());// 单位：米
//                sb.append("\ndirection : ");
//                sb.append(location.getDirection());// 单位度
//                sb.append("\naddr : ");
//                sb.append(location.getAddrStr());
//                sb.append("\ndescribe : ");
//                sb.append("gps定位成功");
//
//            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {// 网络定位结果
//                sb.append("\naddr : ");
//                sb.append(location.getAddrStr());
//                //运营商信息
//                sb.append("\noperationers : ");
//                sb.append(location.getOperators());
//                sb.append("\ndescribe : ");
//                sb.append("网络定位成功");
//            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {// 离线定位结果
//                sb.append("\ndescribe : ");
//                sb.append("离线定位成功，离线定位结果也是有效的");
//            } else if (location.getLocType() == BDLocation.TypeServerError) {
//                sb.append("\ndescribe : ");
//                sb.append("服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因");
//            } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
//                sb.append("\ndescribe : ");
//                sb.append("网络不同导致定位失败，请检查网络是否通畅");
//            } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
//                sb.append("\ndescribe : ");
//                sb.append("无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机");
//            }
//            sb.append("\nlocationdescribe : ");
//            sb.append(location.getLocationDescribe());// 位置语义化信息
//            List<Poi> list = location.getPoiList();// POI数据
//            if (list != null) {
//                sb.append("\npoilist size = : ");
//                sb.append(list.size());
//                for (Poi p : list) {
//                    sb.append("\npoi= : ");
//                    sb.append(p.getId() + " " + p.getName() + " " + p.getRank());
//                }
//            }
//            //Log.e("描述：", sb.toString());


            //ToastUtil.ShowShortToast(sb.toString());

        }
    }


    private class ASyncUploadImage extends AsyncTask<String, Void, String> {

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //处理接收到的json字符串  这个方法是在UI线程执行，所以能直接更新界面
            //getJson(s);
            if("ok".equals(s)){
                ToastUtil.ShowShortToast("获取天气信息成功。。。。。。");
            }else{
                ToastUtil.ShowShortToast("获取天气信息失败。。。。。。");
            }
        }

        @Override
        protected String doInBackground(String... params) {
            //返回接收到的数据
            return getData(params[0],params[1],params[2]);
        }
    }

    private String getData(String provinceName,String cityName,String countyName ) {

        try{
            String weatherId= chooseAreaFragment.getCurrentWeatherId(provinceName,cityName,countyName);


            requestWeather(weatherId);

        }catch(Exception e){
            e.printStackTrace();
            return "not";
        }

        return "ok";



    }


//    private void getJson(String json) {
//
//
//    }
}


