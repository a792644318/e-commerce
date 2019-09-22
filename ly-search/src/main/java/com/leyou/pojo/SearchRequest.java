package com.leyou.pojo;

import org.elasticsearch.common.recycler.Recycler;

import java.util.Map;

public class SearchRequest{
    private String key;
    private Integer page;
    private Map<String,String> filter;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getPage() {
        //如果page为空，则返回默认的page值
        if(page == null){
            return DEFAULT_PAGE;
        }
        //如果page不为空，则返回最大的
        return Math.max(DEFAULT_PAGE,page);
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Map<String, String> getFilter() {
        return filter;
    }

    public void setFilter(Map<String, String> filter) {
        this.filter = filter;
    }

    public Integer getSize() {
        return DEFAULT_SIZE;
    }
}