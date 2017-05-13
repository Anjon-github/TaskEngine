package com.colin.framework.task;

import java.util.ArrayList;
import java.util.List;

public class Dispather extends TaskHandler {

	List<TaskHandler> list = null;

	public void addWatcher(TaskHandler tw) {
		if(tw == null)return;
		if (list == null) {
			list = new ArrayList<TaskHandler>();
			list.add(tw);
		} else if (!list.contains(tw)) {
			list.add(tw);
		}
	}

	public void removeWatcher(TaskHandler tw) {
		if (isNotEmpty() && tw != null && list.contains(tw)) {
			list.remove(tw);
		}
	}

	public void clearAll() {
		if (list != null) {
			list.clear();
			list = null;
		}
	}

	private boolean isNotEmpty() {
		return list != null && !list.isEmpty();
	}
	
	public void dispathMessage(int what, int arg1, Object obj){
		if(isNotEmpty()){
			for(TaskHandler th : list){
				th.handleMessage(what, arg1, obj);
			}
		}
	}
	
	public void dispathError(int what, int arg1, Object obj){
		if(isNotEmpty()){
			for(TaskHandler th : list){
				th.handleError(what, arg1, obj);
			}
		}
	}

	public int getSize(){
		return list != null ? list.size() : 0;
	}
	
	@Override
	public void handleMessage(int what, int arg1, Object obj) {
	}

	@Override
	public void handleError(int what, int arg1, Object obj) {

	}

}
