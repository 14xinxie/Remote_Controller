package com.example.xinxie.remote_conroller.util;

import android.app.ProgressDialog;
import android.content.Context;
import android.widget.Toast;

import com.example.xinxie.remote_conroller.MainActivity;

/**
 *处理提示信息的工具类
 */
public class PromptUtil {

    //全局对话框实例
    private static ProgressDialog progressDialog;

    /**
     * Toast短时间显示
     * @param s
     */
    public static void showShortToast(String s){

        Toast.makeText(MyApplication.getContext(),s,Toast.LENGTH_SHORT).show();

    }

    /**
     * Toast长时间显示
     * @param s
     */
    public static void showLongToast(String s){

        Toast.makeText(MyApplication.getContext(),s,Toast.LENGTH_LONG).show();

    }


    /**
     * 显示进度对话框
     */
    public static void showProgressDialog(String msg, Context context) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(context);
        }
        progressDialog.setMessage(msg);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    public static void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }


}


