///*
// * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
// */
//
//package com.sanlux.trade.settle.dto;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.google.common.base.MoreObjects;
//import com.google.common.base.Strings;
//import com.sanlux.common.constants.SettleConstants;
//import io.terminus.common.utils.JsonMapper;
//import io.terminus.parana.common.constants.JacksonType;
//import io.terminus.parana.settle.dto.SettleFee;
//import io.terminus.parana.settle.model.SettleOrderDetail;
//import io.terminus.parana.settle.model.SettleRefundOrderDetail;
//import io.terminus.parana.settle.model.Settlement;
//import lombok.Data;
//
//import java.util.Collections;
//import java.util.Map;
//
///**
// * @author : panxin
// */
//@Data
//public class VegaSettleFee extends SettleFee {
//    private static final long serialVersionUID = 5058964955133663351L;
//
//    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
//
//    private String extraJson;
//
//    private Map<String, String> extra;
//
//    /**
//     * 支付平台佣金和运费由平台承担
//     * @return 商家应收款
//     */
//    public Long getSellerReceivableFee() {
//        // 一级经销商佣金
//        Long dealerCommission = 0L;
//        if (this.extra != null) {
//            dealerCommission = Long.valueOf(String.valueOf(MoreObjects.firstNonNull(
//                    extra.get(SettleConstants.DEALER_COMMISSION), 0)));
//        }
//        // 商家应收款 = 订单金额 - 商家优惠 + 运费 - 平台抽佣 - 一级经销商抽佣
//        return getOriginFee() - getSellerDiscount() + getShipFee() - getPlatformCommission() - dealerCommission;
//    }
//
//    public Settlement toSettlement() {
//        Settlement settlement = new Settlement();
//        settlement.setOriginFee(getOriginFee());
//        settlement.setSellerDiscount(getSellerDiscount());
//        settlement.setPlatformDiscount(getPlatformDiscount());
//        settlement.setShipFee(getShipFee());
//        settlement.setShipFeeDiscount(getShipFeeDiscount());
//        settlement.setPlatformCommission(getPlatformCommission());
//        settlement.setGatewayCommission(getGatewayCommission());
//        settlement.setActualFee(getActualFee());
//        settlement.setExtra(extra);
//        return settlement;
//    }
//
//    public SettleOrderDetail toSettleOrderDetail() {
//        SettleOrderDetail detail = new SettleOrderDetail();
//        detail.setOriginFee(getOriginFee());
//        detail.setSellerDiscount(getSellerDiscount());
//        detail.setPlatformDiscount(getPlatformDiscount());
//        detail.setShipFee(getShipFee());
//        detail.setShipFeeDiscount(getShipFeeDiscount());
//        detail.setPlatformCommission(getPlatformCommission());
//        detail.setGatewayCommission(getGatewayCommission());
//        detail.setActualPayFee(getActualFee());
//        detail.setSellerReceivableFee(getSellerReceivableFee());
//        detail.setExtra(extra);
//        return detail;
//    }
//
//    public SettleRefundOrderDetail toSettleRefundOrderDetail() {
//        SettleRefundOrderDetail detail = new SettleRefundOrderDetail();
//        detail.setOriginFee(getOriginFee());
//        detail.setSellerDiscount(getSellerDiscount());
//        detail.setPlatformDiscount(getPlatformDiscount());
//        detail.setShipFee(getShipFee());
//        detail.setShipFeeDiscount(getShipFeeDiscount());
//        detail.setPlatformCommission(getPlatformCommission());
//        detail.setGatewayCommission(getGatewayCommission());
//        detail.setActualRefundFee(getActualFee());
//        detail.setSellerDeductFee(getSellerReceivableFee());
//        detail.setExtra(extra);
//        return detail;
//    }
//
//    public void setExtraJson(String extraJson) throws Exception {
//        this.extraJson = extraJson;
//        if (Strings.isNullOrEmpty(extraJson)) {
//            this.extra = Collections.emptyMap();
//        } else {
//            this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
//        }
//    }
//
//    public void setExtra(Map<String, String> extra) {
//        this.extra = extra;
//        if (extra == null || extra.isEmpty()) {
//            this.extraJson = null;
//        } else {
//            try {
//                this.extraJson = objectMapper.writeValueAsString(extra);
//            } catch (Exception e) {
//                //ignore this exception
//            }
//        }
//    }
//
//}
