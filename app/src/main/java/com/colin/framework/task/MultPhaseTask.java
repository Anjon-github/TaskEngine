package com.colin.framework.task;

public abstract class MultPhaseTask extends Task{

	protected int mPhase;

	protected MultPhaseTask(int taskType, TaskEngine engine) {
		super(taskType, engine);
	}
	
	protected void nextPhase(int phase){
		mPhase = phase;
		reExecuteTask();
	}
	
	/**
	 * 再次执行任务
	 * @time 2014-9-25 下午9:26:57
	 */
	protected void reExecuteTask(){
		mTaskEngnie.reExecuteTask(this);
	}
	
	@Override
   public final void run() {
		try{
			if(!isCancel()){
				onTask();
			}
		}catch (Exception e) {
			e.printStackTrace();
			onException(e);
			removeTask();
		}finally {
			if(isRemoved()){
				setTaskWatcher(null);
				mTaskEngnie = null;
			}
		}
	}
	
}
