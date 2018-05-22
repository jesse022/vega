package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 运营销售情况统计报表类目金额统计DTO
 * Created by lujm on 2017/9/21.
 */
@Data
public class ReportCategoryDto implements Serializable{

    private static final long serialVersionUID = 704639422090891411L;

    /*类目ID*/
    private Long categoryId;

    /*类目名称*/
    private String categoryName;

    /*类目金额*/
    private Long categoryFee;
}
