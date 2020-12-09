package com.example.downloadservicetest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int WRITE_REQUEST = 1;
    private Button btn_start;
    private Button btn_pause;
    private Button btn_cancel;
    //private DownloadService downloadService;

    private DownloadService.DownloadBinder downloadBinder ;//为了调用service里面的方法，onbind传输数据

    private ServiceConnection conn = new ServiceConnection() {//bindservice要传的参数conn，在这从写匿名内部类
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {//活动与服务成功绑定时
            downloadBinder = (DownloadService.DownloadBinder) service;//service向下转型，得到downloadBinder实例
            //绑定成功后在这样得到实例，使活动和服务更加关系紧密
            //没绑定成功如果使用一般的new可能会报错吧
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {//活动与服务没有成功绑定时
//
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化
        init();
        btn_start.setOnClickListener(this);
        btn_pause.setOnClickListener(this);
        btn_cancel.setOnClickListener(this);

        //创建服务
        Intent i = new Intent(this,DownloadService.class);
        startService(i);//启动服务，为了一直在后台运行
        bindService(i,conn,BIND_AUTO_CREATE);//绑定服务，为了通信  记住！！！第一次都忘写了

        //权限申请
        //先判断是否有这个权限
        if(ContextCompat.checkSelfPermission(MainActivity.this
                , Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
        //没有的话，申请一下权限看他同意吗，结果直接跳转到方法onRequestPermissionsResult
            ActivityCompat.requestPermissions(MainActivity.this
                    , new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                    ,WRITE_REQUEST);
        }

    }

    //权限申请结束后自动调用的方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
       // super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case WRITE_REQUEST:
                if(grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED ){
                    Toast.makeText(this,"拒绝了",Toast.LENGTH_SHORT).show();
                    //finish();
                }else {
                    Toast.makeText(this,"接受了",Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }

    private void init() {
        btn_start = findViewById(R.id.btn_start);
        btn_pause = findViewById(R.id.btn_pause);
        btn_cancel = findViewById(R.id.btn_cancel);
    }

    //活动销毁时记得关绑定
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
    }

    @Override
    public void onClick(View v) {
        if(downloadBinder== null)//..
            return ;
        switch (v.getId()){
            case R.id.btn_start://自己创建一个下载地址，后台服务下载，调用服务的startDownload方法
                String url = "https://raw.githubusercontent.com/guolindev/eclipse/master/eclipse-inst-win64.exe";
                downloadBinder.startDownload(url);
                break;
            case R.id.btn_pause://后台服务停止
                downloadBinder.pauseDownload();
                break;
            case R.id.btn_cancel://后台服务取消
                downloadBinder.cancelDownload();
                break;
            default:
                break;
        }
    }
}