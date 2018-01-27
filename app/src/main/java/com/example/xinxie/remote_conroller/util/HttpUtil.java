package com.example.xinxie.remote_conroller.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;


/**
 * 处理Http网络请求的工具类
 */
public class HttpUtil {

    //全局使用一个OkHttpClient实例
    //设置超时时间
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build();
    /**
     * 发送网络请求
     * @param address
     * @param callback
     */
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {

        //如果网络可用，则发送网络请求
        if(!isNetworkAvailable(MyApplication.getContext())){
            PromptUtil.showShortToast("当前网络不可用，请检查你的网络设置");
            return;
        }
        Request request = new Request.Builder().url(address).build();

        //enqueue(callback):开启异步线程访问网络
        try{
            getClientInstance().newCall(request).enqueue(callback);
        }catch (Exception e){
            e.printStackTrace();
        }

    }



    /**
     * 检查网络是否可用
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {

        ConnectivityManager manager = (ConnectivityManager) context
                .getApplicationContext().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return false;
        }
        NetworkInfo networkinfo = manager.getActiveNetworkInfo();

        if (networkinfo == null || !networkinfo.isAvailable()) {
            return false;
        }

        return true;
    }

    /**
     * 获取OkHttpClient
     * 并且设置https 访问的时候对所有证书都进行信任
     * @return OkHttpClient
     */
    public static OkHttpClient getClientInstance() throws Exception {
        final X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, new TrustManager[]{trustManager}, new SecureRandom());

        SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        OkHttpClient.Builder builder = client.newBuilder();
        return builder
                .sslSocketFactory(sslSocketFactory, trustManager)
                .hostnameVerifier(new HostnameVerifier() {
                    @Override
                    public boolean verify(String hostname, SSLSession session) {
                        return true;
                    }
                })
                .build();
    }
}
