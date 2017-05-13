package com.colin.framework.task;

import android.content.Context;

import com.colin.framework.network.NetworkInterface;
import com.colin.framework.network.OkHttpEngine;
import com.colin.framework.utils.PalLog;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 任务引擎
 *    负责添加任务，取消任务
 * @author xionghoumiao
 * @date 2014-8-12上午9:12:08
 */
public class TaskEngine {

	private static final boolean ENABLE_LOG = false;
	
	private static final String TAG = TaskEngine.class.getSimpleName();
	
	private static final int CORE_POOL_SIZE = 3;
	private static final int MAX_POOL_SIZE = 5;
	
	private static final int KEEP_ALIVE_TIME = 3;
	
	private static final TimeUnit  TIME_UNIT = TimeUnit.SECONDS;
	
	private PriorityBlockingQueue<Runnable> mBlockingQueue;
	private Map<Integer, Task> mAllTaskMap;
	
	private final Object mLock = new Object();

	private ExecutorService mService;

	private NetworkInterface mHttpEngine;

	public TaskEngine(Context con, boolean needCookieStore) {
//		mHttpEngine = new HttpClientEngine(con);
		mHttpEngine = new OkHttpEngine(con, needCookieStore);
		mBlockingQueue = new PriorityBlockingQueue<>();
		mAllTaskMap = new Hashtable<>();
	}
	
	public void excuteTask(Task task){
		synchronized (mLock) {
			int taskId = task.getTaskId();
			if(!mAllTaskMap.containsKey(taskId)){
				if(ENABLE_LOG){
					PalLog.d(TAG, "addAlltask " + taskId);
				}
				mAllTaskMap.put(taskId, task);
				getExcutor().execute(task);
			}
		}
	}
	
	public void cancelTask(int taskId){
	    cancelTask(taskId, null);
	}
	
	private void cancelTask(int taskId, Iterator<Integer> itera){
		synchronized (mLock) {
			if(mAllTaskMap != null && mAllTaskMap.containsKey(taskId)){
				Task task = mAllTaskMap.get(taskId);
				task.removeAllMessage();
				task.setTaskWatcher(null);
				task.cancel();
				mBlockingQueue.remove(task);
				if(itera == null){
				    mAllTaskMap.remove(taskId);
				}else{
				    itera.remove();
				}
				if(ENABLE_LOG){
					PalLog.d(TAG, "cancelTask " + taskId);
				}
			}
		}
	}
	
	public void cancelTask(Task task){
		cancelTask(task.getTaskId());
	}
	
	private void canelAll(){
		synchronized (mLock) {
			if(mAllTaskMap != null && mAllTaskMap.size() != 0){
				Iterator<Integer> itera = mAllTaskMap.keySet().iterator();
				while(itera.hasNext()){
					int taskId = itera.next();
					cancelTask(taskId, itera);
				}
			}
        }
	}
	
	void reExecuteTask(Task task){
		if(task == null || mAllTaskMap == null)return;
		
		int taskId = task.getTaskId();
		synchronized (mLock) {
			if(mAllTaskMap.containsKey(taskId)){
				if(ENABLE_LOG){
					PalLog.e(TAG, "reExecuteTask containskey " + taskId);
				}
			}else{
				mAllTaskMap.put(taskId, task);
				if(ENABLE_LOG){
					PalLog.e(TAG, "reExecuteTask not containskey " + taskId);
				}
			}
			getExcutor().execute(task);
		}
	}
	
	void removeTask(Task task){
		synchronized (mLock) {
			int taskId = task.getTaskId();
			if(mAllTaskMap != null && mAllTaskMap.containsKey(taskId)){
				mAllTaskMap.remove(taskId);
				if(ENABLE_LOG){
					PalLog.d(TAG, "removeAllTask " + taskId);
				}
			}
		}
	}

	public NetworkInterface getHttpEngine(){
		return mHttpEngine;
	}
	
	private ExecutorService getExcutor(){
		if(mService == null){
			synchronized (mLock) {
				mService = new ThreadPoolExecutor(CORE_POOL_SIZE, MAX_POOL_SIZE, 
						KEEP_ALIVE_TIME, TIME_UNIT, mBlockingQueue);
			}
		}
		return mService;
	}
	
	public void shutDown(){
		canelAll();
		if(mAllTaskMap != null){
			mAllTaskMap.clear();
			mAllTaskMap = null;
		}
		if(mBlockingQueue != null){
			mBlockingQueue.clear();
			mBlockingQueue = null;
		}
		if(mHttpEngine != null){
			mHttpEngine.shutDown();
			mHttpEngine = null;
		}
		if(mService != null){
			mService.shutdownNow();
			mService = null;
		}
	}

}
