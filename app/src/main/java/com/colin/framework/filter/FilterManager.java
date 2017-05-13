package com.colin.framework.filter;

/**
 * Created by xhm on 16-8-9.
 */
public class FilterManager {

    private static final int MAX_SIZE = 10;

    private IFilter[] filterList = new IFilter[MAX_SIZE];

    private static FilterManager instance;

    private FilterManager(){
        for(int index = 0; index < MAX_SIZE; index++){
            filterList[index] = null;
        }
    }

    public static FilterManager getInstance(){
        if(instance == null){
            synchronized (FilterManager.class){
                if(instance == null){
                    instance = new FilterManager();
                }
            }
        }
        return instance;
    }

    public void addFilter(IFilter filter){
        for(int index = 0; index < MAX_SIZE; index++){
            if(filterList[index] == null){
                filterList[index] = filter;
                break;
            }
        }
    }

    public void removeFilter(IFilter filter){
        for(int index = 0; index < MAX_SIZE; index++){
            if(filterList[index] == filter){
                filterList[index] = null;
                break;
            }
        }
    }


    public void doFilter(int what, int arg1, int arg2, Object obj) {
        if(filterList != null){
            for(IFilter filter : filterList){
                if(filter != null){
                    filter.doFilter(what, arg1, arg2, obj);
                }
            }
        }
    }

    public static void destory(){
        if(instance != null){
            for(int index = 0; index < MAX_SIZE; index++){
                instance.filterList[index] = null;
            }
            instance = null;
        }
    }

}
