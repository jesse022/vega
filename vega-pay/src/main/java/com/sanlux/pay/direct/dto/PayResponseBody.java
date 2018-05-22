package com.sanlux.pay.direct.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class PayResponseBody implements Serializable {
    private static final long serialVersionUID = -4899468068851081243L;

    //流水号
    private String SQRNBR;
    //业务参考号
    private String YURREF;
    //流程实例号
    private String REQNBR;
    //业务请求状态
    private String REQSTS;
    //业务处理结果
    private String RTNFLG;
    //待处理操作序列
    private String OPRSQN;
    //操作别名
    private String OPRALS;
    //错误代码
    private String ERRCOD;
    //错误文本
    private String ERRTXT;


}
