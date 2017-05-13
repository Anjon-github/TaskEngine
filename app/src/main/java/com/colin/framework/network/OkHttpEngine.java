package com.colin.framework.network;

import android.content.Context;

import com.colin.framework.Response.Response;
import com.colin.framework.cache.Cache;
import com.colin.framework.cache.OkCookieStore;
import com.colin.framework.request.DownloadRequest;
import com.colin.framework.request.FileUploadRequest;
import com.colin.framework.request.HttpGetRequest;
import com.colin.framework.request.HttpPostRequest;
import com.colin.framework.request.HttpRequest;
import com.colin.framework.utils.PalLog;
import com.colin.framework.utils.ProtocolUtil;

import org.apache.http.impl.cookie.DateUtils;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Created by xhm on 16-12-3.
 */

public class OkHttpEngine implements NetworkInterface{

    OkCookieStore mCookieStore;
    OkHttpClient okHttpClient;
    Context mContext;

    public OkHttpEngine(Context con, boolean needCookieStore){
        mContext = con;
        mCookieStore = OkCookieStore.getInstance(con);
        okHttpClient = newBuilder(true, needCookieStore);
    }

    private OkHttpClient newBuilder(boolean supportRedirects, boolean needCookieStore){
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .followRedirects(supportRedirects)
            .followSslRedirects(supportRedirects)
            .sslSocketFactory(createSSLSocketFactory())
            .hostnameVerifier(new TrustAllHostnameVerifier())
            .readTimeout(HttpConstants.READ_TIME_OUT, TimeUnit.MILLISECONDS)
            .connectTimeout(HttpConstants.CONNECT_TIME_OUT, TimeUnit.MILLISECONDS)
            .writeTimeout(HttpConstants.WRITE_TIME_OUT, TimeUnit.MILLISECONDS);

        if(needCookieStore){
            builder.cookieJar(mCookieStore);
        }

        return builder.build();
    }

    public static Request httpGet(HttpGetRequest req) {
        Request.Builder builder = new Request.Builder();
        builder.url(req.getRequestUrl());
        addHeader(builder, req);
        return builder.build();
    }

    public static Request httpPost(HttpPostRequest req) {
        Request.Builder builder = new Request.Builder();
        builder.url(req.getRequestUrl());
        builder.post(RequestBody.create(null, req.getPostData()));
        addHeader(builder, req);
        return builder.build();
    }

    public static Request httpDownload(DownloadRequest req) {
        Request.Builder builder = new Request.Builder();
        builder.url(req.getRequestUrl());
        addHeader(builder, req);
        return builder.build();
    }

    public static Request httpUpload(final FileUploadRequest req) {
        MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
        bodyBuilder.setType(MultipartBody.FORM);
        Map<String,String> strMap = req.getPostData();
        Iterator<String> keyIter = strMap.keySet().iterator();
        while (keyIter.hasNext()){
            String key = keyIter.next();
            String value = strMap.get(key);
            bodyBuilder.addFormDataPart(key, value);
        }
        RequestBody fileBody = RequestBody.create(MediaType.parse(req.getMineType()), new File(req.getFilePath()));
        bodyBuilder.addFormDataPart(req.getFileKey(), req.getFileName(), fileBody);

        Request.Builder builder = new Request.Builder();
        builder.url(req.getRequestUrl());
        builder.post(new CustomMultipartBody(new CustomMultipartBody.OnUploadListener() {
            @Override
            public void onUploadProgress(int percent) {
                req.onUploadProgress(percent);
            }
        },bodyBuilder.build()));
        addHeader(builder, req);
        return builder.build();
    }

