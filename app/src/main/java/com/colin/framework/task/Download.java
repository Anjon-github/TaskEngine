package com.colin.framework.task;

import android.content.Context;

import com.colin.framework.Response.Response;
import com.colin.framework.network.NetworkInterface;
import com.colin.framework.network.OkHttpEngine;
import com.colin.framework.request.DownloadRequest;
import com.colin.framework.request.HttpRequest;
import com.colin.framework.utils.FileUtils;

import org.apache.http.client.ClientProtocolException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 下载类
 * 
 * @author xionghoumiao
 * @date 2014-5-30下午4:54:46
 */
public class Download {

	private static final String TAG = "Download";

	private static final String TMP = ".tmp";

	Context mCon;
	NetworkInterface mHttpClient;
	String mUrl;
	String mFilePath;

	boolean cancel;
	boolean mSupportBreakPoint;
	boolean mIfExistDelete;
	DownloadListener mListener;
	Map<String, String> mHeadMap;

	public Download(Context con, String url, NetworkInterface client, String filePath) {
		if (url == null || url.length() == 0) {
			throw new NullPointerException("Url must not be null!");
		}
		mUrl = url;
		mCon = con;

		if (client == null) {
			mHttpClient = getDefaultClient(con);
		} else {
			mHttpClient = client;
		}

		if (filePath == null || filePath.length() == 0) {
			mFilePath = FileUtils.getDownloadFile(mCon, url).getPath();
		} else {
			mFilePath = filePath;
		}
	}

	/**
	 * set support breakpoint download
	 * 
	 * @param breakPoint
	 */
	public void setSupportBreakPoint(boolean breakPoint) {
		mSupportBreakPoint = breakPoint;
	}

	public void doCancel() {
		cancel = true;
	}

	/**
	 * if the remote source has downloaded to local, thus delete local file and
	 * download again
	 * 
	 * @param deleteExist
	 */
	public void setIfExistDelete(boolean deleteExist) {
		mIfExistDelete = deleteExist;
	}

	public void setDownloadListener(DownloadListener listener) {
		mListener = listener;
	}

	public void setHeadMap(Map<String, String> map) {
		mHeadMap = map;
	}

	public void addHead(String key, String value) {
		if (mHeadMap == null) {
			mHeadMap = new HashMap<String, String>();
		}
		mHeadMap.put(key, value);
	}

	/**
	 * 抛出异常下载
	 * 
	 * @throws Exception
	 */
	public void startThrowExcept() throws Exception {
		cancel = false;
		download(true);
	}

	/**
	 * 开始下载
	 */
	public void start() {
		try {
			cancel = false;
			download(false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			notifyFailed(-1, e.getMessage());
		}
	}

	private void download(boolean throwExcept) throws Exception {
		mkDirs();

		File saveFile = new File(mFilePath);
		if (saveFile.exists()) {
			if (mIfExistDelete) {
				// saveFile.delete();
			} else {
				notifySuccess(saveFile.getPath());
				return;
			}
		}

		String url = mUrl;
		File tmpFile = new File(mFilePath + TMP);
//		HttpGet get = new HttpGet(url);
		DownloadRequest req = new DownloadRequest(url);
		addHeadToMethod(req);
		breakPoint(tmpFile, req);

		InputStream is = null;
		FileOutputStream fos = null;
		try {
			Response rsp = mHttpClient.execute(req);
			int statesCode = rsp.getStatus();
			if (statesCode == 200) {
				is = rsp.getRspStream();
				if (cancel) {
					is.close();
					is = null;
					notifyCancel();
				} else {
					int length = (int) rsp.getContentLen();
					fos = new FileOutputStream(tmpFile);

					byte[] buff = new byte[1024 * 2];
					int len = -1;
					int sum = 0;

					while ((len = is.read(buff)) != -1 && sum <= length
					        && !cancel) {
						sum += len;
						fos.write(buff, 0, len);
						fos.flush();
						notifyProgress(sum * 100 / length);
					}

					is.close();
					fos.close();
					is = null;
					fos = null;

					if (cancel) {
						notifyCancel();
					} else {
						tmpFile.renameTo(saveFile);
						notifySuccess(saveFile.getPath());
					}
				}
			} else {
				notifyFailed(statesCode, null);
			}
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throwExceptOrNotifyError(throwExcept, e);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throwExceptOrNotifyError(throwExcept, e);
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
			throwExceptOrNotifyError(throwExcept, e);
		} finally {
			if (is != null) {
				try {
					is.close();
					is = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
				}
			}
			if (fos != null) {
				try {
					fos.close();
					fos = null;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private void throwExceptOrNotifyError(boolean throwExcept, Exception e)
	        throws Exception {
		if (throwExcept) {
			throw e;
		} else {
			notifyFailed(-1, e.getMessage());
		}
	}

	private void mkDirs() {
		int index = mFilePath.lastIndexOf(File.separator);
		String dirPath = mFilePath.substring(0, index);
		File dir = new File(dirPath);

		if (!dir.exists()) {
			dir.mkdirs();
		}
	}

	private void breakPoint(File tmpFile, HttpRequest get) {
		if (mSupportBreakPoint) {
			if (tmpFile.exists()) {
				String range = "bytes=" + tmpFile.length() + "-";
				get.addHeader("RANGE", range);
			}
		} else {
			if (tmpFile.exists()) {
				tmpFile.delete();
			}
		}
	}

	private void addHeadToMethod(HttpRequest get) {
		if (mHeadMap != null && mHeadMap.size() != 0) {
			Iterator<String> keyIter = mHeadMap.keySet().iterator();
			String name = null;
			String value = null;
			while (keyIter.hasNext()) {
				name = keyIter.next();
				value = mHeadMap.get(name);
				get.addHeader(name, value);
			}
		}
	}

	private void notifyCancel() {
		if (mListener != null) {
			mListener.onCancel();
		}
	}

	private void notifySuccess(String filePath) {
		if (mListener != null) {
			mListener.onSuccess(filePath);
		}
	}

	private void notifyFailed(int errCode, String errMsg) {
		if (mListener != null) {
			mListener.onFailed(errCode, errMsg);
		}
	}

	int mPercent;

	private void notifyProgress(int percent) {
		if(percent - mPercent > 5){
			mPercent = percent;

			if (mListener != null) {
				mListener.onProgress(percent);
			}
		}
	}

	public static interface DownloadListener {

		public void onProgress(int percent);

		public void onSuccess(String filePath);

		public void onCancel();

		public void onFailed(int errCode, String errMsg);

	}

	private NetworkInterface getDefaultClient(Context con) {
		return new OkHttpEngine(con, false);
	}


}
