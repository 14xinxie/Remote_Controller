package com.example.xinxie.remote_conroller;

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
import com.example.xinxie.remote_conroller.db.City;
import com.example.xinxie.remote_conroller.db.County;
import com.example.xinxie.remote_conroller.db.Province;
import com.example.xinxie.remote_conroller.util.HttpUtil;
import com.example.xinxie.remote_conroller.util.JsonUtil;
import com.example.xinxie.remote_conroller.util.MyApplication;
import com.example.xinxie.remote_conroller.util.PromptUtil;
import org.greenrobot.eventbus.EventBus;
import org.litepal.crud.DataSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    private String TAG="ChooseAreaFragment";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private TextView titleText;

    private Button backButton;

    private ListView listView;


    private ArrayAdapter<String> adapter;

    //ListView中显示的数据
    private List<String> dataList = new ArrayList<>();

    //省列表
    private List<Province> provinceList;

    //市列表
    private List<City> cityList;

    //县列表
    private List<County> countyList;

    //选中的省份
    private Province selectedProvince;

    //选中的城市
    private City selectedCity;

    //当前选中的级别
    public int currentLevel;

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


        Log.e(TAG,"onCreateView...");
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

                    //使用EventBus在Fragment和Activity之间传递参数
                    //即将当前的天气信息代号weatherId发送MainActivity送给
                    EventBus.getDefault().post(weatherId);

                    MainActivity activity = (MainActivity) getActivity();

                    //隐藏侧滑菜单
                    activity.drawerLayout.closeDrawers();
                    activity.swipeRefresh.setRefreshing(true);

                    //通过id获取WeatherFragment实例
                    WeatherFragment weatherFragment=(WeatherFragment)getFragmentManager().findFragmentById(R.id.weather_fragment);
                    //获取选中县区的天气信息
                    weatherFragment.requestWeather(weatherId);
                }
            }
        });

        //侧滑菜单中的返回按钮点击事件
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

        Log.e(TAG,"onActivityCreated...");
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询。
     * 由于该方法需要在MainActivity中调用，所以声明为public
     */
    public void queryProvinces() {
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
     * 由于该方法需要在MainActivity中调用，所以声明为public
     */
    public void queryCities() {
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
    private void queryFromServer(String address, final String type) {

        //getActivity()有可能为空，需要提前判断
        if(getActivity()==null){

            PromptUtil.showShortToast("服务器繁忙，请稍后重试！");
            return;
        }
        if(!HttpUtil.isNetworkAvailable(MyApplication.getContext())){
            PromptUtil.showShortToast("当前网络不可用，请检查你的网络设置");
            return;
        }


        PromptUtil.showProgressDialog("正在加载...",getActivity());
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)) {
                    result = JsonUtil.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = JsonUtil.handleCityResponse(responseText, selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = JsonUtil.handleCountyResponse(responseText, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {

                            PromptUtil.closeProgressDialog();
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {

                                queryCounties();
                            }
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
                        PromptUtil.closeProgressDialog();
                        PromptUtil.showShortToast("加载失败");
                    }
                });
            }
        });
    }


    /**
     * Fragment销毁时调用
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
