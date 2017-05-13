package com.colin.framework.task;

import com.colin.framework.Response.Response;
import com.colin.framework.request.FileUploadRequest;
import com.colin.framework.request.Request;

/**
 * 网络请求任务
 * 
 * @author xionghoumiao
 * @date 2014-4-21下午6:28:14
 */
public abstract class NetworkTask extends MultPhaseTask {

	protected NetworkTask(int taskType, TaskEngine engine) {
		super(taskType, engine);
	}

	protected abstract boolean onSuccessResponse(Response response);

	protected boolean onErrorResponse(Response rsp) {
		Response.RspType rspType;
        int status = -1;
		if(rsp != null){
			rspType = rsp.getRspType();
            status = rsp.getStatus();
		}else{
			rspType = Response.RspType.ERR_TYPE_NETWORK;
		}
		int errType = rspType.getErrCode();
		String errInfo = rspType.getErrInfo();
        return onExecuteError(errType, String.format(errInfo, status));
	}

	protected void sendRequest(Request req) {
		addFileUploadListener(req);
		boolean complete;
		Response rsp = mTaskEngnie.getHttpEngine().execute(req);
		if(rsp != null && rsp.isSucc()){
			complete = onSuccessResponse(rsp);
		}else{
			complete = onErrorResponse(rsp);
		}
		if(complete){
			removeTask();
		}
	}

	private void addFileUploadListener(Request req){
		if(req instanceof FileUploadRequest){
			((FileUploadRequest)req).setUploadInterface(new FileUploadRequest.UploadInterface() {
				@Override
				public void onUploadProgress(int percent) {
					notifyMessage(MSG_UPLOAD_PERCENT, percent);
				}
			});
		}
	}
	
//	protected HttpPost httpUpload(FileUploadRequest req){
//		File file = new File(req.getFilePath());
//		CustomMultipartEntity entity = new CustomMultipartEntity(new OnUploadListener() {
//			@Override
//			public void onUploadProgress(int percent) {
//				notifyMessage(TaskConstants.MSG_UPLOAD_PERCENT, percent);
//			}
//		});
//
//		Map<String, String> postData = req.getPostData();
//		Set<String> keySet = postData.keySet();
//		Iterator<String> keys = keySet.iterator();
//		try{
//			while (keys.hasNext()) {
//				String key = keys.next();
//				String value = postData.get(key);
//				entity.addPart(key, new StringBody(value));
//			}
//			entity.addPart(req.getFileKey(), new FileBody(file, req.getFileName(), req.getMineType(), null));
//		}catch(UnsupportedEncodingException e){
//			e.printStackTrace();
//		}
//		HttpPost hp = new HttpPost(req.getRequestUrl());
//		hp.setEntity(entity);
//
//		return hp;
//	}

//	private boolean readFromCache(Request req) {
//		if (req.shouldCache()) {
//			Cache cache = mTaskEngnie.getHttpEngine().getCache();
//			Cache.Entry entry = cache.get(req.getCacheKey());
//			if (entry != null) {
//				if (entry.isExpired()) {
//					req.setCacheEntry(entry);
//				} else {
//					boolean isComplete = onSuccessResponse(new Response(entry.data, req));
//					if(isComplete){
//						removeTask();
//					}
//					return true;
//
//					//不做新鲜读判断
////					boolean refreshNeeded = entry.refreshNeeded();
////					if (isComplete && !refreshNeeded) {
////						removeTask();
////					}
////					if (refreshNeeded) {
////						req.setCacheEntry(entry);
////					} else {
////						return true;
////					}
//				}
//			}
//		}
//		return false;
//	}

//	private void setHttpReqParam(HttpUriRequest uriReq, Request req) {
//		HttpParams params = uriReq.getParams();
//		HttpConnectionParams.setSoTimeout(params, req.getTimeOutMs());
//		uriReq.setParams(params);
//	}

//	private void execute(HttpUriRequest uriReq, Request req) {
//		HttpResponse httpRsp = null;
//		// 是否完成
//		boolean isComplete = true;
//		try {
//			req.setSendTime();
//			httpRsp = mTaskEngnie.getHttpEngine().execute(uriReq);
//			isComplete = parseRsp(httpRsp, req);
//		} catch (NullPointerException e) {
//			ProtocolUtil.printExceptReq(req, e);
//			e.printStackTrace();
//			isComplete = onException(e);
//		} catch (java.net.SocketTimeoutException e) {
//			ProtocolUtil.printExceptReq(req, e);
//			e.printStackTrace();
//			if (req != null && req.hasRetryCount()) {
//				req.doRetry();
//				execute(uriReq, req);
//			} else {
//				isComplete = onExecuteError(ERR_TYPE_TIMEOUT, NETWORK_TIMEOUT);
//			}
//		} catch (Exception e) {
//			ProtocolUtil.printExceptReq(req, e);
//			e.printStackTrace();
//			isComplete = onExecuteError(ERR_TYPE_NETWORK, NETWORK_ERROR);
//		} finally {
//			closeResonse(httpRsp);
//		}
//		if (isComplete) {
//			removeTask();
//		}
//	}

//	private void closeResonse(HttpResponse httpRsp) {
//		if (httpRsp != null && httpRsp.getEntity() != null) {
//			try {
//				InputStream is = httpRsp.getEntity().getContent();
//				if (is != null) {
//					is.close();
//					is = null;
//				}
//			} catch (Exception e) {
//			}
//		}
//	}

//	protected boolean parseRsp(HttpResponse httpRsp, Request req)
//	        throws IOException {
//		int statusCode = httpRsp.getStatusLine().getStatusCode();
//		Map<String, String> responseHeaders = Collections.emptyMap();
//		responseHeaders = convertHeaders(httpRsp.getAllHeaders());
//		Header[] headers = httpRsp.getAllHeaders();
//		Response rsp = new Response(statusCode, null, headers, req);
//		rsp.setRspTime();
//
//		ProtocolUtil.printReqLog(req);
//
//		boolean succ = false;
//		if (statusCode == 200) {
//			succ = true;
//			if(req instanceof DownloadRequest){
//			}else{
//				byte[] data = readData(httpRsp);
//				saveCache(req, data, responseHeaders);
//				rsp.setRspData(data);
//			}
//		} else if (statusCode == HttpStatus.SC_NOT_MODIFIED) {
//			Entry entry = req.getCacheEntry();
//			if (entry != null) {
//				succ = true;
//				saveCache(req, entry.data, responseHeaders);
//				rsp.setRspData(entry.data);
//			}
//		}
//		ProtocolUtil.printRspLog(rsp);
//		closeResonse(httpRsp);
//		if(succ){
//			return onSuccessResponse(rsp);
//		}else{
//			return onErrorResponse(rsp);
//		}
//	}
	
//	private void saveFile(DownloadRequest req, HttpResponse rsp) throws Exception {
//		File saveFile = new File(req.getSaveFilePath());
//		File tmpFile = new File(saveFile.getAbsolutePath() + ".tmp");
//
//		InputStream is = null;
//		FileOutputStream fos = null;
//		try {
//			is = rsp.getEntity().getContent();
//			if (isCancel()) {
//				is.close();
//				is = null;
////				notifyCancel();
//			} else {
//				int length = (int) rsp.getEntity().getContentLength();
//				fos = new FileOutputStream(tmpFile);
//
//				byte[] buff = new byte[1024 * 2];
//				int len = -1;
//				int sum = 0;
//
//				while ((len = is.read(buff)) != -1 && sum <= length
//				        && !isCancel()) {
//					sum += len;
//					fos.write(buff, 0, len);
//					fos.flush();
////					notifyProgress(sum * 100 / length);
//					notifyMessage(TaskConstants.MSG_UPLOAD_PERCENT, sum * 100 / length);
//				}
//
//				is.close();
//				fos.close();
//				is = null;
//				fos = null;
//
//				if (isCancel()) {
////					notifyCancel();
//				} else {
//					tmpFile.renameTo(saveFile);
////					notifySuccess();
//				}
//			}
//		} catch (ClientProtocolException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (Exception e) {
//			e.printStackTrace();
//		} finally {
//			if (is != null) {
//				try {
//					is.close();
//					is = null;
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//				}
//			}
//			if (fos != null) {
//				try {
//					fos.close();
//					fos = null;
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		}
//	}

