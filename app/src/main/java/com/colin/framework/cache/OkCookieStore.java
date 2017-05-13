package com.colin.framework.cache;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * Created by xhm on 2016/12/18.
 */
public class OkCookieStore implements CookieJar{

    private static final String TAG = OkCookieStore.class.getSimpleName();

    private CookieDBManager dbMgr;
    private boolean openLog;
    Set<Cookie> cacheCookies;


    private static OkCookieStore instance;

    public static OkCookieStore getInstance(Context con) {
        if (instance == null) {
            synchronized (OkCookieStore.class) {
                if (instance == null) {
                    instance = new OkCookieStore(con.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private OkCookieStore(Context con){
        dbMgr = CookieDBManager.getInstance(con);
    }

    private Set<Cookie> getCachedCookies() {
        Set<Cookie> result;
        if (cacheCookies != null && cacheCookies.size() != 0) {
            result = cacheCookies;
        } else {
            result = new HashSet<Cookie>();
            List<CookieInternal> cached = dbMgr.getAllCookies();
            if (cached != null && !cached.isEmpty()) {
                for(CookieInternal cookieInternal : cached){
                    result.add(transCookie(cookieInternal));
                }
            }
        }
        return result;
    }

    public synchronized void clear(){
        dbMgr.clear();
        if (cacheCookies != null) {
            cacheCookies.clear();
        }
    }

    @Override
    public synchronized void saveFromResponse(HttpUrl httpUrl, List<Cookie> list) {
        if(list != null && !list.isEmpty()){
            Set<CookieInternal> cookieInternals = new HashSet<>();
            for (Cookie cookie : list) {
                if (!isCookieExpired(cookie)) {
                    boolean isNew = getCachedCookies().add(cookie);

                    if(isNew){
                        CookieInternal cookieInternal = transCookie(cookie);
                        cookieInternals.add(cookieInternal);
                    }
                }
            }
            if (!cookieInternals.isEmpty()) {
                dbMgr.saveCookies(cookieInternals.toArray(new CookieInternal[cookieInternals.size()]));
            }
        }
    }

    @Override
    public synchronized List<Cookie> loadForRequest(HttpUrl httpUrl) {
        Set<Cookie> cookieInternalList = getCachedCookies();
        List<CookieInternal> toRemoveCookies = new ArrayList<>();
        Set<Cookie> validCookiesSet = new HashSet<>();

        Iterator<Cookie> iterator = cookieInternalList.iterator();
        while(iterator.hasNext()) {
            Cookie cookie = iterator.next();
            if(isCookieExpired(cookie)){
                toRemoveCookies.add(transCookie(cookie));
                iterator.remove();
            }else if(cookie.matches(httpUrl)){
                validCookiesSet.add(cookie);
            }
        }

        if (!toRemoveCookies.isEmpty()) {
            dbMgr.removeCookies(toRemoveCookies.toArray(new CookieInternal[toRemoveCookies.size()]));
        }

        List<Cookie> validCookieList = new ArrayList<>();
        Iterator<Cookie> validIterator = validCookiesSet.iterator();
        while (validIterator.hasNext()){
            validCookieList.add(validIterator.next());
        }
        return validCookieList;
    }

    private boolean isCookieExpired(Cookie cookie) {
        return cookie == null || cookie.expiresAt() < System.currentTimeMillis() || "deleted".equals(cookie.value());
    }

    private CookieInternal transCookie(Cookie cookie){
        CookieInternal cookieIna = new CookieInternal(cookie.name(), cookie.value());
        cookieIna.setSecure(cookie.secure());
        cookieIna.setPath(cookie.path());
        cookieIna.setExpiryAt(cookie.expiresAt());
        cookieIna.setDomain(cookie.domain());
        return cookieIna;
    }

    private Cookie transCookie(CookieInternal cookieInternal){
        Cookie.Builder builder = new Cookie.Builder();
        builder.name(cookieInternal.getName());
        builder.value(cookieInternal.getValue());
        builder.domain(cookieInternal.getDomain());
        builder.path(cookieInternal.getPath());
        builder.expiresAt(cookieInternal.getExpiryAt());
        if(cookieInternal.isSecure()){
            builder.secure();
        }

        return builder.build();
    }

}
