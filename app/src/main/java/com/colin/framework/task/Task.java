package com.colin.framework.task;


import com.colin.framework.utils.PalLog;

/**
 * 任务抽象类
 * @author xionghoumiao
 * @date 2014-8-12上午9:11:39
 */
public abstract class Task implements Runnable, Comparable<Task>{
	
	/**
	 * Priority values. Requests will be processed from higher priorities to
	 * lower priorities, in FIFO order.
	 */
	public enum Priority {
		LOW, NORMAL, HIGH, IMMEDIATE
	}
	
	private static short TASK_ID;
	
	/** 通用成功 */
//	public static final int SUCC_TYPE_COMMON = 0;
	/** 上传文件进度 */
	public static final int MSG_UPLOAD_PERCENT = 1;
	/** 下载文件进度 */
	public static final int MSG_DOWNLOAD_PERCENT = 2;
	
	/** 通用错误 */
	public static final int ERR_TYPE_COMMON = -1;
	/** 连接超时 */
	public static final int ERR_TYPE_TIMEOUT = -2;
	/** 网络中断 */
	public static final int ERR_TYPE_NETWORK = -3;
	/** 服务器异常（非200） */
	public static final int ERR_TYPE_SERVER = -4;
//	/** 数据编码格式 */
//	public static final int ERR_TYPE_DATA_ENCODING = -5;
	/** 业务逻辑异常 */
	public static final int ERR_TYPE_BUSI_LOGIC = -6;
	
	
	
	private int mTaskId;
	
	private int mTaskType;//任务类型
	protected int mSubType;//子类型
	
	private boolean mCancel;
	private boolean mRemoved;
	
	private TaskWatcher mWatcher;
	protected TaskEngine mTaskEngnie;
	
	private Priority mPri = Priority.NORMAL;
	
	protected Task(int taskType, TaskEngine engine){
		mTaskId = getNextTaskId();
		mTaskType = taskType;
		mTaskEngnie = engine;
	}
	
	public void setPriority(Priority pri){
		mPri = pri;
	}
	
	public void setTaskWatcher(TaskWatcher watcher){
		mWatcher = watcher;
	}

	public void removeAllMessage(){
		if(mWatcher != null){
			mWatcher.removeAllMessage();
		}
	}
	
	protected final void notifyMessage(int what){
		notifyMessage(what, null);
	}
	
	protected final void notifyMessage(int what, Object obj){
		notifyMessage(what, getTaskId(), getTaskType(), obj);
	}
	
	protected final void notifyMessage(int what, int arg1, int arg2, Object obj){
		if(mWatcher!= null && !isCancel()){
			mWatcher.onTaskMessage(what, arg1, arg2, mSubType, obj);
		}
		PalLog.d(getClass().getSimpleName(), "notifyMessage " + what);
	}
	
	protected final void notifyError(String errMsg){
		notifyError(ERR_TYPE_COMMON, errMsg);
	}
	
	protected final void notifyError(int what, Object obj){
		notifyError(what, getTaskId(), getTaskType(), obj);
	}
	
	protected final void notifyError(int what, int arg1, int arg2, Object obj){
		if(mWatcher!= null && !isCancel()){
			mWatcher.onTaskError(what, arg1, arg2, mSubType, obj);
		}
		PalLog.d(getClass().getSimpleName(), "notifyError " + what);
	}
	
	protected final int getTaskType(){
		return mTaskType;
	}
	
	public final int getTaskId(){
		return mTaskId;
	}
	
	public synchronized void cancel(){
		mCancel = true;
	}
	
	protected synchronized boolean isCancel(){
		return mCancel;
	}
	
	@Override
	public int compareTo(Task other) {
		Priority left = mPri;
		Priority right = other.mPri;
		
		// High-priority requests are "lesser" so they are sorted to the front.
		// Equal priorities are sorted by sequence number to provide FIFO
		// ordering.
		return left == right ? this.mTaskId - other.mTaskId : right
		        .ordinal() - left.ordinal();
	}

	/**
	 * 移除任务
	 */
	protected void removeTask(){
		mRemoved = true;
		mTaskEngnie.removeTask(this);
	}

	public boolean isRemoved(){
		return mRemoved;
	}

	@Override
	public void run() {
		try{
			if(!isCancel()){
				onTask();
			}
		}catch (Exception e) {
			e.printStackTrace();
			onException(e);
		}finally{
			removeTask();
			setTaskWatcher(null);
			mTaskEngnie = null;
		}
	}
	
	protected abstract void onTask() throws Exception;
	
	/**
	 *  执行中出错
	 * @param errorType
	 */
	protected boolean onExecuteError(int errorType, String errorMsg){
		notifyError(errorType, errorMsg);
		return true;
	}
	
	/**
	 *  执行中抛出Exception
	 * @param e
	 */
	protected final boolean onException(Exception e){
		notifyError(e.getMessage());
		return true;
	};
	
	
	private synchronized short getNextTaskId(){
		if(TASK_ID >= Short.MAX_VALUE){
			TASK_ID = 0;
		}
		return ++TASK_ID;
	}

}
