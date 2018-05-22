package com.sanlux.trade.dto;

import io.terminus.common.model.Paging;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.shop.model.Shop;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 订单预览页 商品信息 由于需要分页 所以运费是在分页时计算的
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/17/16
 * Time: 4:57 PM
 */
@Data
public class PreOrderSku implements Serializable{

    private static final long serialVersionUID = -7872002224159331420L;

    /**
     * 当一级经销商下单时 需要根据店铺拆单,每个店铺代表一个订单
     */
    private List<Shop> shops;

    /**
     * 当前订单店铺名称
     */
    private String currentShopName;

    /**
     * 当前订单店铺Id
     */
    private Long currentShopId;

    /**
     * 当前下单人身份 BUYER(普通用户) DEALER_FIRST(一级经销商) DEALER_SECOND(二级经销商)
     */
    private String roleName;

    /**
     * 当前订单商品价格
     */
    private Long originFee;

    /**
     * 当前订单的商品分页
     */
    private Paging<RichSku> paging;
}
