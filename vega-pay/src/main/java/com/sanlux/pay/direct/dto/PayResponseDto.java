package com.sanlux.pay.direct.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@XmlRootElement(name = "CMBSDKPGK")
public class PayResponseDto implements Serializable {
    private static final long serialVersionUID = 3236742606971687864L;
    @XmlElement(name = "INFO")
    private PayResponseSystemInfo payResponseSystemInfo;
    //支付输出接口
    @XmlElement(name = "NTQPAYRQZ")
    private PayResponseBody payResponseBody;
    @XmlTransient
    public PayResponseSystemInfo getPayResponseSystemInfo() {
        return payResponseSystemInfo;
    }

    public void setPayResponseSystemInfo(PayResponseSystemInfo payResponseSystemInfo) {
        this.payResponseSystemInfo = payResponseSystemInfo;
    }

    @XmlTransient
    public PayResponseBody getPayResponseBody() {
        return payResponseBody;
    }

    public void setPayResponseBody(PayResponseBody payResponseBody) {
        this.payResponseBody = payResponseBody;
    }
}
