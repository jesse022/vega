package com.sanlux.pay.direct.dto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/26
 */
@Data
@ConfigurationProperties(prefix = "direct.pay.business")
public class PayBusinessInfo implements Serializable{

    private static final long serialVersionUID = -657236923153773156L;
    //业务类别,N02030:支付,N02040:集团支付
    private String BUSCOD;
    //业务模式编号
    private String BUSMOD;
    //业务模式名称,可空
    private String MODALS;
}
