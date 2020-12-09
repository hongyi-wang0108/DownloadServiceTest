package com.example.downloadservicetest;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.downloadservicetest.Interface.DownloadInterface;

import java.io.File;
import java.util.Timer;

public class DownloadService extends Service {//控制下载进程，前台通知是否展现，新建通知
    public NotificationManager getNotificationManager(){//为了得到NotificationManager，进行通知的notify
        return (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }
    //NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    private DownloadInterface downloadlistener = new DownloadInterface() {//重写接口方法，这个接口是为了更好的反应下载的结果
        @Override
        public void OnProgress(int progress) {//更新通知，下载的进度
            /*Intent i = new Intent(DownloadService.this,MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(DownloadService.this,0,i,0);
            Notification notification = new NotificationCompat
                    .Builder(DownloadService.this)
                    .setContentTitle("title")
                    .setContentIntent(pi)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher))
                    .setWhen(System.currentTimeMillis())
                    .build();*/
            getNotificationManager().notify(DOWNLOAD,getNotification("downloading",progress));
        }
        //控制下载进程，前台通知是否展现，新建通知
        @Override
        public void OnSuccess() {//下载结果是成功：
            downloadTask = null;//线程关闭
            stopForeground(true);//停止前台服务
            getNotificationManager().notify(1,getNotification("onsuccess",-1));//更新通知
        }

        @Override
        public void OnFailed() {
            downloadTask = null;//线程关闭
            stopForeground(true);//停止前台服务
            getNotificationManager().notify(1,getNotification("onfailed",-1));//更新通知
        }

        @Override
        public void OnCanceled() {
            downloadTask = null;//线程关闭
            stopForeground(true);//停止前台服务
            getNotificationManager().notify(1,getNotification("oncanceled",-1));//更新通知
        }

        @Override
        public void OnPaused() {
            downloadTask = null;//线程关闭
            //不用停止前台，可能之后会重启下载任务之类的
            getNotificationManager().notify(1,getNotification("oncanceled",-1));//更新通知
        }
    };
    private DownloadTask downloadTask  ;//在AsyncTask中进行耗时任务
    private DownloadBinder mbinder = new DownloadBinder();//自己创建一个类exendsbinder，之后使得活动能调用这个类的方法
    private int DOWNLOAD = 1;
    private String lasturl = null;//下载地址

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {//通信
        return mbinder;
    }

    public class DownloadBinder extends Binder {//自己创建的类，为了返给onbind方法
        public void startDownload(String url) {//他的点击前的下载状态：暂停/没开始下载。点完btn_start之后调用这个，参数是下载的地址
            if(downloadTask == null){//暂停/没开始下载
                lasturl = url;
                downloadTask = new DownloadTask(downloadlistener);//创建异步任务
                downloadTask.execute(lasturl);//执行
                startForeground(DOWNLOAD,getNotification("startDownload",0));//开启前台服务
            }
        }

        public void pauseDownload() {//他的点击前的下载状态：开始。点完btn_pause之后调用这个
            if(downloadTask != null){//下载当前状态不为null，暂停才有意义
                /*downloadTask = new DownloadTask(downloadlistener);
                downloadTask*/
                downloadTask.pauseDownload();//这时候已经经历过上面的startDownload了，所以可以直接调用异步任务的pauseDownload方法
            }
        }

        public void cancelDownload() {//他的点击前的下载状态：开始/暂停/没开始下载/下载完的
            if(downloadTask == null){//现在下载状态是   没开始下载/暂停/下载完的  ，则要删除文件
                if(lasturl != null){//下载地址为空说明不用删除啥，现在不为空，进行下面的
                    //得到下载的名字和地址
                    String filename = lasturl.substring(lasturl.lastIndexOf("/"));//名字
                    String path = Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            .getPath();//地址：在SD卡中，可通过这个语句找到
                    File file = new File(path+filename);//创建文件用这个名字
                    if(file.exists()){//存在就删除，不存在就说明 当前状态是 未开始 就被取消了
                        file.delete();
                    }
                    getNotificationManager().cancel(DOWNLOAD);//这个方法是 通知被取消
                    stopForeground(true);//移除前台服务
                }
            }else{//说明当前下载状态是downloadTask ！= null正开始之后在下载中
                downloadTask.cancelDownload();
               // stopForeground();
            }
        }
    }
    private Notification getNotification(String title,int progress){//通知，为了简化代码
        Intent i = new Intent(DownloadService.this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(DownloadService.this
                ,0,i,0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(title);//每个调用的地方可能title不同，所以作为参数传入
        builder.setContentIntent(pi);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setWhen(System.currentTimeMillis());
        if(progress > 0){//进度
            builder.setContentText(progress+"%");
            builder.setProgress(100,progress,false);//（最大，现在进度，是否模糊化）
        }
        return builder.build();
    }

}
