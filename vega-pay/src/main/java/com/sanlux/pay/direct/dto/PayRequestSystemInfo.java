package com.sanlux.pay.direct.dto;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.soap.Name;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/26
 */
@Data
public class PayRequestSystemInfo implements Serializable{

    private static final long serialVersionUID = 730185369559750962L;

    //函数名称
    private String FUNNAM;
    //数据类型默认为2,mxl格式3
    private Integer DATTYP;
    //登录用户名,前置机形式必填
    private String LGNNAM;


}
