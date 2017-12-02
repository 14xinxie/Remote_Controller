package com.example.xinxie.remote_conroller.util;

import android.app.Application;
import android.content.Context;

import org.litepal.LitePal;

/**
 *
 * 自定义一个用于获取全局context的Application类
 * Created by 14292 on 2017-11-16.
 */
public class MyApplication extends Application {

    //定义一个Android应用范围内的context
    private static Context context;

    @Override
    public void onCreate(){

        super.onCreate();

        context=getApplicationContext();

        //由于任何一个项目只能配置一个Application，且在AndroidManifest.xml文件中
        //已经对MyApplication进行了配置，所以org.litepal.LitePalApplication只能
        //通过MyApplication的onCreate()方法获取Context进行初始化了

        LitePal.initialize(context);
    }


    /**
     * 获取全局Context
     * @return
     */
    public static Context getContext(){

        return context;
    }
}
