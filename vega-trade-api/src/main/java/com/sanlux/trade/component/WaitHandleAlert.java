package com.sanlux.trade.component;

import lombok.Data;

import java.io.Serializable;

/**
 * 待处理提醒
 * Created by songrenfei on 16/12/9
 */
@Data
public class WaitHandleAlert implements Serializable{


    /**
     * 待付款
     */
    private Long waitPay;

    /**
     * 待发货
     */
    private Long waitDeliver;

    /**
     * 待收货
     */
    private Long waitConfirmArrive;



}
