package com.example.xinxie.remote_conroller.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
            if(progressDialog.isShowing()){
                //get the Context object that was used to great the dialog
                Context context = ((ContextWrapper)progressDialog.getContext()).getBaseContext();

                //if the Context used here was an activity AND it hasn't been finished or destroyed
                //then dismiss it
                if(context instanceof Activity) {
                    if(!((Activity)context).isFinishing() && !((Activity)context).isDestroyed())
                        progressDialog.dismiss();
                } else //if the Context used wasnt an Activity, then dismiss it too
                    progressDialog.dismiss();
            }
                progressDialog = null;
            }

        }
    }




