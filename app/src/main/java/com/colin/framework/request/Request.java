package com.colin.framework.request;

import com.colin.framework.cache.Cache;
import com.colin.framework.network.HttpConstants;


/**
 * 网络请求抽象类
 * 
 * @author xionghoumiao
 * @date 2014-8-12上午9:16:06
 */
public abstract class Request {

	/** The default number of retries */
	public static final int DEFAULT_MAX_RETRIES = 0;

	private static short REQ_ID;

	private short mReqId;
	private String mUrl;
	private boolean mShouldCache;
	
	private Cache.Entry mCacheEntry;

	/** 编码格式 */
	private String charset = "UTF-8";

	/** 超时时间 */
	private int mTimeOutMs = HttpConstants.WRITE_TIME_OUT;

	/** 重试次数 */
	private int mRetryCount = DEFAULT_MAX_RETRIES;

	private boolean mOpenLog = true;

	private String mLogTag;

	private long mSendTime;

	private RequestListener mListener;

	public Request(String url) {
		mReqId = getNextReqId();
		mUrl = url;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void seTimeOutMs(int timeOutMs) {
		mTimeOutMs = timeOutMs;
	}

	public void setRetryCount(int retryCount) {
		mRetryCount = retryCount;
	}

	/**
	 * 是否有重试机会
	 * 
	 * @return
	 */
	public boolean hasRetryCount() {
		return mRetryCount > 0;
	}

	public void doRetry() {
		mRetryCount--;
	}

	public int getTimeOutMs() {
		return mTimeOutMs;
	}

	public int getRetryCount() {
		return mRetryCount;
	}

	public String getRequestUrl() {
		return mUrl;
	}

	public short getRequestId() {
		return mReqId;
	}

	/**
	 * Set whether or not responses to this request should be cached.
	 * 
	 * @return This Request object to allow for chaining.
	 */
	public Request setShouldCache(boolean shouldCache) {
		mShouldCache = shouldCache;
		return this;
	}

	/**
	 * Returns true if responses to this request should be cached.
	 */
	public boolean shouldCache() {
		return mShouldCache;
	}
	
	public void setCacheEntry(Cache.Entry entry) {
        mCacheEntry = entry;
    }

    /**
     * Returns the annotated cache entry, or null if there isn't one.
     */
    public Cache.Entry getCacheEntry() {
        return mCacheEntry;
    }

	public boolean isOpenLog() {
		return mOpenLog;
	}

	public Request setOpenLog(boolean openLog) {
		mOpenLog = openLog;
		return this;
	}

	public Request setLogTag(String logTag) {
		mLogTag = logTag;
		return this;
	}

	public String getLogTag(){
		return mLogTag != null ? mLogTag : "";
	}

	public void setListener(RequestListener listener){
		mListener = listener;
	}

	public RequestListener getListener(){
		return mListener;
	}

	public long getSendTime() {
		return mSendTime;
	}

	public void setSendTime() {
		this.mSendTime = System.currentTimeMillis();
	}

	public abstract String getCacheKey();


	private synchronized short getNextReqId() {
		if (REQ_ID >= Short.MAX_VALUE) {
			REQ_ID = 0;
		}
		return REQ_ID++;
	}

	public static interface RequestListener{
		public void onSucc(String rspData);
		public void onError(int type, String errInfo);
	}
}
