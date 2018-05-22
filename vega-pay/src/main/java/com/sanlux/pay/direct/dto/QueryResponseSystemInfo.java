package com.sanlux.pay.direct.dto;

import com.sun.org.apache.xpath.internal.operations.String;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class QueryResponseSystemInfo implements Serializable{
    private static final long serialVersionUID = 7560725338779659193L;

    //函数名
    private String FUNNAM;
    //登录格式
    private String DATTYP;
    //返回代码
    private String RETCOD;
    //错误信息
    private String ERRMSG;

}
