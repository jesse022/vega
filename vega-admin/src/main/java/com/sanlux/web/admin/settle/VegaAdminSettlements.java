/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.admin.settle;

import com.google.common.collect.Lists;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.settle.criteria.VegaSellerTradeDailySummaryCriteria;
import com.sanlux.trade.settle.criteria.VegaSettleOrderDetailCriteria;
import com.sanlux.trade.settle.criteria.VegaSettleRefundOrderDetailCriteria;
import com.sanlux.trade.settle.dto.PlatformTradeDailySummaryDto;
import com.sanlux.trade.settle.dto.SellerTradeDailySummaryDto;
import com.sanlux.trade.settle.dto.SettleOrderDetailDto;
import com.sanlux.trade.settle.dto.SettleRefundOrderDetailDto;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.dto.paging.PlatformTradeDailySummaryCriteria;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.SummaryType;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.service.PlatformTradeDailySummaryReadService;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.parana.settle.service.SettleRefundOrderDetailReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/settle")
public class VegaAdminSettlements {

    @RpcConsumer
    private ShopReadService shopReadService;
    @RpcConsumer
    private PlatformTradeDailySummaryReadService platformTradeDailySummaryReadService;
    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService sellerTradeDailySummaryReadService;
    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;
    @RpcConsumer
    private SettleRefundOrderDetailReadService settleRefundOrderDetailReadService;

