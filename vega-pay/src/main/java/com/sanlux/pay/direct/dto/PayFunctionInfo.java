package com.sanlux.pay.direct.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlElement;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/26
 */
@Data
public class PayFunctionInfo implements Serializable{
    private static final long serialVersionUID = -7399603461041166474L;
    //业务参考号
    private String YURREF;
    //期望日期,可为空,默认为当前日期
    private String EPTDAT;
    //期望时间,可为空,默认"0000000"
    private String EPTTIM;
    //付款账号
    private String DBTACC;
    //付方开户地区代码
    private String DBTBBK;
    //交易金额
    private String TRSAMT;
    //交易币代码,目前只支持10-人民币
    private String CCYNBR;
    //结算代码,N:普通,F:快速
    private String STLCHN;
    //用途,对应对账单中的摘要
    private String NUSAGE;
    //业务摘要,用于企业付款时填写说明或者备注
    private String BUSNAR;
    //收款账号
    private String CRTACC;
    //收方帐户名,可空
    private String CRTNAM;
    //收方长户名,可空
    private String LRVEAN;
    //收方行号
    private String BRDNBR;
    //系统内外标志,Y：招行；N：非招行
    private String BNKFLG;
    //收方开户行,可空
    private String CRTBNK;
    //城市代码,可空
    private String CTYCOD;
    //收方省份
    private String CRTPVC;
    //收方城市
    private String CRTCTY;
    //收方县/区,可空
    private String CRTDTR;
    //收方电子邮件,可空
    private String NTFCH1;
    //收方移动电话,可空
    private String NTFCH2;
    //收方编号,可空
    private String CRTSQN;
    //业务种类,100001=普通汇兑(默认),101001=慈善捐款,101002 =其他,可空
    private String TRSTYP ;
    //行内收方账号户名校验,1校验,空或其他值不校验,可空
    private String RCVCHK ;
    //保留字段,可空
    private String RSVFLD;


}
