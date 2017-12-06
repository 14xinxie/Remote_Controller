package com.example.xinxie.remote_conroller.util;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
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
            progressDialog.dismiss();
        }
    }


    public static void showDialog(final View view, String title, Context context){

        final AlertDialog.Builder builder=new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setPositiveButton("确定",new DialogInterface.OnClickListener(){

            @Override
            public void onClick(DialogInterface dialogInterface,int which){



                //关闭对话框之前，需移除view的父容器
                //否则会报异常
                if (view != null) {
                    ViewGroup parent = (ViewGroup) view.getParent();
                    if (parent != null) {
                        parent.removeView(view);
                    }
                }
                    builder.show().dismiss();

            }


        });

        //设置对话框中的布局为自定义布局
        builder.setView(view);
        builder.show();
    }

}


