package com.colin.framework.cache;

/**
 * Created by xhm on 2016/12/18.
 */

public class CookieInternal {

    private String name;
    private String value;
    private String comment;
    private String commentUrl;
    private long expiryAt;
    private String domain;
    private String path;
    private int[] ports;
    private int version;
    private boolean isSecure;

    public CookieInternal(String name, String value){
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCommentUrl() {
        return commentUrl;
    }

    public void setCommentUrl(String commentUrl) {
        this.commentUrl = commentUrl;
    }

    public long getExpiryAt() {
        return expiryAt;
    }

    public void setExpiryAt(long expiryAt) {
        this.expiryAt = expiryAt;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int[] getPorts() {
        return ports;
    }

    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public void setSecure(boolean secure) {
        isSecure = secure;
    }


    public String toString(){
        return name + "=" + value + " isExiry " + isExpired();
    }

    public boolean isDeleted(){
        return "deleted".equals(value);
    }

    public boolean isExpired(){
        return System.currentTimeMillis() > expiryAt;
    }

    public String getCookieKey() {
        return (isSecure ? "https" : "http") + "://" + domain + path + "|" + name;
    }

}
