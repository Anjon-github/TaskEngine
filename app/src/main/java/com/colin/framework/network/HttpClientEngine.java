package com.colin.framework.network;

import android.content.Context;

import com.android.internal.http.multipart.FilePart;
import com.android.internal.http.multipart.MultipartEntity;
import com.android.internal.http.multipart.Part;
import com.android.internal.http.multipart.StringPart;
import com.colin.framework.Response.Response;
import com.colin.framework.cache.Cache;
import com.colin.framework.cache.CustomCookieStore;
import com.colin.framework.cache.DiskBasedCache;
import com.colin.framework.request.DownloadRequest;
import com.colin.framework.request.FileUploadRequest;
import com.colin.framework.request.HttpGetRequest;
import com.colin.framework.request.HttpPostRequest;
import com.colin.framework.request.HttpRequest;
import com.colin.framework.request.Request;
import com.colin.framework.task.ByteArrayPool;
import com.colin.framework.task.PoolingByteArrayOutputStream;
import com.colin.framework.task.TrustAllSSLSocketFactory;
import com.colin.framework.utils.FileUtils;
import com.colin.framework.utils.PalLog;
import com.colin.framework.utils.ProtocolUtil;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

//import org.apache.http.entity.mime.MultipartEntity;
//import org.apache.http.entity.mime.content.FileBody;
//import org.apache.http.entity.mime.content.StringBody;

/**
 * Created by xhm on 16-12-3.
 */

public class HttpClientEngine implements NetworkInterface{

    private static final String TAG = HttpClientEngine.class.getSimpleName();

    private static final int MAX_BYTE_SIZE = 3000;

    private final Object mLock = new Object();

    private Context mAppCon;

    private DefaultHttpClient mHttpClient;
    private HttpContext mHttpContext;
    private DiskBasedCache mCache;

    private ByteArrayPool mBytePool;

    public HttpClientEngine(Context con){
        mAppCon = con.getApplicationContext();
        mBytePool = new ByteArrayPool(MAX_BYTE_SIZE);
    }

    HttpContext getHttpContext() {
        if (mHttpContext == null) {
            synchronized (mLock) {
                mHttpContext = new BasicHttpContext();
                mHttpContext.setAttribute(ClientContext.COOKIE_STORE, CustomCookieStore.getInstance(mAppCon));
            }
        }
        return mHttpContext;
    }

    public void clearCookieStore(){
        if(mHttpClient != null){
            if(mHttpClient.getCookieStore() != null){
                mHttpClient.getCookieStore().clear();
            }
        }
        Object obj;
        if(mHttpContext != null
                && (obj = mHttpContext.getAttribute(ClientContext.COOKIE_STORE)) != null
                && obj instanceof CustomCookieStore){
            ((CustomCookieStore)obj).clear();
        }
    }

    public void shutDown(){
        clearCookieStore();
//        if(mHttpClient != null){
//			mHttpClient.getConnectionManager().shutdown();
//			mHttpClient = null;
//		}
    }

    synchronized HttpClient getHttpClient(){
        if(null == mHttpClient){
//            synchronized (TaskEngine.class) {
//                if(null == mHttpClient){
                    mHttpClient = newHttpClient();
//                }
//            }
        }
        return mHttpClient;
    }

    public Cache getCache(){
        if(mCache == null){
            synchronized (mLock) {
                mCache = new DiskBasedCache(FileUtils.getHttpCacheDir(mAppCon));
                mCache.initialize();
            }
        }
        return mCache;
    }

    private DefaultHttpClient newHttpClient() {
        HttpParams params = new BasicHttpParams();
        params.setParameter(ClientPNames.ALLOW_CIRCULAR_REDIRECTS, true);
        HttpProtocolParams.setUserAgent(params, "Android");
        HttpConnectionParams.setConnectionTimeout(params, HttpConstants.CONNECT_TIME_OUT);

        //在每个请求体中设置了
//		HttpConnectionParams.setSoTimeout(params, 40 * 1000);
        HttpConnectionParams.setSocketBufferSize(params, 500 * 1024);
        HttpConnectionParams.setTcpNoDelay(params, true);//
        HttpProtocolParams.setUseExpectContinue(params, true);
        ConnManagerParams.setMaxTotalConnections(params, 10);
        ConnPerRouteBean connPerRoute = new ConnPerRouteBean(10);
        ConnManagerParams.setMaxConnectionsPerRoute(params, connPerRoute);

        SchemeRegistry schReg = new SchemeRegistry();
        schReg.register(new Scheme("http", PlainSocketFactory
                .getSocketFactory(), 80));
        schReg.register(new Scheme("https", TrustAllSSLSocketFactory
                .getDefault(),443));
        ClientConnectionManager conMgr = new ThreadSafeClientConnManager(
                params, schReg);

        return new DefaultHttpClient(conMgr, params);
    }

