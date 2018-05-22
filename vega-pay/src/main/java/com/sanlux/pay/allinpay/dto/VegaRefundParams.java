package com.sanlux.pay.allinpay.dto;

import io.terminus.pay.model.RefundParams;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/19/16
 * Time: 10:47 AM
 */
@Data
public class VegaRefundParams extends RefundParams implements Serializable{


    private static final long serialVersionUID = 397073604947735351L;
    /**
     * 支付时间
     */
    private Date payAt;
}
