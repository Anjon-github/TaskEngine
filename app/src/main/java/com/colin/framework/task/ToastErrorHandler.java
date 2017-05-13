package com.colin.framework.task;

import android.content.Context;
import android.os.Looper;

import com.colin.framework.utils.ToastUtil;

public class ToastErrorHandler extends TaskHandler {

	private Context mCon;

    public ToastErrorHandler(Context con, Looper looper){
        super(looper);
        mCon = con;
    }

	public ToastErrorHandler(Context con) {
		mCon = con;
	}

	@Override
	public void handleMessage(int what, int arg1, Object obj) {

	}

	@Override
	public void handleError(int what, int arg1, Object obj) {
		if(mCon != null && obj != null && obj instanceof String){
			ToastUtil.toast(mCon, (String)obj);
		}
	}

}
