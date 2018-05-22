package com.sanlux.pay.direct.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by liangfujie on 16/10/27
 */
@Data
public class QueryRequestBody implements Serializable {
    private static final long serialVersionUID = 7143168656218201737L;

    //业务类别,可空
    private String BUSCOD;
    //起始日期
    private String BGNDAT;
    //结束日期
    private String ENDDAT;
    //日期类型,可空
    private String DATFLG;
    //最小金额,可空
    private String MINAMT;
    //最大金额,可空
    private String MAXAMT;
    //业务参考号,可空
    private String YURREF;
    //业务处理结果,可空
    private String RTNFLG;
    //经办用户,可空
    private String OPRLGN;


}
