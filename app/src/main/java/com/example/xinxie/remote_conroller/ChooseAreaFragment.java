package com.example.xinxie.remote_conroller;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xinxie.remote_conroller.db.City;
import com.example.xinxie.remote_conroller.db.County;
import com.example.xinxie.remote_conroller.db.Province;
import com.example.xinxie.remote_conroller.util.HttpUtil;
import com.example.xinxie.remote_conroller.util.ToastUtil;
import com.example.xinxie.remote_conroller.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    private static final String TAG = "ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;







    /**
     * 每次创建、绘制该Fragment的View组件时回调该方法
     * Fragment将会显示该方法返回的View组件。
     * 主要给当前的Fragment绘制UI布局，尽量不要在这里做耗时操作
     * 可以使用线程更新UI
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        Log.e("描述：", TAG+"........onCreateView........");
        return view;
    }


    /**
     *当Activity中的onCreate方法执行完后调用。
     *这个方法主要是初始化 那些你需要你的父Activity或者Fragment的UI已经被完
     *整初始化 才能初始化的元素。
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                } else if (currentLevel == LEVEL_COUNTY) {

                    //获取当前选中的县区的代号（用于访问和风天气接口获取相应天气信息），
                    String weatherId = countyList.get(position).getWeatherId();

                    //使用接口回调技术，将当前天气信息代号weatherId
                    //从当前Fragment传给其关联的Activity
                    //callBackValue.SendMessageValue(weatherId);
//                    if (getActivity() instanceof MainActivity) {
//                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
//                        intent.putExtra("weather_id", weatherId);
//                        startActivity(intent);
//                        //结束与ChooseAreaFragment关联的Activity
//                        getActivity().finish();
//                    } else if (getActivity() instanceof WeatherActivity) {
//                        WeatherActivity activity = (WeatherActivity) getActivity();
//
//                        //如果当前Activity是WeatherActivity,
//                        //则隐藏侧滑菜单，并获取选中县区的天气信息，
//                        //然后刷新主屏幕中的内容
//                        activity.drawerLayout.closeDrawers();
//                        activity.swipeRefresh.setRefreshing(true);
//                        activity.requestWeather(weatherId);
//                    }
                    MainActivity activity = (MainActivity) getActivity();

                    //如果当前Activity是WeatherActivity,
                    //则隐藏侧滑菜单，并获取选中县区的天气信息，
                    //然后刷新主屏幕中的内容
                    activity.drawerLayout.closeDrawers();
                    activity.swipeRefresh.setRefreshing(true);
                    //activity.requestWeather(weatherId);
                    //getActivity().getFragmentManager().get

                    //通过id获取WeatherFragment实例
                    WeatherFragment weatherFragment=(WeatherFragment)getFragmentManager().findFragmentById(R.id.weather_fragment);
                    weatherFragment.requestWeather(weatherId);
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        //查询所有的省份信息
        queryProvinces();


        Log.e("描述：", TAG+"........onActivityCreated........");
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询。
     */
    private void queryCounties() {
        //ToastUtil.ShowShortToast("查询中。。。。");

        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityId = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据。
     */
    public void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                    boolean result = false;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {

                                queryCounties();
                            }

                            //ToastUtil.ShowShortToast(Thread.currentThread().getName());
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


    /**
     * 根据当前的城市名和县区名获取当前县区的代号
     * @param cityName
     * @param countyName
     * @return
     */
    public String getCurrentWeatherId(String provinceName,String cityName,String countyName){

        String weatherId=null;
        int cityCode=0;
        int provinceCode=0;
        //查询数据库中所有的省份
        List<Province> proList=DataSupport.findAll(Province.class);

        //如果数据库中没有存储省份信息,
        //则从和风天气服务器中获取并存入数据库中
        //然后再查询数据库
        if(proList.size()==0){
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
            proList=DataSupport.findAll(Province.class);
        }

        //获取当前的省份的代号
        for(int i=0;i<proList.size();i++){
            if(provinceName.equals(proList.get(i).getProvinceName())){
                provinceCode=proList.get(i).getProvinceCode();
                selectedProvince=proList.get(i);
                break;
            }
        }


        //查询数据库中当前省份下所有的城市
        List<City> citList=DataSupport.where("provinceId=?",String.valueOf(selectedProvince.getId())).find(City.class);
        //如果数据库中没有存储当前省份下的城市信息,
        //则从和风天气服务器中获取并存入数据库中
        //然后再查询数据库
        if(citList.size()==0) {
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
            citList=DataSupport.where("provinceId=?",String.valueOf(selectedProvince.getId())).find(City.class);
        }

        //获取当前城市的代号
        for (int i=0;i<citList.size();i++) {
            if (cityName.equals(citList.get(i).getCityName())) {
                cityCode = citList.get(i).getCityCode();
                selectedCity=citList.get(i);
                break;
            }
        }


        //获取当前县区的代号
        //若当前县区名字在数据库中不存在，则默认为当前城市的第一个县区的代号

        //查询数据库中当前城市下的所有县区
        List<County> couList=DataSupport.where("cityId=?",String.valueOf(selectedCity.getId())).find(County.class);

        //int i=0;
        if(couList.size()==0) {
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "county");
            couList=DataSupport.where("cityId=?",String.valueOf(selectedCity.getId())).find(County.class);
        }

        //int i =0;
        int m=0;
        for(int i=0;i<couList.size();i++){
            //如果在数据库中找到当前县区名字，则获取当前县区的代号，并跳出for循环
            if(countyName.equals(couList.get(i).getCountyName())){
                weatherId=couList.get(i).getWeatherId();
                break;
            }
            m++;
        }

        //判断当前县区名字在数据库中是否存在
        if(m==couList.size()){
            weatherId=couList.get(0).getWeatherId();
        }

        return weatherId;
    }


}
