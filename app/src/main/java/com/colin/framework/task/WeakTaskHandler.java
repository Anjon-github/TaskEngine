//package com.hithink.framework.task;
//
//
//public abstract class WeakTaskHandler extends WeakHandler implements
//        TaskWatcher {
//
//	public abstract void handleMessage(int what, int arg1, Object obj);
//
//	public abstract void handleError(int what, int arg1, Object obj);
//
//	@Override
//	public void onTaskMessage(int what, int arg1, int arg2, Object obj) {
//		post(new MsgRunnable(TYPE_MSG, what, arg1, arg2, obj));
//	}
//
//	@Override
//	public void onTaskError(int what, int arg1, int arg2, Object obj) {
//		post(new MsgRunnable(TYPE_ERROR, what, arg1, arg2, obj));
//	}
//
//	@Override
//	public void removeAllMessage() {
//		removeCallbacksAndMessages(null);
//	}
//
//	private static final int TYPE_MSG = 0;
//	private static final int TYPE_ERROR = 1;
//
//	class MsgRunnable implements Runnable {
//
//
//		private int type;
//		private int what;
//		private int arg1;
//		private int arg2;
//		private Object obj;
//
//		MsgRunnable(int type, int what, int arg1, int arg2, Object obj) {
//			this.type = type;
//			this.what = what;
//			this.arg1 = arg1;
//			this.arg2 = arg2;
//			this.obj = obj;
//		}
//
//		@Override
//		public void run() {
//			if (type == TYPE_MSG) {
//				handleMessage(what, arg1, obj);
//			} else if(type == TYPE_ERROR){
//				handleError(what, arg1, obj);
//			}
//		}
//
//	}
//
//}
