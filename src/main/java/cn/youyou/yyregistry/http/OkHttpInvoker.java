package cn.youyou.yyregistry.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpInvoker implements HttpInvoker {

    final static MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client;

    public OkHttpInvoker(int timeout) {
        this.client = new OkHttpClient.Builder()
                .connectionPool(new ConnectionPool(16, 60, TimeUnit.SECONDS))
                .readTimeout(timeout, TimeUnit.SECONDS)
                .writeTimeout(timeout, TimeUnit.SECONDS)
                .connectTimeout(timeout, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public String post(String requestString, String url) {
        log.debug(" ===> post  url = {}, requestString = {}", requestString, url);
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(requestString, JSONTYPE))
                .build();
        try {
            String respJson = client.newCall(request).execute().body().string();
            log.debug(" ===> respJson = " + respJson);
            return respJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String get(String url) {
        log.debug(" ===> get url = " + url);
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try {
            String respJson = client.newCall(request).execute().body().string();
            log.debug(" ===> respJson = " + respJson);
            return respJson;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
