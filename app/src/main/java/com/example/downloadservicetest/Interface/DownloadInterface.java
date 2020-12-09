package com.example.downloadservicetest.Interface;

public interface DownloadInterface {
    //接口的作用是：规范化代码
    //这几个方法是对结果的回应

    void OnProgress(int progress);//显示当前进度

    void OnSuccess();//结果是成功

    void OnFailed();

    void OnCanceled();

    void OnPaused();
}