    /**
     * 订单日汇总
     * @param criteria 查询条件
     * @return 汇总信息
     */
    @RequestMapping(
            value = "/platform-daily-summary-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public PlatformTradeDailySummaryDto platformTradeDailySummaryPaging(PlatformTradeDailySummaryCriteria criteria) {
        criteria.setSummaryType(SummaryType.ALL.value());
        Response<Paging<PlatformTradeDailySummary>> resp = platformTradeDailySummaryReadService.
                pagingPlatformTradeDailySummarys(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging PlatformTradeDailySummary by criteria = {}, cause : {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generatePlatformTradeDailyDto(resp.getResult());
    }

    /**
     * 一级经销商日汇总
     * @param criteria 查询条件
     * @return 信息
     */
    @RequestMapping(
            value = "/dealer-daily-summary",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<VegaSellerTradeDailySummary> dealerDaySummary(VegaSellerTradeDailySummaryCriteria criteria) {
        criteria.setSummaryType(VegaShopType.DEALER_FIRST.value());
        Response<Paging<VegaSellerTradeDailySummary>> resp = sellerTradeDailySummaryReadService
                .pagingSellerTradeDailySummarys(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging dealer daily summary by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 二级经销商日汇总
     * @param criteria 查询条件
     * @return 信息
     */
    @RequestMapping(
            value = "/dealer2ed-daily-summary",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<VegaSellerTradeDailySummary> dealer2thDaySummary(VegaSellerTradeDailySummaryCriteria criteria) {
        criteria.setSummaryType(VegaShopType.DEALER_SECOND.value());
        Response<Paging<VegaSellerTradeDailySummary>> resp = sellerTradeDailySummaryReadService
                .pagingSellerTradeDailySummarys(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging dealer daily summary by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 供应商日汇总
     * @param criteria 查询条件
     * @return 汇总信息
     */
    @RequestMapping(
            value = "/supplier-daily-summary",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<VegaSellerTradeDailySummary> supplierDaySummary(VegaSellerTradeDailySummaryCriteria criteria) {
        criteria.setSummaryType(VegaShopType.SUPPLIER.value());
        Response<Paging<VegaSellerTradeDailySummary>> resp = sellerTradeDailySummaryReadService
                .pagingSellerTradeDailySummarys(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging supplier daily summary by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 订单明细
     * @param criteria 查询条件
     * @return 结果
     */
    @RequestMapping(
            value = "/order-detail-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<SettleOrderDetailDto> pagingSettleOrderDetails(VegaSettleOrderDetailCriteria criteria) {
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging SettleOrderDetail by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSettleOrderDetailDto(resp.getResult());
    }

    /**
     * 订单明细对应的详情
     * @param settleOrderDetailId 订单明细ID
     * @return 详情
     */
    @RequestMapping(
            value = "/order-detail/{settleOrderDetailId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SettleOrderDetailDto findSettleOrderDetailById(@PathVariable("settleOrderDetailId") Long settleOrderDetailId) {
        Response<SettleOrderDetail> resp = settleOrderDetailReadService.findSettleOrderDetailById(settleOrderDetailId);
        if (!resp.isSuccess()) {
            log.error("failed to find SettleOrderDetail by id = {}, cause : {}",
                    settleOrderDetailId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        SettleOrderDetail detail = resp.getResult();
        Shop shop = findShopById(detail.getSellerId());
        return new SettleOrderDetailDto(detail, shop.getName(), shop.getType());
    }

    /**
     * 退款单明细
     * @param criteria 查询条件
     * @return 信息
     */
    @RequestMapping(
            value = "/refund-order-detail-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<SettleRefundOrderDetailDto> pagingSettleRefundOrderDetails(VegaSettleRefundOrderDetailCriteria criteria) {
        criteria.setCheckStatus(CheckStatus.CHECK_SUCCESS.value());
        Response<Paging<SettleRefundOrderDetail>> resp = settleRefundOrderDetailReadService.pagingSettleRefundOrderDetails(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging SettleRefundOrderDetail by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSettleRefundOrderDetailDto(resp.getResult());
    }

    /**
     * 退款单明细对应的详情
     * @param settleRefundOrderDetailId 明细ID
     * @return 详情
     */
    @RequestMapping(
            value = {"/refund-order-detail/{settleRefundOrderDetailId}"},
            method = {RequestMethod.GET},
            produces = {"application/json"}
    )
    public SettleRefundOrderDetailDto findSettleRefundOrderDetailById(@PathVariable("settleRefundOrderDetailId") Long settleRefundOrderDetailId) {
        Response<SettleRefundOrderDetail> resp = settleRefundOrderDetailReadService.findSettleRefundOrderDetailById(settleRefundOrderDetailId);
        if (!resp.isSuccess()) {
            log.error("failed to find SettleOrderDetail by id = {}, cause : {}",
                    settleRefundOrderDetailId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        SettleRefundOrderDetail detail = resp.getResult();
        Shop shop = findShopById(detail.getSellerId());
        return new SettleRefundOrderDetailDto(detail, shop.getName(), shop.getType());
    }

    /**
     * 设置信息
     * @param paging 分页信息
     * @return 返回信息
     */
    private PlatformTradeDailySummaryDto generatePlatformTradeDailyDto(Paging<PlatformTradeDailySummary> paging) {
        PlatformTradeDailySummaryDto summaryDto = new PlatformTradeDailySummaryDto();
        summaryDto.setPaging(paging);
        return summaryDto;
    }

    /**
     * 设置信息
     * @param paging 分页信息
     * @return 返回信息
     */
    private SellerTradeDailySummaryDto generateSellerTradeDailyDto(Paging<SellerTradeDailySummary> paging) {
        SellerTradeDailySummaryDto summaryDto = new SellerTradeDailySummaryDto();
        summaryDto.setPaging(paging);
        return summaryDto;
    }

    /**
     * 生成订单明细DTO
     * @param paging 订单明细分页
     * @return 订单明细DTO
     */
    private Paging<SettleOrderDetailDto> generateSettleOrderDetailDto(Paging<SettleOrderDetail> paging) {
        SettleOrderDetailDto detailDto = null;
        List<SettleOrderDetailDto> detailDtoList = Lists.newArrayListWithCapacity(paging.getData().size());
        for (SettleOrderDetail detail : paging.getData()) {
            detailDto = new SettleOrderDetailDto();
            detailDto.setOrderDetail(detail);

            Shop shop = findShopById(detail.getSellerId());
            detailDto.setShopName(shop.getName());
            detailDto.setShopType(shop.getType());

            detailDtoList.add(detailDto);
        }
        return new Paging<>(paging.getTotal(), detailDtoList);
    }

    /**
     * 生成退款单单明细DTO
     * @param paging 退款单明细分页
     * @return 订单明细DTO
     */
    private Paging<SettleRefundOrderDetailDto> generateSettleRefundOrderDetailDto(Paging<SettleRefundOrderDetail> paging) {
        SettleRefundOrderDetailDto detailDto = null;
        List<SettleRefundOrderDetailDto> detailDtoList = Lists.newArrayListWithCapacity(paging.getData().size());
        for (SettleRefundOrderDetail detail : paging.getData()) {
            detailDto = new SettleRefundOrderDetailDto();
            detailDto.setRefundOrderDetail(detail);

            Shop shop = findShopById(detail.getSellerId());
            detailDto.setShopName(shop.getName());
            detailDto.setShopType(shop.getType());

            detailDtoList.add(detailDto);
        }
        return new Paging<>(paging.getTotal(), detailDtoList);
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
