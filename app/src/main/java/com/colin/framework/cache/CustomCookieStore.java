package com.colin.framework.cache;

import android.content.Context;

import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.Date;
import java.util.List;

public class CustomCookieStore extends BasicCookieStore {
	
	private static final String TAG = "CustomCookieStore";

	CookieDBManager mDbMgr;
	private Context mCon;
	
	boolean isFromThis;

	private static CustomCookieStore instance;

	public static CustomCookieStore getInstance(Context con) {
		if (instance == null) {
			synchronized (CustomCookieStore.class) {
				if (instance == null) {
					instance = new CustomCookieStore(con.getApplicationContext());
				}
			}
		}
		return instance;
	}

	private CustomCookieStore(Context con) {
		super();
		try {
			mCon = con;
			mDbMgr = CookieDBManager.getInstance(con);
			List<CookieInternal> cookieInterList = mDbMgr.getAllCookies();
			CookieInternal[] cookiesArr = new CookieInternal[cookieInterList.size()];
			for (int i = cookieInterList.size() - 1; i >= 0; i--) {
				cookiesArr[i] = cookieInterList.get(i);
			}
			isFromThis = true;

			Cookie[] cookies = new Cookie[cookiesArr.length];
			for(int i = 0; i < cookiesArr.length; i++){
				cookies[i] = transCookie(cookiesArr[i]);
			}
			addCookies(cookies);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	Cookie transCookie(CookieInternal cookieIn){
		BasicClientCookie cookie = new BasicClientCookie(cookieIn.getName(), cookieIn.getValue());
		cookie.setComment(cookieIn.getComment());
		cookie.setDomain(cookieIn.getDomain());
		cookie.setExpiryDate(new Date(cookieIn.getExpiryAt()));
		cookie.setPath(cookieIn.getPath());
		cookie.setSecure(cookieIn.isSecure());
		cookie.setVersion(cookieIn.getVersion());
		return cookie;
	}

	CookieInternal transCookie(Cookie cookie){
		CookieInternal cookieInternal = new CookieInternal(cookie.getName(), cookie.getValue());
		cookieInternal.setComment(cookie.getComment());
		cookieInternal.setCommentUrl(cookie.getCommentURL());
		cookieInternal.setDomain(cookie.getDomain());
		cookieInternal.setExpiryAt(cookie.getExpiryDate().getTime());
		cookieInternal.setPath(cookie.getPath());
		cookieInternal.setPorts(cookie.getPorts());
		cookieInternal.setSecure(cookie.isSecure());
		cookieInternal.setVersion(cookie.getVersion());
		return cookieInternal;
	}

	@Override
	public synchronized void addCookie(Cookie cookie) {
		super.addCookie(cookie);
		try {
			mDbMgr.saveCookie(transCookie(cookie));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	@Override
	public synchronized void addCookies(Cookie[] cookies) {
		super.addCookies(cookies);
		if(isFromThis){
			isFromThis = false;
			return;
		}
		CookieInternal[] cookieInternals = new CookieInternal[cookies.length];
		for(int i = 0; i < cookies.length; i++){
			cookieInternals[i] = transCookie(cookies[i]);
		}
		try {
			mDbMgr.saveCookies(cookieInternals);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void clear() {
		super.clear();
		try {
			mDbMgr.clear();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized boolean clearExpired(Date date) {
		try {
			mDbMgr.clearExpired();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return super.clearExpired(date);
	}

}
