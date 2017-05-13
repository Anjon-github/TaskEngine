package com.colin.framework.request;

import java.util.Hashtable;
import java.util.Map;

public abstract class HttpRequest extends Request{

    private String action;

	private Map<String, String> mHeaders;

	private Host host;

	//是否支持重定向
	private boolean supportRedirect = true;
	
	public HttpRequest(String url) {
		super(url);
	}

	public void addHeader(String key, String value) {
		if (mHeaders == null) {
			mHeaders = new Hashtable<>();
		}
		mHeaders.put(key, value);
	}

	public boolean isSupportRedirect() {
		return supportRedirect;
	}

	public void setSupportRedirect(boolean supportRedirect) {
		this.supportRedirect = supportRedirect;
	}

	public Map<String, String> getHeaders() {
		return mHeaders;
	}

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

	public void setHost(Host host){
		this.host = host;
	}

	public Host getHost(){
		return host;
	}

	public static class Host{

		private String ip;
		private int port = -1;

		public Host(String ip){
			this.ip = ip;
		}

		public Host(String ip, int port){
			this.ip = ip;
			this.port = port;
		}

		public String getIp(){
			return ip;
		}

		public int getPort(){
			return port;
		}

	}
}