    private HttpPost httpPost(HttpPostRequest req) {
        HttpPost httpPost = new HttpPost(req.getRequestUrl());
        addHeader(req, httpPost);
        httpPost.setEntity(new ByteArrayEntity(req.getPostData()));
        return httpPost;
    }

    private HttpGet httpGet(HttpGetRequest req) {
        HttpGet httpGet = new HttpGet(req.getRequestUrl());
        addHeader(req, httpGet);
        return httpGet;
    }

    private HttpGet httpDownload(DownloadRequest req) {
        HttpGet httpGet = new HttpGet(req.getRequestUrl());
        addHeader(req, httpGet);
        return httpGet;
    }

    protected HttpPost httpUpload(final FileUploadRequest req){
        File file = new File(req.getFilePath());

        Map<String, String> postData = req.getPostData();
        Set<String> keySet = postData.keySet();
        Iterator<String> keys = keySet.iterator();
//        Part[] parts = new Part[postData.size() + 1];
        List<Part> partList = new ArrayList<>();
        try{
            while (keys.hasNext()) {
                String key = keys.next();
                String value = postData.get(key);
                StringPart part = new StringPart(key, value);
                partList.add(part);
//                entity.addPart(key, new StringBody(value));
            }
            FilePart filePart = new FilePart(req.getFileKey(), req.getFileName(), file, req.getMineType(), req.getCharset());
            partList.add(filePart);
//            entity.addPart(req.getFileKey(), new FileBody(file, req.getFileName(), req.getMineType(), null));
        }catch(Exception e){
            e.printStackTrace();
        }
        Part[] parts = new Part[partList.size()];
        partList.toArray(parts);
        CustomMultipartEntity entity = new CustomMultipartEntity(parts, new CustomMultipartEntity.OnUploadListener() {
            @Override
            public void onUploadProgress(int percent) {
//                notifyMessage(TaskConstants.MSG_UPLOAD_PERCENT, percent);
                req.onUploadProgress(percent);
            }
        });

        HttpPost hp = new HttpPost(req.getRequestUrl());
        hp.setEntity(entity);
        addHeader(req, hp);
        return hp;
    }

