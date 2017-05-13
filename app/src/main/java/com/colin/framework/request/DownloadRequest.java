package com.colin.framework.request;

public class DownloadRequest extends HttpRequest {
	
	private String savedFilePath;
	
	private boolean mSupportBreakPoint;//是否支持断点

	public DownloadRequest(String url) {
		super(url);
	}

	@Override
	public String getCacheKey() {
		return "";
	}
	
	public String getSaveFilePath(){
		return savedFilePath;
	}
	
	public void setSaveFilePath(String path){
		savedFilePath = path;
	}

	public boolean isSupportBreakPoint() {
		return mSupportBreakPoint;
	}

	public void setSupportBreakPoint(boolean mSupportBreakPoint) {
		this.mSupportBreakPoint = mSupportBreakPoint;
	}
	
	

}