//	private void saveCache(Request request, byte[] data,
//	        Map<String, String> headers) {
//		if(request == null)return;
//		if (request.shouldCache()) {
//			Cache.Entry entry = parseCacheHeaders(data, headers);
//			if(entry != null){
//				mTaskEngnie.getHttpEngine().getCache().put(request.getCacheKey(), entry);
//			}
//		}
//	}

//	private byte[] readData(HttpResponse httpRsp) throws IOException {
//		InputStream is = httpRsp.getEntity().getContent();
//
//		PoolingByteArrayOutputStream baos = new PoolingByteArrayOutputStream(
//		        mTaskEngnie.getBytePool());
//
//		byte[] buff = new byte[1024];
//		int len = -1;
//		while ((len = is.read(buff)) != -1) {
//			baos.write(buff, 0, len);
//		}
//		baos.flush();
//		byte[] datas = baos.toByteArray();
//		baos.close();
//		return datas;
//	}

//	private HttpPost httpPost(HttpPostRequest req) {
//		HttpPost httpPost = new HttpPost(req.getRequestUrl());
//		addHeader(req, httpPost);
//		httpPost.setEntity(new ByteArrayEntity(req.getPostData()));
//		return httpPost;
//	}
//
//	private HttpGet httpGet(HttpGetRequest req) {
//		HttpGet httpGet = new HttpGet(req.getRequestUrl());
//		addHeader(req, httpGet);
//		return httpGet;
//	}
//
//	private HttpGet httpDownload(DownloadRequest req) {
//		HttpGet httpGet = new HttpGet(req.getRequestUrl());
//		addHeader(req, httpGet);
//		return httpGet;
//	}

