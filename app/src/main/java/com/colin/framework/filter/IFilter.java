package com.colin.framework.filter;

/**
 * Created by xhm on 16-8-9.
 */
public interface IFilter {

//    public void addFiter(int what);

//    public void addFilterMessage(int what);
//
//    public void removeFilterMessage(int what);

    void doFilter(int what, int arg1, int arg2, Object obj);

//    public void handleFilter(int what, int arg1, int arg2, Object obj);

}
