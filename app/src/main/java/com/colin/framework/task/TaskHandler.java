package com.colin.framework.task;

import android.os.Handler;
import android.os.Looper;

import com.colin.framework.filter.FilterManager;

public abstract class TaskHandler extends Handler implements TaskWatcher {

    public TaskHandler(){}

    public TaskHandler(Looper looper){
        super(looper);
    }

	public static final class DefaultHandler extends TaskHandler{

		@Override
		public void handleMessage(int what, int arg1, Object obj) {

		}

		@Override
		public void handleError(int what, int arg1, Object obj) {

		}
	}

	public abstract void handleMessage(int what, int arg1, Object obj);

	public abstract void handleError(int what, int arg1, Object obj);

	public boolean handleMessage(int what, int arg1, int arg2, int arg3, Object obj){
		return false;
	}

	public boolean handleError(int what, int arg1, int arg2, int arg3, Object obj){
		return false;
	}

	@Override
	public void onTaskMessage(int what, int taskId, int taskType, int subType, Object obj) {
		post(new MsgRunnable(TYPE_MSG, what, taskId, taskType, subType, obj));
	}

	@Override
	public void onTaskError(int what, int taskId, int taskType, int subType, Object obj) {
		post(new MsgRunnable(TYPE_ERROR, what, taskId, taskType, subType, obj));
	}

	@Override
	public void removeAllMessage() {
		removeCallbacksAndMessages(null);
	}

	private static final int TYPE_MSG = 0;
	private static final int TYPE_ERROR = 1;
	
	class MsgRunnable implements Runnable {

		private int type;
		private int what;
		private int arg1;
		private int arg2;
		private int arg3;
		private Object obj;

		MsgRunnable(int type, int what, int arg1, int arg2, int arg3, Object obj) {
			this.type = type;
			this.what = what;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.arg3 = arg3;
			this.obj = obj;
		}

		@Override
		public void run() {
			if (type == TYPE_MSG) {
				if(!handleMessage(what, arg1, arg2, arg3, obj)){
					//未处理完继续处理
					handleMessage(what, arg1, obj);
				}
			} else if(type == TYPE_ERROR){
				if(!handleError(what, arg1, arg2, arg3, obj)){
					//未处理完继续处理
					handleError(what, arg1, obj);
				}
			}
			FilterManager.getInstance().doFilter(what, arg1, arg2, obj);
		}

	}

}
