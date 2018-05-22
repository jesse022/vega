package com.sanlux.item.enums;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
public enum VegaTaskJobStatus {
    WAIT_HANDLE(0,"未处理"),
    HANDLEING(1,"处理中"),
    HANDLED(2,"处理完成"),
    FAIL(-1,"处理失败"),
    COLSE(-2,"已关闭");

    private final int value;

    private final String description;

    private VegaTaskJobStatus(int value, String description) {
        this.value = value;
        this.description = description;
    }

    public int value() {
        return this.value;
    }


    @Override
    public String toString() {
        return description;
    }
}
