/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.enums.VegaShopType;
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
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.settle.dto.paging.PlatformTradeDailySummaryCriteria;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.service.PlatformTradeDailySummaryReadService;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.parana.settle.service.SettleRefundOrderDetailReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/settle")
public class VegaAdminSettleExports { // extends SettlementExports {

    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService sellerTradeDailySummaryReadService;
    @RpcConsumer
    private PlatformTradeDailySummaryReadService platformTradeDailySummaryReadService;
    @RpcConsumer
    private SettleRefundOrderDetailReadService settleRefundOrderDetailReadService;
    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;
    @Autowired
    private ShopCacher shopCacher;
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
    public void exportSettleOrders(VegaSettleOrderDetailCriteria criteria, HttpServletResponse response) {
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        List<SettleOrderDetail> datas = Lists.newArrayList();
        int pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);

            Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging SettleOrderDetail by criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            List<SettleOrderDetail> orderDetails = resp.getResult().getData();
            if(orderDetails.isEmpty()) {
                try {
                    retrieveResponse(response, "订单明细报表");
                    List<VegaSettleOrderDetail> detailList = generateVegaSettleOrderDetail(datas);
                    VegaSettleExportHandler.exportSettleOrderDetail(
                            detailList, response.getOutputStream(), Boolean.TRUE);
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

    /**
     * 退款单明细
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/settle-refund-order-export"},
            method = {RequestMethod.GET}
    )
    public void exportSettleRefundOrders(VegaSettleRefundOrderDetailCriteria criteria, HttpServletResponse response) {
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        List<SettleRefundOrderDetail> datas = Lists.newArrayList();
        int pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);

            Response<Paging<SettleRefundOrderDetail>> resp =
                    settleRefundOrderDetailReadService.pagingSettleRefundOrderDetails(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging SettleRefundOrderDetail by criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            List<SettleRefundOrderDetail> refundOrderDetails = resp.getResult().getData();
            if(refundOrderDetails.isEmpty()) {
                try {
                    retrieveResponse(response, "退款单明细报表");
                    List<VegaSettleRefundOrderDetail> detailList = generateVegaSettleRefundOrderDetail(datas);
                    VegaSettleExportHandler.exportSettleRefundOrderDetail(
                            detailList, response.getOutputStream(), Boolean.TRUE);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("download the Excel of settle refund order detail info failed ");
                }

                return;
            }

            datas.addAll(refundOrderDetails);
            ++pageNo;
        }
    }

    /**
     * 供应商日汇总
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/supplier/settle-seller-summary-export"},
            method = {RequestMethod.GET}
    )
    public void exportSupplierTradeSummarys(VegaSellerTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        criteria.setSummaryType(VegaShopType.SUPPLIER.value());
        String fileName = "供应商日汇总报表";
        sellerTradeDailyExport(criteria, response, fileName);
    }

    /**
     * 一级经销商日汇总
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/dealer1th/settle-seller-summary-export"},
            method = {RequestMethod.GET}
    )
    public void exportDealer1thTradeSummarys(VegaSellerTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        criteria.setSummaryType(VegaShopType.DEALER_FIRST.value());
        String fileName = "一级经销商日汇总报表";
        sellerTradeDailyExport(criteria, response, fileName);
    }

    /**
     * 二级经销商日汇总
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/dealer2ed/settle-seller-summary-export"},
            method = {RequestMethod.GET}
    )
    public void exportDealer2edTradeSummarys(VegaSellerTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        criteria.setSummaryType(VegaShopType.DEALER_SECOND.value());
        String fileName = "二级经销商日汇总报表";
        sellerTradeDailyExport(criteria, response, fileName);
    }

    /**
     * 平台日汇总
     * @param criteria 查询条件
     * @param response http响应
     */
    @RequestMapping(
            value = {"/settle-platform-summary-export"},
            method = {RequestMethod.GET}
    )
    public void exportPlatformTradeSummarys(PlatformTradeDailySummaryCriteria criteria, HttpServletResponse response) {
        List<PlatformTradeDailySummary> datas = Lists.newArrayList();
        Integer pageNo = 1;

        while(true) {
            criteria.setPageNo(pageNo);
            criteria.setPageSize(200);
            Response<Paging<PlatformTradeDailySummary>> resp = platformTradeDailySummaryReadService.pagingPlatformTradeDailySummarys(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to paging PlatformTradeDailySummary by criteria = {}, cause : {}",
                        criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }

            List<PlatformTradeDailySummary> summaries = resp.getResult().getData();
            // 查询直到数据为空
            if(summaries.isEmpty()) {
                try {
                    retrieveResponse(response, "平台交易日汇总报表");
                    VegaSettleExportHandler.exportPlatformTradeDailySummary(datas, response.getOutputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("download the Excel of platform trade daily summary info failed.");
                }
                return;
            }

            datas.addAll(summaries);
            pageNo++;
        }
    }

    /**
     * 商家日汇总
     * @param criteria 条件
     * @param response 响应
     */
    private void sellerTradeDailyExport(VegaSellerTradeDailySummaryCriteria criteria,
                                        HttpServletResponse response,
                                        String fileName) {
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

            List<VegaSellerTradeDailySummary> summaryList = resp.getResult().getData();
            if(summaryList.isEmpty()) {
                try {
                    retrieveResponse(response, fileName);
                    VegaSettleExportHandler.exportSellerTradeDailySummary(datas, response.getOutputStream(), Boolean.TRUE);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("download the Excel of seller trade daily summary info failed ");
                }
                return;
            }

            datas.addAll(summaryList);
            ++pageNo;
        }
    }

    private void retrieveResponse(HttpServletResponse response, String excelName) throws UnsupportedEncodingException {
        String xlsFileName = URLEncoder.encode(excelName, "UTF-8") + ".xlsx";
        response.setContentType("application/x-download");
        String headerKey = "Content-Disposition";
        String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
        response.setHeader(headerKey, headerValue);
    }

    private List<VegaSettleOrderDetail> generateVegaSettleOrderDetail(List<SettleOrderDetail> details) {
        List<VegaSettleOrderDetail> orderDetails = Lists.newArrayListWithCapacity(details.size());
        Map<Long, String> shopType = findShopIdWithTypes();
        details.forEach(detail -> {
            VegaSettleOrderDetail vegaSettleOrderDetail = BeanMapper.map(detail, VegaSettleOrderDetail.class);
            vegaSettleOrderDetail.setSellerType(shopType.get(detail.getSellerId()));
            orderDetails.add(vegaSettleOrderDetail);
        });
        return orderDetails;
    }

    private List<VegaSettleRefundOrderDetail> generateVegaSettleRefundOrderDetail(List<SettleRefundOrderDetail> details) {
        List<VegaSettleRefundOrderDetail> orderDetails = Lists.newArrayListWithCapacity(details.size());
        Map<Long, String> shopType = findShopIdWithTypes();
        details.forEach(detail -> {
            VegaSettleRefundOrderDetail vegaSettleOrderDetail = BeanMapper.map(detail, VegaSettleRefundOrderDetail.class);
            vegaSettleOrderDetail.setSellerType(shopType.get(detail.getSellerId()));
            orderDetails.add(vegaSettleOrderDetail);
        });
        return orderDetails;
    }

    /**
     * 通过ID查店铺
     * @param shopId 店铺ID
     * @return 店铺信息
     */
    private Shop findShopById(Long shopId) {
        return shopCacher.findShopById(shopId);
    }

    /**
     * 店铺对应的类型 desc toString
     * @return map
     */
    private Map<Long, String> findShopIdWithTypes() {
        Integer pageNo = 1;
        Integer pageSize = 100;
        Map<Long, String> shopIdWithType = Maps.newHashMap();
        while (true) {
            Response<Paging<Shop>> resp = shopReadService.pagination(null, null, null, null, pageNo, pageSize);
            if (!resp.isSuccess()) {
                log.error("failed to page shop, pageNo = {}, pageSize = {}, cause : {}",
                        pageNo, pageSize, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<Shop> paging = resp.getResult();
            List<Shop> shops = paging.getData();
            shops.forEach(shop -> {
                shopIdWithType.put(shop.getId(), VegaShopType.from(shop.getType()).toString());
            });

            Long total = paging.getTotal();
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }
        return shopIdWithType;
    }

}
