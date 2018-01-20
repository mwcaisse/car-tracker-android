package com.ricex.cartracker.androidrequester.request.response;

import org.springframework.http.HttpStatus;

/**
 * Created by Mitchell on 2018-01-20.
 */

public class SuccessResponse<T>  extends RequestResponse<T> {

    private final T result;

    public SuccessResponse(T result, HttpStatus statusCode) {
        super(statusCode);
        this.result = result;
    }

    public T getData() {
        return result;
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public boolean isValid() {
        return result != null;
    }

}
