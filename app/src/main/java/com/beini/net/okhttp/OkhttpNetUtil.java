package com.beini.net.okhttp;

import android.os.Environment;
import android.util.Log;

import com.beini.util.BLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by beini on 2016/11/4.
 */

public class OkhttpNetUtil {

    private static OkhttpNetUtil okhttpInstance = new OkhttpNetUtil();
    private int cacheSize = 10 * 1024 * 1024; // 10 MiB
    private static File file = new File(Environment.getDataDirectory() + "/cacheBeini/");

    public static OkhttpNetUtil getInstance() {
        if (!file.exists()) {
            file.mkdir();
        }
        return okhttpInstance;
    }

    /**
     * 因为OkHttpClient有自己的连接池和线程池，使用的时候重用一个单例就可以了；闲置的连接和线程会被释放；
     * 也可以通过 client.dispatcher().executorService().shutdown();
     * client.connectionPool().evictAll();自己释放连接和线程
     */
    final OkHttpClient client = new OkHttpClient
            .Builder()
            .readTimeout(10, TimeUnit.SECONDS)//设置超时时间
            .cache(new Cache(file, cacheSize))//设置缓存
            //.proxy(proxy)//设置代理
            //.authenticator(authenticator)
            .build();

    /**
     * Http 缓存
     */
    public void httpCache() {
        String url = "http://www.cnblogs.com/whoislcj/p/5537640.html";
        final CacheControl.Builder builder = new CacheControl.Builder();
        builder.maxAge(10, TimeUnit.MILLISECONDS);
        CacheControl cache = builder.build();
        final Request request = new Request.Builder().cacheControl(cache).url(url).build();
        final Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("com.beini", e.toString());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String string = response.body().string();
                    long endTime = System.currentTimeMillis();
                    Log.d("com.beini", "response ----->" + string);
                }
            }
        });

    }

    /**
     * 获取网页内容  post
     */
    public void getWebData() {
        Log.d("com.beini", "---------------------->获取网页内容");
        String url = "http://10.0.0.48:8080/SpringMVC/addUser";
        FormBody.Builder formBoy = new FormBody.Builder();
        formBoy.add("userName", "beini");
        formBoy.add("password", "123456");
        Request request = new Request.Builder()
                .url(url)
                .post(formBoy.build())
                .build();
        //异步
        client.newCall(request).enqueue(new Callback() {
            /**
             * 触发条件：1 连接取消 2 连接问题  3 超时
             * @param call
             * @param e
             */
            @Override
            public void onFailure(Call call, IOException e) {

            }

            /**
             * The recipient of the callback may
             * consume the response body on another thread.
             * @param call
             * @param response
             */
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {//code >= 200 && code < 300;
                    try {
                        Log.d("com.beini", "----------------------->" + response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
//		同步
//		try {
//			Response mResponse = okHttpClient.newCall(request).execute();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
    }

    /**
     * get
     * 读取响应头
     */
    public void getHead() {
        Log.d("com.beini", "------------------>读取响应头");
        String url = "http://www.baidu.com";
        final Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(request);
        call.enqueue(new Callback() {//异步
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {//子线程
                response.body().string();//response.body().bytes();  response.body().byteStream();
                if (response.isSuccessful()) {
                    Headers responseHeaders = response.headers();
                    for (int i = 0; i < responseHeaders.size(); i++) {
                        BLog.d(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                }
            }
        });
        //		call.cancel();//取消
        //同步
        /**
         *
         * try {
         *      Response response = call.execute();
         *  } catch (IOException e) {
         *     e.printStackTrace();
         *  }
         */
    }
    /**
     * 下载  上传-----------------------------
     */
    /**
     * 文件  表单 上传
     */
    public void uploadFile() {
        String url = "http://10.0.0.48:8080/SpringMVC/upload";

        File file = new File(new StringBuilder().append(Environment.getExternalStorageDirectory() + "/A.xml").toString());

        RequestBody fileBody = RequestBody.create(MediaType.parse("image/png;charset=utf-8"), file);

        RequestBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("upFile", file.getName(), fileBody)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("com.beini", "--------------->e=" + e.getLocalizedMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("com.beini", "---------------------->onResponse");
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                Log.d("com.beini", "---------文件上传------------>" + response.body().string());
            }
        });

    }


    /**
     * 文件下载
     */
    public void downloadFile() {
        File file = new File(Environment.getExternalStorageDirectory() + "/OkhttpDemo");
        if (file.exists()) {
            file.mkdir();
        }
        Request request = new Request.Builder().url("http://h-bolin.imwork.net:14572/androiddownloadfile/111.pdf").build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    FileOutputStream fileOutputStream;
                    InputStream inputStream = response.body().byteStream();
                    try {
                        fileOutputStream = new FileOutputStream(new File("/sdcard/wangshu.jpg"));
                        byte[] buffer = new byte[2048];
                        int len;
                        while ((len = inputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, len);
                        }
                        fileOutputStream.flush();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    public void uploadbyMultipartBody() {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", "file")
                .addFormDataPart("image", "image1.jpg", RequestBody.create(MediaType.parse("image/png;"), new File("")))
                .build();
        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + "...")
                .url("")
                .post(requestBody)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });

    }

}
