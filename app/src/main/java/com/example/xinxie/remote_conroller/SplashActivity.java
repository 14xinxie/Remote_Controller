package com.example.xinxie.remote_conroller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/**
 * 程序启动时的闪屏界面
 */
public class SplashActivity extends AppCompatActivity {

    //显示倒计时
    private TextView tv_countDown;

    //是否已跳转标志位,默认为未跳转
    private boolean is_skip=false;

    //倒计时实例变量
    private MyCountDownTimer mc;

    //处理延时启动MainActivity的handler
    private Handler handler;

    //延时结束后执行的Runnable接口
    private SplashRunnable splashRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 通过下面两行代码也可实现全屏无标题栏显示activity
        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_welcome);
        tv_countDown = (TextView) findViewById(R.id.tv_countDown);
        /********************************************************************************
         *
         * 倒计时闪屏实现方式
         *
         * ******************************************************************************/
        mc = new MyCountDownTimer(4000, 1000);
        mc.start();

        handler=new Handler();

        splashRunnable=new SplashRunnable();
        //延时结束时执行splashRunnable里面的方法

        //开启延时启动
        handler.postDelayed(splashRunnable, 1000*4);

        tv_countDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!is_skip){
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                    finish();
                    is_skip=true; //置跳转标志位为true

                }

            }
        });
    }

    /**
     * 实现Runnable接口
     */
    class SplashRunnable implements Runnable {
        @Override
        public void run() {
            if(!is_skip){
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                is_skip=true;
            }

        }
    }

    /**
     * 继承CountDownTimer类，实现倒计时
     */
    class MyCountDownTimer extends CountDownTimer {
        //millisInFuture:倒计时的总数,单位毫秒
        //例如 millisInFuture=1000;表示1秒
        //countDownInterval:表示间隔多少毫秒,调用一次onTick方法()
        //例如: countDownInterval =1000;表示每1000毫秒调用一次onTick()
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }
        public void onFinish() {
            //tv_countDown.setText("跳转中……");
        }
        public void onTick(long millisUntilFinished) {
            tv_countDown.setText("跳过(" + millisUntilFinished / 1000 + "秒)");
        }
    }

    /**
     *  闪屏界面SplashActvity销毁时调用
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        //取消倒计时
        mc.cancel();
        //关闭延时启动
        handler.removeCallbacks(splashRunnable);

    }
}


