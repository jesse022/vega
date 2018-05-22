package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 运营销售情况统计报表访客类型DTO
 * Created by lujm on 2017/9/21.
 */
@Data
public class ReportTerminalDto implements Serializable {

    private static final long serialVersionUID = -654836567208546008L;

    /*访客类型ID*/
    private Integer terminalId;

    /*访客类型名称*/
    private String terminalType;

    /*数量,默认为0*/
    private Integer sum = 0;
}
