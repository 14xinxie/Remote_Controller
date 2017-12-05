package com.example.xinxie.remote_conroller;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class AboutActivity extends AppCompatActivity {

    private String TAG="AboutActivity";

    private Button backButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        backButton = (Button)findViewById(R.id.back_button);

        backButton.setOnClickListener(new View.OnClickListener(){

              @Override
              public void onClick(View v) {

                  Intent intent=new Intent(AboutActivity.this,MainActivity.class);
                  startActivity(intent);
                  finish();
              }
          }
        );

        Log.e(TAG,"onCreate()...");

        //Toolbar main_toolbar=(Toolbar) findViewById(R.id.main_toolbar);
        //给左上角图标的左边加上一个返回的图标
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home:

//                PromptUtil.showShortToast("点击了Home键");
//                Intent intent = new Intent(AboutActivity.this,MainActivity.class);
//                startActivity(intent);
//                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
