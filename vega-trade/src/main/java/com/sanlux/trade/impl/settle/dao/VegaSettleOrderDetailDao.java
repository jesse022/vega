/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.impl.settle.dao;

import io.terminus.parana.settle.impl.dao.SettleOrderDetailDao;
import org.springframework.stereotype.Repository;

/**
 * @author : panxin
 */
@Repository
public class VegaSettleOrderDetailDao extends SettleOrderDetailDao {

//    /**
//     * 封装日汇总信息 group by seller id
//     * @param startAt 开始日期
//     * @param endAt 截止日期
//     * @return 日汇总对象
//     */
//    public List<SellerTradeDailySummary> generateSellerTradeDailySummary(Date startAt, Date endAt){
//        //return getSqlSession().selectList(sqlId("sumSellerSettlement"), ImmutableMap.of("startAt", startAt, "endAt", endAt));
//        // TODO 所有数据一次读 or 多次读
//        Map<String, Object> criteria = ImmutableMap.of(
//                "startAt", startAt,
//                "endAt", endAt
//        );
//
//        Paging<SettleOrderDetail> paging = this.paging(criteria);
//        List<SettleOrderDetail> orderDetailList = paging.getData();
//        SellerTradeDailySummary summary = new SellerTradeDailySummary();
//
//        // 汇总信息
//        for (SettleOrderDetail order : orderDetailList) {
//            summary.setOrderCount(MoreObjects.firstNonNull(summary.getOrderCount(), 0) + 1); // 总订单数
//            summary.setOriginFee(MoreObjects.firstNonNull(summary.getOriginFee(), 0L) + order.getOriginFee());
//            summary.setSellerDiscount(MoreObjects.firstNonNull(summary.getSellerDiscount(), 0L) + order.getSellerDiscount());;
//            summary.setPlatformDiscount(MoreObjects.firstNonNull(summary.getPlatformDiscount(), 0L) + order.getPlatformDiscount());
//            summary.setShipFee(MoreObjects.firstNonNull(summary.getShipFee(), 0L) + order.getShipFee());
//            summary.setActualPayFee(MoreObjects.firstNonNull(summary.getActualPayFee(), 0L) + order.getActualPayFee());
//            summary.setGatewayCommission(MoreObjects.firstNonNull(summary.getGatewayCommission(), 0L) + order.getGatewayCommission());
//            summary.setPlatformCommission(MoreObjects.firstNonNull(summary.getPlatformCommission(), 0L) + order.getPlatformCommission());
//            summary.setSellerReceivableFee(MoreObjects.firstNonNull(summary.getSellerReceivableFee(), 0L) + order.getSellerReceivableFee());
//        }
//        summary.setRefundOrderCount(0);
//        summary.setRefundFee(0L);
//        // summary.setSummaryType(summary.getSellerId());
//        summary.setSumAt(new Date());
//
//        return null;
//    }
    /*
            select  count(1) as order_count,
        0 as refund_order_count,
        seller_id as seller_id,
        seller_name as seller_name,
        sum(origin_fee) as origin_fee,
        sum(seller_discount) as seller_discount,
        sum(platform_discount) as platform_discount,
        sum(ship_fee) as ship_fee,
        sum(ship_fee_discount) as ship_fee_discount,
        0 as refund_fee,
        sum(actual_pay_fee) as actual_pay_fee,
        sum(gateway_commission) as gateway_commission,
        sum(platform_commission) as platform_commission,
        sum(seller_receivable_fee) as seller_receivable_fee,
        1 as summary_type,
        current_date() as sum_at

        from  <include refid="table_name"/>
        WHERE <![CDATA[ check_at >= #{startAt} ]]> AND <![CDATA[ check_at < #{endAt}]]>
        group by seller_id

     */

//    /**
//     * 封装日汇总信息
//     * @param startAt 开始日期
//     * @param endAt 截止日期
//     * @return 日汇总对象
//     */
//    public PlatformTradeDailySummary generatePlatformTradeDailySummary(Date startAt, Date endAt){
//        // return getSqlSession().selectOne(sqlId("sumPlatformSettlement"), ImmutableMap.of("startAt", startAt, "endAt", endAt));
//        // TODO 所有数据一次读 or 多次读
//        Map<String, Object> criteria = ImmutableMap.of(
//                "startAt", startAt,
//                "endAt", endAt
//        );
//
//        Paging<SettleOrderDetail> paging = this.paging(criteria);
//        List<SettleOrderDetail> orderDetailList = paging.getData();
//        PlatformTradeDailySummary summary = new PlatformTradeDailySummary();
//
//        // 汇总信息
//        for (SettleOrderDetail order : orderDetailList) {
//            summary.setOrderCount(MoreObjects.firstNonNull(summary.getOrderCount(), 0) + 1); // 总订单数
//            summary.setOriginFee(MoreObjects.firstNonNull(summary.getOriginFee(), 0L) + order.getOriginFee());
//            summary.setSellerDiscount(MoreObjects.firstNonNull(summary.getSellerDiscount(), 0L) + order.getSellerDiscount());;
//            summary.setPlatformDiscount(MoreObjects.firstNonNull(summary.getPlatformDiscount(), 0L) + order.getPlatformDiscount());
//            summary.setShipFee(MoreObjects.firstNonNull(summary.getShipFee(), 0L) + order.getShipFee());
//            summary.setActualPayFee(MoreObjects.firstNonNull(summary.getActualPayFee(), 0L) + order.getActualPayFee());
//            summary.setGatewayCommission(MoreObjects.firstNonNull(summary.getGatewayCommission(), 0L) + order.getGatewayCommission());
//            summary.setPlatformCommission(MoreObjects.firstNonNull(summary.getPlatformCommission(), 0L) + order.getPlatformCommission());
//            summary.setSellerReceivableFee(MoreObjects.firstNonNull(summary.getSellerReceivableFee(), 0L) + order.getSellerReceivableFee());
//        }
//        summary.setRefundOrderCount(0);
//        summary.setRefundFee(0L);
//        summary.setSummaryType(SummaryType.ALL.value());
//        summary.setSumAt(new Date());
//
//        return summary;
//    }


}
