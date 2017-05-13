package com.colin.framework.utils;

import android.text.TextUtils;

import com.colin.framework.Response.Response;
import com.colin.framework.network.HeaderInternal;
import com.colin.framework.request.HttpGetRequest;
import com.colin.framework.request.HttpPostRequest;
import com.colin.framework.request.HttpRequest;
import com.colin.framework.request.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by xhm on 16-8-19.
 */
public class ProtocolUtil {

    private static final String TAG = "ProUtil";

    public static final String UTF8_CHARSET = "utf-8";
    public static final String GBK_CHARSET = "GBK";


    public static Request createHttpRequest(String url){
        return createHttpRequest(url, "");
    }

    public static Request createHttpRequest(String url, Map<String, String> map){
        return createHttpRequest(url, map, UTF8_CHARSET);
    }

    public static Request createHttpRequest(String url, Map<String, String> map, String charset){
        if(TextUtils.isEmpty(charset)){
            charset = UTF8_CHARSET;
        }
        String postDataStr = mapToStr(map, charset);
        return createHttpRequest(url, postDataStr, charset);
    }

    public static Request createHttpRequest(String url, String postDataStr){
        return createHttpRequest(url, postDataStr, UTF8_CHARSET);
    }

    public static Request createHttpRequest(String url, String postDataStr, String charset){
        if(TextUtils.isEmpty(charset)){
            charset = UTF8_CHARSET;
        }
        HttpRequest req;
        if(postDataStr == null || postDataStr.length() == 0){
            req = new HttpGetRequest(url);
        }else{
            req = new HttpPostRequest(url, postDataStr.getBytes());
        }
        req.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
        req.setCharset(charset);
        return req;
    }

    public static String mapToStr(Map<String, String> map){
        return mapToStr(map, UTF8_CHARSET);
    }

    public static String mapToStr(Map<String, String> map, String charset){
        if(TextUtils.isEmpty(charset)){
            charset = UTF8_CHARSET;
        }
        if(map != null && map.size() != 0){
            StringBuilder sb = new StringBuilder();
            Iterator<String> keyIter = map.keySet().iterator();
            try {
                while(keyIter.hasNext()){
                    String key = keyIter.next();
                    String value = map.get(key);
                    if(!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)){
                        if(sb.length() != 0){
                            sb.append("&");
                        }
                        sb.append(URLEncoder.encode(key, charset));
                        sb.append("=");
                        sb.append(URLEncoder.encode(value, charset));
                    }
                }
            }catch (UnsupportedEncodingException e){
                e.printStackTrace();
            }
            return sb.toString();
        }
        return "";
    }

    public static void printReqLog(Request req){
        if(req == null || !req.isOpenLog())return;

        String charset = req.getCharset();
        try {
            PalLog.d(getTag(req), "reqUrl " + URLDecoder.decode(req.getRequestUrl(), charset));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        if(req instanceof HttpPostRequest){
            byte[] postData = ((HttpPostRequest) req).getPostData();
            if(postData != null && postData.length < 1024){
                String postDataStr = new String(postData);
                //上传内容小于2kb打印
                try {
                    PalLog.d(getTag(req), "postDataDecode " + URLDecoder.decode(postDataStr, charset));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void printExceptReq(Request req, Throwable throwable){
//        printReqLog(req);
        StringBuffer sb = new StringBuffer();
        while (throwable != null){
            sb.append(throwable.getMessage());
            throwable = throwable.getCause();
        }
        PalLog.e(getTag(req), sb.toString());
    }

    public static void printRspLog(Response rsp){
        if(rsp == null || !rsp.isOpenLog())return;

        String charset = rsp.getCharset();

        int rspStatus = rsp.getStatus();
        byte[] rspData = rsp.getRspData();
        PalLog.d(getTag(rsp), "rspStatus:" + rspStatus + "; excuteTime:" + rsp.calcExcuteTime());
        printRspheader(rsp);
        if(rspData != null){
            String rspStr = "";
            try {
                rspStr = new String(rsp.getRspData(), charset);

                String consoleStr = "";
                String fileStr = "";
                if(rspStr.startsWith("{")){
                    JSONObject jsonObj = new JSONObject(rspStr);
                    consoleStr = jsonObj.toString(2);
                    fileStr = jsonObj.toString();
                }else if(rspStr.startsWith("[")){
                    JSONArray jsonArray = new JSONArray(rspStr);
                    consoleStr = jsonArray.toString(2);
                    fileStr = jsonArray.toString();
                }else{
                    consoleStr = fileStr = rspStr;
                }

                if(rspData.length < 1024){
                    //小于10kb打印
                    PalLog.v(getTag(rsp), "rspJson " + consoleStr, PalLog.TO_CONSOLE);
                    PalLog.v(getTag(rsp), "rspJson " + fileStr, PalLog.TO_FILE);
                }else{
                    PalLog.v(getTag(rsp), "rspJson " + fileStr);
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                PalLog.e(getTag(rsp), "rspStr " + "UTF-8编码格式错误");
            }catch (JSONException e) {
                e.printStackTrace();
                PalLog.e(getTag(rsp), "rspStr " + rspStr);
            }

        }

    }

    private static void printRspheader(Response rsp){
//        if(!ConfigMgr.getInstance(KaihuApp.getContext()).isHeaderLogOpen())return;
        HeaderInternal[] headers = rsp.getHeaders();
        if (headers != null && headers.length != 0) {
            StringBuilder sb = new StringBuilder();
            for(HeaderInternal header : headers){
                sb.append(header.getName());
                sb.append(":");
                sb.append(header.getValue());
                sb.append(" ");
            }
            PalLog.d(getTag(rsp), "rspHeader:" + sb.toString());
        }
    }

    private static String getTag(Response rsp){
        return TAG + "/" + rsp.getLogTag();
    }

    private static String getTag(Request req){
        return TAG + "/" + req.getLogTag();
    }

}
