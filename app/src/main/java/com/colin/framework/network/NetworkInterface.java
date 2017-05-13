package com.colin.framework.network;

import com.colin.framework.Response.Response;
import com.colin.framework.request.Request;

/**
 * Created by xhm on 16-12-5.
 */

public interface NetworkInterface {

    Response execute(Request uriReq);

    void shutDown();

    void clearCookieStore();

}
