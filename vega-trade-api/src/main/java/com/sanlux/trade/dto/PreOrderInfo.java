package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 下单预览页信息
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/18/16
 * Time: 4:04 PM
 */
@Data
public class PreOrderInfo implements Serializable{

    private static final long serialVersionUID = -7250674571270848031L;


    /**
     * 商品原价
     */
    private Long totalFee=0L;

    /**
     * 接单店铺id
     */
    private Long receiveShopId;

}
