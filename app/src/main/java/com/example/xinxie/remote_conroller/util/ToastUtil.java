package com.example.xinxie.remote_conroller.util;

import android.widget.Toast;

/**
 *
 * Toast工具类
 * Created by 14292 on 2017-11-16.
 */
public class ToastUtil {

    /**
     * Toast长时间显示
     * @param s
     */
    public static void ShowShortToast(String s){

        Toast.makeText(MyApplication.getContext(),s,Toast.LENGTH_SHORT).show();

    }

    /**
     * Toast短时间显示
     * @param s
     */
    public static void ShowLongToast(String s){

        Toast.makeText(MyApplication.getContext(),s,Toast.LENGTH_LONG).show();

    }
}
