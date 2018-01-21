package com.ricex.cartracker.common.viewmodel;

/**
 * Created by Mitchell on 2018-01-21.
 */

public class BulkUploadViewModel<T> {

    private String uuid;

    private T data;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
