package com.colin.framework.task;

public interface TaskWatcher {

	void onTaskMessage(int what, int taskId, int taskType, int subType, Object obj);

	void onTaskError(int what, int taskId, int taskType, int subType, Object obj);

//	void onTaskMessage(int what, int arg1, int arg2, Object obj);
//
//	void onTaskError(int what, int arg1, int arg2, Object obj);

	void removeAllMessage();
	
}
