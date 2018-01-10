package com.example.xinxie.remote_conroller;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.xinxie.remote_conroller.util.PromptUtil;

public class AboutActivity extends AppCompatActivity {

    //private String TAG="AboutActivity";

    //返回按钮
    private Button backButton;

    //联系电话，点击可拨打
    private TextView phoneText;

    //标题栏控件
    private Toolbar aboutToolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        aboutToolbar=(Toolbar) findViewById(R.id.about_toolbar);
        //将Toolbar标题栏中的标题内容设置为空,使用布局文件中的自定义居中的标题
        aboutToolbar.setTitle("");
        //使用ToolBar控件替代ActionBar控件
        setSupportActionBar(aboutToolbar);

        backButton = (Button)findViewById(R.id.back_button);

        phoneText = (TextView)findViewById(R.id.phone_text);

        backButton.setOnClickListener(new View.OnClickListener(){

              @Override
              public void onClick(View v) {

                  Intent intent=new Intent(AboutActivity.this,MainActivity.class);
                  startActivity(intent);
                  finish();
              }
          }
        );

        phoneText.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                //获取联系电话
                String phone=phoneText.getText().toString();
                //实现拨打电话
                Intent intent=new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:"+phone));
                startActivity(intent);
            }
        });

    }

    /**
     * Home键监听事件
     */
    @Override
    public void onBackPressed() {
        //super.onBackPressed(); //注释这行代码,才能保证按下Home键不退出程序
        Intent intent=new Intent(this,MainActivity.class);
        startActivity(intent);//跳转至MainActivity
        finish();
    }
}
