package com.colin.framework.network;

/**
 * Created by xhm on 16-12-5.
 */

public class HeaderInternal {

    private String name;
    private String value;

    public HeaderInternal(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String getName(){
        return name;
    }

    public String getValue(){
        return value;
    }

    @Override
    public String toString(){
        return name + "=" + value;
    }
}