    private static void addHeader(Request.Builder builder, HttpRequest req){
        Map<String, String> headers = req.getHeaders();
        Cache.Entry entry;
        if ((entry = req.getCacheEntry()) != null) {
            if (entry.etag != null) {
                headers.put("If-None-Match", entry.etag);
            }
            if (entry.lastModified > 0) {
                Date refTime = new Date(entry.lastModified);
                headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
            }
        }

        if (headers != null && headers.size() != 0) {
            Set<String> keySet = headers.keySet();
            Iterator<String> keys = keySet.iterator();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = headers.get(key);
                builder.addHeader(key, value);
            }
        }
    }

    @Override
    public Response execute(com.colin.framework.request.Request req) {
        Response rsp = null;
        Request method = null;
        if (req instanceof HttpGetRequest) {
            HttpGetRequest httpReq = (HttpGetRequest) req;
            method = httpGet(httpReq);
        }else if(req instanceof HttpPostRequest){
            HttpPostRequest httpReq = (HttpPostRequest) req;
            method = httpPost(httpReq);
        }else if(req instanceof FileUploadRequest){
            FileUploadRequest httpReq = (FileUploadRequest) req;
            method = httpUpload(httpReq);
        }else if(req instanceof DownloadRequest){
            DownloadRequest httpReq = (DownloadRequest) req;
            method = httpDownload(httpReq);
        }
        if(method != null){
            rsp = execute(method, req);
        }
        return rsp;
    }

    private OkHttpClient getOkHttpClient(com.colin.framework.request.Request req){
        if(req instanceof HttpRequest){
            HttpRequest httpReq = (HttpRequest)req;
            if(!httpReq.isSupportRedirect()){
                return newBuilder(false, true);
            }
        }
        return okHttpClient;
    }

    private Response execute(Request uriReq, com.colin.framework.request.Request req) {
        Response locRsp = new Response(req);
        req.setSendTime();

        try {
            okhttp3.Response okRsp = getOkHttpClient(req).newCall(uriReq).execute();
            int code = okRsp.code();
            locRsp.setStatus(code);
            locRsp.setRspTime();
            if(code == 200){
                if(req instanceof DownloadRequest){
                    InputStream is = okRsp.body().byteStream();
                    locRsp.setRspStream(is);
                }else{
                    locRsp.setRspType(Response.RspType.SUCC_TYPE);
                    locRsp.setRspData(okRsp.body().bytes());
                    locRsp.setHeaders(transHeaders(okRsp));
                }
                locRsp.setContentLen(okRsp.body().contentLength());
            }else{
                locRsp.setRspType(Response.RspType.ERR_TYPE_SERVER);
            }
        } catch (IOException e) {
            ProtocolUtil.printExceptReq(req, e);
            e.printStackTrace();
            if (req != null && req.hasRetryCount()) {
                req.doRetry();
                execute(uriReq, req);
            } else {
                if(e instanceof SocketTimeoutException){
                    locRsp.setRspType(Response.RspType.ERR_TYPE_TIMEOUT);
                }else{
                    locRsp.setRspType(Response.RspType.ERR_TYPE_NETWORK);
                }
            }
        }
        ProtocolUtil.printReqLog(req);
        ProtocolUtil.printRspLog(locRsp);
        return locRsp;
    }

    private HeaderInternal[] transHeaders(okhttp3.Response httpRsp){
        Headers headers = httpRsp.headers();
        int len = headers.size();
        HeaderInternal[] headersInternal = new HeaderInternal[len];
        for(int i = 0; i < len; i++){
            String name = headers.name(i);
            String value = headers.value(i);
            headersInternal[i] = new HeaderInternal(name, value);
        }
        return headersInternal;
    }

    @Override
    public void shutDown() {
    }

    @Override
    public void clearCookieStore() {
        if(okHttpClient != null && okHttpClient.cookieJar() instanceof OkCookieStore){
            ((OkCookieStore)okHttpClient.cookieJar()).clear();
        }
    }

    public static class CustomMultipartBody extends RequestBody {


        public interface OnUploadListener{
            void onUploadProgress(int percent);
        }

        private OnUploadListener mListener;
        private RequestBody mRelBody;
        //包装完成的BufferedSink
        private BufferedSink bufferedSink;

        public CustomMultipartBody(OnUploadListener listener, RequestBody requestBody) {
            mListener = listener;
            mRelBody = requestBody;
        }

        @Override
        public MediaType contentType() {
            return mRelBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            return mRelBody.contentLength();
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
//            mRelBody.writeTo(bufferedSink);
            if (bufferedSink == null) {
                //包装
                bufferedSink = Okio.buffer(sink(sink));
            }
            //写入
            mRelBody.writeTo(bufferedSink);
            //必须调用flush，否则最后一部分数据可能不会被写入
            bufferedSink.flush();
        }

        private Sink sink(Sink sink) {
            return new ForwardingSink(sink) {
                //当前写入字节数
                long bytesWritten = 0L;
                //总字节长度，避免多次调用contentLength()方法
                long contentLength = 0L;

                int mLastPercent;

                @Override
                public void write(Buffer source, long byteCount) throws IOException {
                    super.write(source, byteCount);
                    if (contentLength == 0) {
                        //获得contentLength的值，后续不再调用
                        contentLength = contentLength();
                    }
                    //增加当前写入的字节数
                    bytesWritten += byteCount;
                    //回调
                    notifyUploadPercent();
                }

                private void notifyUploadPercent(){
                    if(mListener != null){
                        int percent = (int)(bytesWritten * 100 / contentLength);
                        if(percent - mLastPercent > 10 || percent == 100){
                            mLastPercent = percent;
                            mListener.onUploadProgress(percent);
                        }
                    }
                }
            };
        }



        class CountingOutputStream extends FilterOutputStream {

            private long mTransferred;
            private int mLastPercent;
            private long mContentLen;

            public CountingOutputStream(OutputStream out, long contentLen) {
                super(out);
                mTransferred = 0;
                mContentLen = contentLen;
                PalLog.d("CountOut", "contentLen " + contentLen);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
                mTransferred += len;
                notifyUploadPercent();
            }

            public void write(int b) throws IOException {
                out.write(b);
                mTransferred++;
                notifyUploadPercent();
            }

            private void notifyUploadPercent(){
                if(mListener != null){
                    int percent = (int)(mTransferred * 100 / mContentLen);
                    if(percent - mLastPercent > 10 || percent == 100){
                        mLastPercent = percent;
                        mListener.onUploadProgress(percent);
                    }
                }
            }
        }
    }

    private static SSLSocketFactory createSSLSocketFactory(){
        SSLSocketFactory sslSocketFactory = null;
        try{
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            sslSocketFactory = sc.getSocketFactory();
        }catch (Exception e){

        }
        return sslSocketFactory;
    }

    private static class TrustAllManager implements X509TrustManager{

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private static class TrustAllHostnameVerifier implements HostnameVerifier{

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }


}
