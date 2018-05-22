package com.sanlux.web.front.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/11/2
 */
@Slf4j
@ConfigurationProperties(prefix = "direct.pay.params")
@Component
public class DircetPayParams implements Serializable {
    private static final long serialVersionUID = -6743603263310181351L;

    //业务类别,N02030:支付,N02040:集团支付
    private String BUSCOD;
    //业务模式编号
    private String BUSMOD;
    //交易币代码,目前只支持10-人民币
    private String CCYNBR;
    //结算代码,N:普通,F:快速
    private String STLCHN;
    //用途,对应对账单中的摘要
    private String NUSAGE;
    //业务摘要,用于企业付款时填写说明或者备注
    private String BUSNAR;
    //函数名称
    private String FUNNAM;
    //数据类型默认为2,mxl格式3
    private Integer DATTYP;
    //登录用户名,前置机形式必填
    private String LGNNAM;

    //付款账号
    private String DBTACC;
    //付方开户地区代码
    private String DBTBBK;

    public String getDBTACC() {
        return DBTACC;
    }

    public void setDBTACC(String DBTACC) {
        this.DBTACC = DBTACC;
    }

    public String getDBTBBK() {
        return DBTBBK;
    }

    public void setDBTBBK(String DBTBBK) {
        this.DBTBBK = DBTBBK;
    }

    public String getBUSCOD() {
        return BUSCOD;
    }

    public void setBUSCOD(String BUSCOD) {
        this.BUSCOD = BUSCOD;
    }

    public String getBUSMOD() {
        return BUSMOD;
    }

    public void setBUSMOD(String BUSMOD) {
        this.BUSMOD = BUSMOD;
    }

    public String getCCYNBR() {
        return CCYNBR;
    }

    public void setCCYNBR(String CCYNBR) {
        this.CCYNBR = CCYNBR;
    }

    public String getSTLCHN() {
        return STLCHN;
    }

    public void setSTLCHN(String STLCHN) {
        this.STLCHN = STLCHN;
    }

    public String getNUSAGE() {
        return NUSAGE;
    }

    public void setNUSAGE(String NUSAGE) {
        this.NUSAGE = NUSAGE;
    }

    public String getBUSNAR() {
        return BUSNAR;
    }

    public void setBUSNAR(String BUSNAR) {
        this.BUSNAR = BUSNAR;
    }

    public String getFUNNAM() {
        return FUNNAM;
    }

    public void setFUNNAM(String FUNNAM) {
        this.FUNNAM = FUNNAM;
    }

    public Integer getDATTYP() {
        return DATTYP;
    }

    public void setDATTYP(Integer DATTYP) {
        this.DATTYP = DATTYP;
    }

    public String getLGNNAM() {
        return LGNNAM;
    }

    public void setLGNNAM(String LGNNAM) {
        this.LGNNAM = LGNNAM;
    }
}
