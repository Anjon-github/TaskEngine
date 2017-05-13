package com.colin.framework.request;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 
 * @author xionghoumiao@myhexin.com
 * 
 * @Time 2015-9-16 下午5:28:54
 */
public class FileUploadRequest extends HttpRequest {
	
	private String mFileKey;
	private String mFilePath;
	private String mFileName;
	private String mMineType;
	
	private Map<String, String> mPostData;
	
	public FileUploadRequest(String url){
		super(url);
		seTimeOutMs(50 * 1000);
		mPostData = new HashMap<>();
	}
	
	public void addPostData(String key, String value){
		if(TextUtils.isEmpty(key) || TextUtils.isEmpty(value))return;

		mPostData.put(key, value);
	}

	public void addPostData(Map<String, String> map){
		if(map != null && map.size() != 0){
			Iterator<String> keyIteror = map.keySet().iterator();
			while(keyIteror.hasNext()){
				String key = keyIteror.next();
				String value = map.get(key);
				addPostData(key, value);
			}
		}
	}
	
	public Map<String, String> getPostData(){
		return mPostData;
	}
	
	public void setFileData(String fileKey, String filePath){
		setFileData(fileKey, filePath, null, null);
	}

	public void setFileData(String fileKey, String filePath, String fileName, String mineType){
		mFileKey = fileKey;
		mFilePath = filePath;

		if(TextUtils.isEmpty(fileName)){
			int lastIndexSep = filePath.lastIndexOf("/");
			fileName = filePath.substring(lastIndexSep + 1);
		}
		mFileName = fileName;
		if(TextUtils.isEmpty(mineType)){
			mineType = "application/octet-stream";
		}
		mMineType = mineType;
	}
	
	public String getFileKey(){
		return mFileKey;
	}

	public String getMineType() {
		return mMineType;
	}

	public String getFileName() {
		return mFileName;
	}

	public String getFilePath(){
		return mFilePath;
	}

	private UploadInterface mInerface;

	public void setUploadInterface(UploadInterface inerface){
		mInerface = inerface;
	}

	public void onUploadProgress(int percent){
		if(mInerface != null){
			mInerface.onUploadProgress(percent);
		}
	}

	public interface UploadInterface{
		void onUploadProgress(int percent);
	}


	/**
	 * 文件上传不支持缓存
	 */
	@Override
	public final boolean shouldCache() {
		return false;
	}

	@Override
    public String getCacheKey() {
	    return "";
    }

	@Override
	public String toString(){
		if(mPostData != null){
			StringBuilder sb = new StringBuilder();
			Set<String> keyset = mPostData.keySet();
			for(String key : keyset){
				sb.append(key);
				sb.append("=");
				sb.append(mPostData.get(key));
				sb.append("&");
			}
			return sb.toString();
		}else{
			return super.toString();
		}
	}

}
