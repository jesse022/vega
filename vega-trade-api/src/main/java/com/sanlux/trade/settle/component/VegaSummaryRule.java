/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.settle.component;

import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算汇总计算
 *
 * @author : panxin
 */
@Slf4j
public class VegaSummaryRule {

    @RpcConsumer
    private ShopReadService shopReadService;

    public VegaSummaryRule() {
    }

    public List<VegaSellerTradeDailySummary> sellerDaily(List<VegaSellerTradeDailySummary> forwardList,
                                                     List<VegaSellerTradeDailySummary> backwardList) {

        Map<Long, VegaSellerTradeDailySummary> allSummaryMapBySellerId = new HashMap<>();

        log.info("Vega handle SellerTradeDailySummary, forwardList = {}, backwardList = {}",
                forwardList, backwardList);

         Shop shop = null;
        // 正向汇总信息
        for (VegaSellerTradeDailySummary summary : forwardList) {
            VegaSellerTradeDailySummary tmp = BeanMapper.map(summary, VegaSellerTradeDailySummary.class);

            // 汇总类型, 用于判断经销商, 供应商
            shop = findShopById(summary.getSellerId());
            tmp.setSummaryType(shop.getType());

            allSummaryMapBySellerId.put(summary.getSellerId(), tmp);
        }

        //如果对应商家只有正向的订单, 则不进入循环
        for (VegaSellerTradeDailySummary backward : backwardList) {
            if (allSummaryMapBySellerId.containsKey(backward.getSellerId())) { //如果包含退款单
                VegaSellerTradeDailySummary forward = allSummaryMapBySellerId.get(backward.getSellerId());

                allSummaryMapBySellerId.get(backward.getSellerId()).setRefundOrderCount(backward.getRefundOrderCount());
                allSummaryMapBySellerId.get(backward.getSellerId()).setRefundFee(backward.getRefundFee());
                allSummaryMapBySellerId.get(backward.getSellerId()).setActualPayFee(forward.getActualPayFee() - backward.getActualPayFee());
                allSummaryMapBySellerId.get(backward.getSellerId()).setSellerReceivableFee(forward.getSellerReceivableFee() - backward.getSellerReceivableFee());

            } else {//如果商家只有退款单, 则将这个退款汇总加入到新增列表中
                // SellerTradeDailySummary summary = new SellerTradeDailySummary();
                VegaSellerTradeDailySummary summary = BeanMapper.map(backward, VegaSellerTradeDailySummary.class);
                summary.setSellerId(backward.getSellerId());
                summary.setSellerName(backward.getSellerName());
                summary.setRefundOrderCount(backward.getRefundOrderCount());
                summary.setOrderCount(backward.getOrderCount());
                summary.setActualPayFee(-backward.getActualPayFee());
                summary.setSellerReceivableFee(-backward.getSellerReceivableFee());

                // 汇总类型, 用于判断经销商, 供应商
                shop = findShopById(summary.getSellerId());
                summary.setSummaryType(shop.getType());

                allSummaryMapBySellerId.put(backward.getSellerId(), summary);
            }
        }
        return new ArrayList<>(allSummaryMapBySellerId.values());
    }

    /**
     * 查询店铺
     *
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        Response<Shop> resp = shopReadService.findById(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by shopId = {}, cause : {}", shopId, resp.getError());
        }
        return resp.getResult();
    }
}