//	private void addHeader(HttpRequest req, HttpRequestBase httpMethod) {
//		Map<String, String> headers = req.getHeaders();
//		Cache.Entry entry = null;
//		if ((entry = req.getCacheEntry()) != null) {
//			if (entry.etag != null) {
//				headers.put("If-None-Match", entry.etag);
//			}
//			if (entry.lastModified > 0) {
//				Date refTime = new Date(entry.lastModified);
//				headers.put("If-Modified-Since", DateUtils.formatDate(refTime));
//			}
//		}
//
//		if (headers != null && headers.size() != 0) {
//			Set<String> keySet = headers.keySet();
//			Iterator<String> keys = keySet.iterator();
//			while (keys.hasNext()) {
//				String key = keys.next();
//				String value = headers.get(key);
//				httpMethod.addHeader(key, value);
//			}
//		}
//
//	}

//	private Map<String, String> convertHeaders(Header[] headers) {
//		Map<String, String> result = new TreeMap<String, String>(
//		        String.CASE_INSENSITIVE_ORDER);
//		for (int i = 0; i < headers.length; i++) {
//			result.put(headers[i].getName(), headers[i].getValue());
//		}
//		return result;
//	}
//
//	public static Cache.Entry parseCacheHeaders(byte[] data,
//	        Map<String, String> headers) {
//		long now = System.currentTimeMillis();
//
//		long serverDate = 0;
//		long lastModified = 0;
//		long serverExpires = 0;
//		long softExpire = 0;
//		long finalExpire = 0;
//		long maxAge = 0;
//		long staleWhileRevalidate = 0;
//		boolean hasCacheControl = false;
//		boolean mustRevalidate = false;
//
//		String serverEtag = null;
//		String headerValue;
//
//		headerValue = headers.get("Date");
//		if (headerValue != null) {
//			serverDate = parseDateAsEpoch(headerValue);
//		}
//
//		headerValue = headers.get("Cache-Control");
//		if (headerValue != null) {
//			hasCacheControl = true;
//			String[] tokens = headerValue.split(",");
//			for (int i = 0; i < tokens.length; i++) {
//				String token = tokens[i].trim();
//				if (token.equals("no-cache") || token.equals("no-store")) {
//					return null;
//				} else if (token.startsWith("max-age=")) {
//					try {
//						maxAge = Long.parseLong(token.substring(8));
//					} catch (Exception e) {
//					}
//				} else if (token.startsWith("stale-while-revalidate=")) {
//					try {
//						staleWhileRevalidate = Long.parseLong(token.substring(23));
//					} catch (Exception e) {
//					}
//				} else if (token.equals("must-revalidate")
//				        || token.equals("proxy-revalidate")) {
//					mustRevalidate = true;
//				}
//			}
//		}
//
//		headerValue = headers.get("Expires");
//		if (headerValue != null) {
//			serverExpires = parseDateAsEpoch(headerValue);
//		}
//
//		headerValue = headers.get("Last-Modified");
//		if (headerValue != null) {
//			lastModified = parseDateAsEpoch(headerValue);
//		}
//
//		serverEtag = headers.get("ETag");
//
//		// Cache-Control takes precedence over an Expires header, even if both
//		// exist and Expires
//		// is more restrictive.
//		if (hasCacheControl) {
//			softExpire = now + maxAge * 1000;
//			finalExpire = mustRevalidate ? softExpire : softExpire + staleWhileRevalidate * 1000;
//		} else if (serverDate > 0 && serverExpires >= serverDate) {
//			// Default semantic for Expire header in HTTP specification is
//			// softExpire.
//			softExpire = now + (serverExpires - serverDate);
//			finalExpire = softExpire;
//		}
//
//		Cache.Entry entry = new Cache.Entry();
//		entry.data = data;
//		entry.etag = serverEtag;
//		entry.softTtl = softExpire;
//		entry.ttl = finalExpire;
//		entry.serverDate = serverDate;
//		entry.lastModified = lastModified;
//		entry.responseHeaders = headers;
//
//		return entry;
//	}
//
//	public static long parseDateAsEpoch(String dateStr) {
//		try {
//			// Parse date in RFC1123 format if this header contains one
//			return DateUtils.parseDate(dateStr).getTime();
//		} catch (DateParseException e) {
//			// Date in invalid format, fallback to 0
//			return 0;
//		}
//	}
	
//	private void printRspheader(Header[] headers){
//		if (headers != null && headers.length != 0) {
//			StringBuilder sb = new StringBuilder();
//			for(Header header : headers){
//				sb.append(header.getName());
//				sb.append(":");
//				sb.append(header.getValue());
//				sb.append(" ");
//			}
//			PalLog.d("NetworkTask headers", sb.toString());
//		}
//	}

}
