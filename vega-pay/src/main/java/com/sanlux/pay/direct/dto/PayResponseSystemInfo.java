package com.sanlux.pay.direct.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class PayResponseSystemInfo implements Serializable{
    private static final long serialVersionUID = -5125902243348999982L;
    //函数名称
    private String FUNNAM;
    //数据格式
    private String DATTYP;
    //返回代码
    private String RETCOD;
    //错误信息
    private String ERRMSG="";

}
