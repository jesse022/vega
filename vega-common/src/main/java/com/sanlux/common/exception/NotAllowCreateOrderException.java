package com.sanlux.common.exception;

/**
 * 不允许下单异常
 */
public class NotAllowCreateOrderException extends RuntimeException {

    private static final long serialVersionUID = -4996243913748994895L;

    public NotAllowCreateOrderException( String message) {
        super(message);
    }
}
