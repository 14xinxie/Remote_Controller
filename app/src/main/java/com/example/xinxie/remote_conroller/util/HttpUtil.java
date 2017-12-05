package com.example.xinxie.remote_conroller.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;


/**
 * 处理Http网络请求的工具类
 */
public class HttpUtil {

    //全局使用一个OkHttpClient实例

    private static OkHttpClient client = new OkHttpClient();

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {

        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

}
