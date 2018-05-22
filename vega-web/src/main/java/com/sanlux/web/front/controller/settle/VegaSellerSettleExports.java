/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.settle;

import com.google.common.collect.Lists;
import com.sanlux.trade.settle.criteria.VegaSellerTradeDailySummaryCriteria;
import com.sanlux.trade.settle.criteria.VegaSettleOrderDetailCriteria;
import com.sanlux.trade.settle.criteria.VegaSettleRefundOrderDetailCriteria;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.model.VegaSettleOrderDetail;
import com.sanlux.trade.settle.model.VegaSettleRefundOrderDetail;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import com.sanlux.web.front.core.utils.VegaSettleExportHandler;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.parana.settle.service.SettleRefundOrderDetailReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/settle")
public class VegaSellerSettleExports { /*} extends SettlementExports {*/

    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService sellerTradeDailySummaryReadService;
    @RpcConsumer
    private SettleRefundOrderDetailReadService settleRefundOrderDetailReadService;
    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;
    @RpcConsumer
    private ShopReadService shopReadService;

    /**
     * 订单明细
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/settle-order-export"},
            method = {RequestMethod.GET}
    )
    public void vegaExportSettleOrders(VegaSettleOrderDetailCriteria criteria, HttpServletResponse response) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        exportSettleOrders(criteria, response);
    }

    /**
     * 退款单明细
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/settle-refund-order-export"},
            method = {RequestMethod.GET}
    )
    public void vegaExportSettleRefundOrders(VegaSettleRefundOrderDetailCriteria criteria, HttpServletResponse response) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        exportSettleRefundOrders(criteria, response);
    }

    /**
     * 日汇总
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/settle-seller-summary-export"},
            method = {RequestMethod.GET}
    )
    public void vegaExportSellerTradeSummarys(VegaSellerTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());
        criteria.setSummaryType(findShopById(currentShopId()).getType());
        this.exportSellerTradeSummarys(criteria, response);
    }

    /**
     * 检查店铺信息
     */
    private void checkShopIdNotNull() {
        ParanaUser user = UserUtil.getCurrentUser();
        if (user.getShopId() == null) {
            log.warn("permission deny for user={}", user);
            throw new JsonResponseException("permission.deny");
        }
    }

    private Long currentShopId() {
        return ((ParanaUser)UserUtil.getCurrentUser()).getShopId();
    }

    private void retrieveResponse(HttpServletResponse response, String excelName) throws UnsupportedEncodingException {
        String xlsFileName = URLEncoder.encode(excelName, "UTF-8") + ".xlsx";
        response.setContentType("application/x-download");
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
        response.setHeader(headerKey, headerValue);
    }

    private void exportSettleOrders(VegaSettleOrderDetailCriteria criteria, HttpServletResponse response) {
        List<SettleOrderDetail> datas = Lists.newArrayList();
        int pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);

            Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging SettleOrderDetail by criteria = {}, cause : {}");
            }

            List<SettleOrderDetail> orderDetails = resp.getResult().getData();
            if(orderDetails.isEmpty()) {
                try {
                    retrieveResponse(response, "订单明细报表");
                    List<VegaSettleOrderDetail> dataList = Lists.newArrayList();
                    for (SettleOrderDetail order : datas) {
                        dataList.add(BeanMapper.map(order, VegaSettleOrderDetail.class));
                    }
                    VegaSettleExportHandler.exportSettleOrderDetail(
                            dataList, response.getOutputStream(), Boolean.FALSE);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("download the Excel of settle order detail info failed ");
                }
                return;
            }

            datas.addAll(orderDetails);
            ++pageNo;
        }
    }

    private void exportSettleRefundOrders(VegaSettleRefundOrderDetailCriteria criteria, HttpServletResponse response) {
        List<SettleRefundOrderDetail> datas = Lists.newArrayList();
        int pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);

            Response<Paging<SettleRefundOrderDetail>> resp =
                    settleRefundOrderDetailReadService.pagingSettleRefundOrderDetails(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paginh SettleRefundOrderDetail by criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            List<SettleRefundOrderDetail> orderDetails = resp.getResult().getData();
            if(orderDetails == null || orderDetails.size() == 0) {
                try {
                    this.retrieveResponse(response, "退款单明细报表");
                    List<VegaSettleRefundOrderDetail> dataList = Lists.newArrayList();
                    for (SettleRefundOrderDetail order : datas) {
                        dataList.add(BeanMapper.map(order, VegaSettleRefundOrderDetail.class));
                    }
                    VegaSettleExportHandler.exportSettleRefundOrderDetail(
                            dataList, response.getOutputStream(), Boolean.FALSE);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("download the Excel of settle refund order detail info failed ");
                }

                return;
            }

            datas.addAll(orderDetails);
            ++pageNo;
        }
    }

    private void exportSellerTradeSummarys(VegaSellerTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        List<VegaSellerTradeDailySummary> datas = Lists.newArrayList();
        int pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);

            Response<Paging<VegaSellerTradeDailySummary>> resp =
                    sellerTradeDailySummaryReadService.pagingSellerTradeDailySummarys(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging SellerTradeDailySummary by criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            List<VegaSellerTradeDailySummary> refundOrderDetails = resp.getResult().getData();
            if(refundOrderDetails.isEmpty()) {
                try {
                    retrieveResponse(response, "商家交易日汇总报表");
                    VegaSettleExportHandler.exportSellerTradeDailySummary(datas, response.getOutputStream(), Boolean.FALSE);
                } catch (Exception var6) {
                    var6.printStackTrace();
                    log.error("download the Excel of seller trade daily summary info failed ");
                }
                return;
            }

            datas.addAll(refundOrderDetails);
            ++pageNo;
        }
    }

    /**
     * 通过ID查店铺
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        Response<Shop> resp = shopReadService.findById(shopId);
        if (!resp.isSuccess()) {
            log.error("failed to find shop by id = {}, cause : {}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

}
