package com.sanlux.pay.direct.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class QueryRequestSystemInfo implements Serializable {
    private static final long serialVersionUID = -6618132555899046791L;

    //函数名
    private String FUNNAM = "GetPaymentInfo";
    //数据格式
    private Integer DATTYP=2;
    //登录用户名
    private String LGNNAM;

}
