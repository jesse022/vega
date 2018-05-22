package com.sanlux.pay.direct.dto;


import javax.xml.bind.annotation.*;
import java.io.Serializable;


/**
 * Created by liangfujie on 16/10/26
 */
@XmlRootElement(name = "CMBSDKPGK")
public class PayRequestDto implements Serializable {
    private static final long serialVersionUID = 2705901852157592610L;


    //INFO信息
    @XmlElement(name = "INFO")
    private PayRequestSystemInfo systemInfo;


    //SDKPAYRQX信息,支付输入概要接口
    @XmlElement(name = "SDKPAYRQX")
    private PayBusinessInfo businessInfo;

    //DCPAYREQX信息,支付输入明细接口
    @XmlElement(name = "DCPAYREQX")
    private PayFunctionInfo functionInfo;

    @XmlTransient
    public PayRequestSystemInfo getSystemInfo() {
        return systemInfo;
    }

    public void setSystemInfo(PayRequestSystemInfo systemInfo) {
        this.systemInfo = systemInfo;
    }
    @XmlTransient
    public PayBusinessInfo getBusinessInfo() {
        return businessInfo;
    }

    public void setBusinessInfo(PayBusinessInfo businessInfo) {
        this.businessInfo = businessInfo;
    }
    @XmlTransient
    public PayFunctionInfo getFunctionInfo() {
        return functionInfo;
    }

    public void setFunctionInfo(PayFunctionInfo functionInfo) {
        this.functionInfo = functionInfo;
    }
}
