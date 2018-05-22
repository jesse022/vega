package com.sanlux.pay.direct.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@XmlRootElement(name = "CMBSDKPGK")
public class QueryRequestDto implements Serializable{
    private static final long serialVersionUID = -3569455640196714508L;

    @XmlElement(name = "INFO")
    private QueryRequestSystemInfo queryRequestSystemInfo;

    @XmlElement(name = "SDKPAYQYX")
    private QueryRequestBody queryRequestBody;
    @XmlTransient
    public QueryRequestSystemInfo getQueryRequestSystemInfo() {
        return queryRequestSystemInfo;
    }

    public void setQueryRequestSystemInfo(QueryRequestSystemInfo queryRequestSystemInfo) {
        this.queryRequestSystemInfo = queryRequestSystemInfo;
    }
    @XmlTransient
    public QueryRequestBody getQueryRequestBody() {
        return queryRequestBody;
    }

    public void setQueryRequestBody(QueryRequestBody queryRequestBody) {
        this.queryRequestBody = queryRequestBody;
    }
}
