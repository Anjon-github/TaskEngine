//package com.hithink.framework.task;
//
//import android.content.Context;
//import android.widget.Toast;
//
//public class WeakToastErrorHandler extends WeakTaskHandler {
//
//	private Context mCon;
//
//	public WeakToastErrorHandler(Context con) {
//		mCon = con;
//	}
//
//	@Override
//	public void handleMessage(int what, int arg1, Object obj) {
//
//	}
//
//	@Override
//	public void handleError(int what, int arg1, Object obj) {
//		if(mCon != null && obj != null && obj instanceof String){
//			Toast.makeText(mCon, (String)obj, Toast.LENGTH_SHORT).show();
//		}
//	}
//
//	@Override
//	public void removeAllMessage() {
//		removeCallbacksAndMessages(null);
//	}
//}
