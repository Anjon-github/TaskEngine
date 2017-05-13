package com.colin.framework.filter;

/**
 * Created by xhm on 16-10-12.
 */
public abstract class BaseFilter implements IFilter{

    int[] msgList = new int[5];
    int len = msgList.length;

    public BaseFilter(){
        for(int index = 0; index < len; index++){
            msgList[index] = -1;
        }
    }

    public void addFilterMessage(int message){
        for(int index = 0; index < len; index++){
            if(msgList[index] == -1){
                msgList[index] = message;
                break;
            }
        }

    }

    public void removeFilterMessage(int message){
        for(int index = 0; index < len; index++){
            if(msgList[index] == message){
                msgList[index] = -1;
                break;
            }
        }
    }


    @Override
    public void doFilter(int what, int arg1, int arg2, Object obj) {
        for(int index = 0; index < len; index++){
            if(msgList[index] == what){
                handleMessage(what, arg1, arg2, obj);
                break;
            }
        }
    }

    public abstract void handleMessage(int what, int arg1, int arg2, Object obj);

}
