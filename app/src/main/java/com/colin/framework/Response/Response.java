package com.colin.framework.Response;

import com.colin.framework.network.HeaderInternal;
import com.colin.framework.request.Request;

import java.io.InputStream;

/**
 * Created by xhm on 16-9-9.
 */
public class Response {

    public enum RspType {

        SUCC_TYPE(0, "succ"),
        /** 网络超时 */
        ERR_TYPE_TIMEOUT(-2, "网络连接超时，请检查网络配置"),
        /** 网络中断 */
        ERR_TYPE_NETWORK(-3, "网络连接失败，请检查网络配置"),
        /** 服务异常（非200） */
        ERR_TYPE_SERVER(-4, "服务异常，请联系服务提供商(%d)");

        int mErrCode;
        String mErrInfo;

        RspType(int errCode, String errInfo){
            mErrCode = errCode;
            mErrInfo = errInfo;
        }

        public int getErrCode(){
            return mErrCode;
        }

        public String getErrInfo(){
            return mErrInfo;
        }
    }

    /**http status code*/
    private int mStatus;

    private RspType mRspType;

    private byte[] mRspData;

    private InputStream mRspStream;

    private HeaderInternal[] mHeaders;

    private long mContentLen;

    private Request mReq;

    private long mRspTime;

    public Response(Request req){
        mReq = req;
    }

    public Response(RspType rspType, byte[] rspData, Request req){
        setRspData(rspData);
        mReq = req;
        mRspType = rspType;
    }

    public Response(RspType rspType, int status, byte[] rspData, HeaderInternal[] headers, Request req){
        mRspType = rspType;
        mStatus = status;
        setRspData(rspData);
        setHeaders(headers);
        mReq = req;
    }

    public void setRspType(RspType rspType){
        mRspType = rspType;
    }

    public RspType getRspType(){
        return mRspType;
    }

    public boolean isSucc(){
        return mRspType == RspType.SUCC_TYPE;
    }

    public HeaderInternal[] getHeaders() {
        return mHeaders;
    }

    public boolean hasRspData(){
        return mRspData != null && mRspData.length != 0;
    }

    public void setHeaders(HeaderInternal[] headers) {
        if(headers != null && headers.length != 0){
            mHeaders = new HeaderInternal[headers.length];
            System.arraycopy(headers, 0, mHeaders, 0, headers.length);
        }
    }

    public byte[] getRspData() {
        return mRspData;
    }

    public void setRspData(byte[] rspData) {
        if(rspData != null && rspData.length != 0){
            mRspData = new byte[rspData.length];
            System.arraycopy(rspData, 0, mRspData, 0, rspData.length);
        }
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int status) {
        this.mStatus = status;
    }

    public Request getReq() {
        return mReq;
    }

    public void setReq(Request req) {
        this.mReq = req;
    }

    public boolean isOpenLog(){
        return mReq != null ? mReq.isOpenLog() : true;
    }

    public String getLogTag(){
        return mReq != null ? mReq.getLogTag() : "";
    }

    public String getCharset(){
        return mReq != null ? mReq.getCharset() : "utf-8";
    }

    public void setRspTime(){
        mRspTime = System.currentTimeMillis();
    }

    public long getRspTime(){
        return mRspTime;
    }

    public InputStream getRspStream() {
        return mRspStream;
    }

    public void setRspStream(InputStream rspStream) {
        this.mRspStream = rspStream;
    }

    public void setContentLen(long contentLen){
        mContentLen = contentLen;
    }

    public long getContentLen(){
        return mContentLen;
    }

    /**
     * 计算请求执行时间
     * @return
     */
    public String calcExcuteTime(){
        if(mReq != null){
            int time = (int)(mRspTime - mReq.getSendTime());
            return String.format("%dms", time);
        }
        return "";
    }

}
