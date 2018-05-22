package com.sanlux.trade.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
@Data
public class IntegrationOrderCriteria  extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = -7101979164735966704L;

    private Long id;

    /**
     * 购买用户ID
     */
    private Long buyerId;

    /**
     *  订单状态
     */
    private Integer status;

    /**
     * 购买者姓名
     */
    private String buyerName;

    /**
     * 购买者手机
     */
    private String buyerPhone;
}
