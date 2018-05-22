package com.sanlux.pay.allinpay.dto;

import io.terminus.pay.model.PaymentParams;
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
public class VegaPaymentParams extends PaymentParams implements Serializable {


    private static final long serialVersionUID = -3841920705245458677L;
    /**
     * 支付时间
     */
    private Date payAt;
}