    private static void addHeader(HttpRequest req, HttpRequestBase httpMethod) {
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
                httpMethod.addHeader(key, value);
            }
        }
    }

    @Override
    public Response execute(Request req){
        Response rsp = readFromCache(req);
        if(rsp != null){
        }else{
            HttpUriRequest method = null;
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
                setHttpReqParam(method, req);
                rsp = execute(method, req);
            }
        }
        return rsp;
    }

    private Response readFromCache(Request req) {
        if (req.shouldCache()) {
            Cache cache = getCache();
            Cache.Entry entry = cache.get(req.getCacheKey());
            if (entry != null) {
                if (entry.isExpired()) {
                    req.setCacheEntry(entry);
                } else {
                    return new Response(Response.RspType.SUCC_TYPE, entry.data, req);

                    //不做新鲜读判断
//					boolean refreshNeeded = entry.refreshNeeded();
//					if (isComplete && !refreshNeeded) {
//						removeTask();
//					}
//					if (refreshNeeded) {
//						req.setCacheEntry(entry);
//					} else {
//						return true;
//					}
                }
            }
        }
        return null;
    }

    private void setHttpReqParam(HttpUriRequest uriReq, Request req) {
        HttpParams params = uriReq.getParams();
        HttpConnectionParams.setSoTimeout(params, req.getTimeOutMs());
        if(req instanceof HttpRequest){
            boolean suportRedirect = ((HttpRequest)req).isSupportRedirect();
            HttpClientParams.setRedirecting(params, suportRedirect);
        }
        uriReq.setParams(params);
    }

    private Response execute(HttpUriRequest uriReq, Request req) {
        Response rsp = new Response(req);
        req.setSendTime();
        try {
            HttpResponse httpRsp = getHttpClient().execute(uriReq, getHttpContext());
            int statusCode = httpRsp.getStatusLine().getStatusCode();
            rsp.setStatus(statusCode);
            rsp.setRspTime();

            if (statusCode == 200) {
                if(req instanceof DownloadRequest){
                    InputStream is = httpRsp.getEntity().getContent();
                    rsp.setRspStream(is);
                }else{
                    byte[] data = readData(httpRsp);
                    rsp.setRspType(Response.RspType.SUCC_TYPE);
                    rsp.setHeaders(transHeaders(httpRsp));
                    rsp.setRspData(data);
                }
                rsp.setContentLen(httpRsp.getEntity().getContentLength());
            }else{
                rsp.setRspType(Response.RspType.ERR_TYPE_SERVER);
            }
            closeResonse(httpRsp);
        } catch (IOException e) {
            ProtocolUtil.printExceptReq(req, e);
            e.printStackTrace();
            if (req != null && req.hasRetryCount()) {
                req.doRetry();
                execute(uriReq, req);
            } else {
                if(e instanceof SocketTimeoutException){
                    rsp = new Response(Response.RspType.ERR_TYPE_TIMEOUT, null, req);
                }else{
                    rsp = new Response(Response.RspType.ERR_TYPE_NETWORK, null, req);
                }
            }
        }
        ProtocolUtil.printReqLog(req);
        ProtocolUtil.printRspLog(rsp);
        return rsp;
    }

    private HeaderInternal[] transHeaders(HttpResponse httpRsp){
        Header[] headers = httpRsp.getAllHeaders();
        int len = headers.length;
        HeaderInternal[] headersInternal = new HeaderInternal[len];
        for(int i = 0; i < len; i++){
            Header header = headers[i];
            headersInternal[i] = new HeaderInternal(header.getName(), header.getValue());
        }
        return headersInternal;
    }

    private byte[] readData(HttpResponse httpRsp) throws IOException {
        InputStream is = httpRsp.getEntity().getContent();

        PoolingByteArrayOutputStream baos = new PoolingByteArrayOutputStream(mBytePool);

        byte[] buff = new byte[1024];
        int len = -1;
        while ((len = is.read(buff)) != -1) {
            baos.write(buff, 0, len);
        }
        baos.flush();
        byte[] datas = baos.toByteArray();
        baos.close();
        return datas;
    }

    private void saveCache(Request request, byte[] data, Map<String, String> headers) {
        if(request == null)return;
        if (request.shouldCache()) {
            Cache.Entry entry = parseCacheHeaders(data, headers);
            if(entry != null){
                getCache().put(request.getCacheKey(), entry);
            }
        }
    }

    private Map<String, String> convertHeaders(HttpResponse httpRsp) {
        Header[] headers = httpRsp.getAllHeaders();
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < headers.length; i++) {
            result.put(headers[i].getName(), headers[i].getValue());
        }
        return result;
    }

    public static Cache.Entry parseCacheHeaders(byte[] data, Map<String, String> headers) {
        long now = System.currentTimeMillis();

        long serverDate = 0;
        long lastModified = 0;
        long serverExpires = 0;
        long softExpire = 0;
        long finalExpire = 0;
        long maxAge = 0;
        long staleWhileRevalidate = 0;
        boolean hasCacheControl = false;
        boolean mustRevalidate = false;

        String serverEtag;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = parseDateAsEpoch(headerValue);
        }

        headerValue = headers.get("Cache-Control");
        if (headerValue != null) {
            hasCacheControl = true;
            String[] tokens = headerValue.split(",");
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i].trim();
                if (token.equals("no-cache") || token.equals("no-store")) {
                    return null;
                } else if (token.startsWith("max-age=")) {
                    try {
                        maxAge = Long.parseLong(token.substring(8));
                    } catch (Exception e) {
                    }
                } else if (token.startsWith("stale-while-revalidate=")) {
                    try {
                        staleWhileRevalidate = Long.parseLong(token.substring(23));
                    } catch (Exception e) {
                    }
                } else if (token.equals("must-revalidate")
                        || token.equals("proxy-revalidate")) {
                    mustRevalidate = true;
                }
            }
        }

        headerValue = headers.get("Expires");
        if (headerValue != null) {
            serverExpires = parseDateAsEpoch(headerValue);
        }

        headerValue = headers.get("Last-Modified");
        if (headerValue != null) {
            lastModified = parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        // Cache-Control takes precedence over an Expires header, even if both
        // exist and Expires
        // is more restrictive.
        if (hasCacheControl) {
            softExpire = now + maxAge * 1000;
            finalExpire = mustRevalidate ? softExpire : softExpire + staleWhileRevalidate * 1000;
        } else if (serverDate > 0 && serverExpires >= serverDate) {
            // Default semantic for Expire header in HTTP specification is
            // softExpire.
            softExpire = now + (serverExpires - serverDate);
            finalExpire = softExpire;
        }

        Cache.Entry entry = new Cache.Entry();
        entry.data = data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = finalExpire;
        entry.serverDate = serverDate;
        entry.lastModified = lastModified;
        entry.responseHeaders = headers;

        return entry;
    }

    public static long parseDateAsEpoch(String dateStr) {
        try {
            // Parse date in RFC1123 format if this header contains one
            return DateUtils.parseDate(dateStr).getTime();
        } catch (DateParseException e) {
            // Date in invalid format, fallback to 0
            return 0;
        }
    }

    private void closeResonse(HttpResponse httpRsp) {
        if (httpRsp != null && httpRsp.getEntity() != null) {
            try {
                InputStream is = httpRsp.getEntity().getContent();
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static class CustomMultipartEntity extends MultipartEntity {

        public interface OnUploadListener{
            void onUploadProgress(int percent);
        }

        private OnUploadListener mListener;

        public CustomMultipartEntity(Part[] parts, OnUploadListener listener) {
            super(parts);
            mListener = listener;
        }

        @Override
        public void writeTo(OutputStream outstream) throws IOException {
            super.writeTo(new CountingOutputStream(outstream, getContentLength()));
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

}
