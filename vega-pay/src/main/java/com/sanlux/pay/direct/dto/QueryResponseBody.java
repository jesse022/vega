package com.sanlux.pay.direct.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class QueryResponseBody implements Serializable {
    private static final long serialVersionUID = -8039709313874344557L;
    //业务类型
    private String C_BUSCOD;
    //业务代码
    private String BUSCOD;
    //业务模式
    private String BUSMOD;
    //付方开户地区
    private String C_DBTBBK;
    //付方开户地区代码
    private String DBTBBK;
    //付方帐号
    private String DBTACC;
    //付方帐户名
    private String DBTNAM;
    //付方公司名
    private String C_DBTREL;
    //付方开户行
    private String DBTBNK;
    //付方行地址
    private String DBTADR;
    //收方开户地区
    private String C_CRTBBK;
    //收方开户地区代码
    private String CRTBBK;
    //收方帐号
    private String CRTACC;
    //收方帐号名
    private String CRTNAM;
    //收方大额行号
    private String RCVBRD;
    //币种
    private String C_CCYNBR;
    //币种代码
    private String CCYNBR;
    //交易金额
    private String TRSAMT;
    //用途
    private String NUSAGE;
    //经办日期
    private String OPRDAT;
    //业务参考号
    private String YURREF;
    //流程实例号
    private String REQNBR;
    //业务摘要
    private String BUSNAR;
    //业务请求状态
    private String C_REQSTS;
    //业务请求状态代码
    private String REQSTS;
    //业务处理结果
    private String C_RTNFLG;
    //业务处理结果代码
    private String RTNFLG;
    //操作别名
    private String OPRALS;
    //结果摘要
    private String RTNNAR;
    //退票日期
    private String RTNDAT;
    //经办用户登录名
    private String LGNNAM;
    //经办用户名
    private String USRNAM;
    //业务种类
    private String TRSTYP;
    //收费方式
    private String FEETYP;
    //收方公私标志
    private String RCVTYP;
    //汇款业务状态
    private String BUSSTS;
    //受理机构
    private String TRSBRN;
    //转汇机构
    private String TRNBRN;
    //保留字段
    private String RSV30Z;


}
