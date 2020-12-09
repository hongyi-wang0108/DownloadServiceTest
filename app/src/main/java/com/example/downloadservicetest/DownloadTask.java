package com.example.downloadservicetest;

import android.os.AsyncTask;
import android.os.Environment;

import com.example.downloadservicetest.Interface.DownloadInterface;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String,Integer,Integer> {//异步任务，处理耗时操作
    private DownloadInterface listener;//为了接收服务传入的监听器
   // private DownloadService downloadService;
    private int lastprogress;//最新进度
    public static final int TYPE_SUCEESS = 1;
    public static final  int TYPE_FAILED = 2;
    public static final  int TYPE_CANCELED = 3;
    public static final  int TYPE_PAUSED = 4;
    public DownloadTask(DownloadInterface listener) {//为了传入监听器做的有参
        this.listener = listener;
    }

    private boolean isPause = false;//先定义为假
    public void pauseDownload() {//当异步任务在服务中调用了这个方法时，说明main活动按了btn_pause
        isPause = true;//在doInBackground下载任务中就可以暂停了，暂时就不用下载了
    }
    private boolean isCancel = false;//先定义为假
    public void cancelDownload() {//当异步任务在服务中调用了这个方法时，说明main活动按了btn_cancel
        isCancel = true;//在doInBackground下载任务中就可以取消了，就不用下载了
    }

    @Override
    protected Integer doInBackground(String... strings) {
        InputStream is = null;//电脑<----别的  所以用is
        RandomAccessFile randomAccessFile = null;//这个是保存下载的文件
        File file = null;//这个是为了判断的文件
        try {
            String downurl = strings[0];//下载地址
            String downurlname = downurl.substring(downurl.lastIndexOf("/"));//名字
            String path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();//

            file = new File(path + downurlname);
            long correntlength = 0;//当前文件长度
            if(file.exists()){
                //判断之前是否下载过一点，获取当前长度
                correntlength = file.length();
            }//没有文件的话，correntlength就还是0

            long successlength = 0;//成功的文件长度，后面在赋正确的值用OkHttpClient
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(downurl)
                    .build();
            Response response = okHttpClient.newCall(request).execute();
            if(response != null && response.isSuccessful()){
                successlength = response.body().contentLength();//得到成功的文件长度
                response.close();//关流
            }
            //现在成功的文件长度不为0了

            //对长度进行判断
            if(correntlength == successlength){//下载完了
                return TYPE_SUCEESS;
            }else if(successlength == 0){//没获取的到正确的下载地址，下载的东西没获取到
                return TYPE_FAILED;
            }

            //正常下载中 的情况
            OkHttpClient okHttpClient1 = new OkHttpClient();
            Request request1 = new Request.Builder()
                    .url(downurl)
                    .addHeader("RANGE","bytes="+correntlength+"-")//断点处下载
                    .build();
            Response response1 = okHttpClient1.newCall(request1).execute();
            if(response1 != null){//说明获取到了正确的response，可以正常下载了
                is = response1.body().byteStream();
                randomAccessFile = new RandomAccessFile(file,"rw");
                randomAccessFile.seek(correntlength);//找到当前位置下载

                //下载
                long total = 0;//这次下载的总长度（不包括correntlength）。每次读取的长度是不一样的
                int len = 0;
                byte[] arr = new byte[1024];
                while ((len = is.read(arr)) != -1){
                    if(isPause){//点了btn_pause
                        return  TYPE_PAUSED;
                    }else if(isCancel){//点了btn_cancel
                        return TYPE_CANCELED;
                    }else{//啥也没点正常运行
                        total += len;
                        randomAccessFile.write(arr,0,len);//写入
                        int progress = (int) ((total + correntlength) *100 / successlength);
                       // listener.OnProgress(progress);
                        publishProgress(progress);//发布进度，这个会自动调用onProgressUpdate方法
                    }
                }
                //下载完了后
                response1.body().close();//关流
                return TYPE_SUCEESS;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }finally{//最后
            try {
                if(is != null){//关流
                    is.close();
                }
                if(randomAccessFile != null){//关流
                    randomAccessFile.close();
                }
                if(isCancel && file != null){//点击btn_cancel的，删除文件
                    file.delete();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
        return TYPE_FAILED ;
    }

    @Override
    protected void onPostExecute(Integer integer) {//对doInBackground的return的处理
        super.onPostExecute(integer);
        switch (integer){//创造新通知，接口通知方法在service里面
            case TYPE_SUCEESS:
                listener.OnSuccess();
                break;
            case TYPE_FAILED:
                listener.OnFailed();
                break;
            case TYPE_CANCELED:
                listener.OnCanceled();
                //break;
            case TYPE_PAUSED:
                listener.OnPaused();//
                break;
            default:
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        int progress = values[0];//values[0]中存在传过来的参数publishProgress(progress);
        if(progress > lastprogress){//!!!
            //更新通知，接口方法在service里面
            //为什么在service中是因为，他要对异步任务进行null，在本类的重写的话不能进行这个操作
            listener.OnProgress(progress);//发布任务，服务的OnProgress那边会更新通知
            lastprogress = progress;//最新进度更新为 传来的最新进度
        }
    }
}
