/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.settle;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
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
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
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
public class VegaSellerSettlements {

    @RpcConsumer
    private ShopReadService shopReadService;
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
            value = "/seller-daily-summary-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<VegaSellerTradeDailySummary> pagingSellerTradeDailySummarys(VegaSellerTradeDailySummaryCriteria criteria) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());
        criteria.setSummaryType(findShopById(currentShopId()).getType());

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
     * 商家日汇总详情
     * @param id 详情ID
     * @return 信息
     */
    @RequestMapping(value = "/seller-daily-detail/{id}", method = RequestMethod.GET)
    public VegaSellerTradeDailySummary sellerTradeDailySummaryDetail(@PathVariable Long id) {
        Response<VegaSellerTradeDailySummary> resp = sellerTradeDailySummaryReadService.findSellerTradeDailySummaryById(id);
        if (!resp.isSuccess()) {
            log.error("failed to find seller daily summary detail by id = {}, cause: {}",
                    id, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 订单明细
     * @param criteria 查询条件
     * @return 订单信息
     */
    @RequestMapping(
            value = "/order-detail-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<SettleOrderDetailDto> pagingSettleOrderDetails(VegaSettleOrderDetailCriteria criteria) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());

        Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging SettleOrderDetail by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSettleOrderDetailDto(resp.getResult());
    }

    /**
     * 订单明细详情
     * @param settleOrderDetailId 订单明细ID
     * @return 订单信息
     */
    @RequestMapping(
            value = "/order-detail/{settleOrderDetailId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SettleOrderDetailDto findSettleOrderDetailById(@PathVariable("settleOrderDetailId") Long settleOrderDetailId) {
        checkShopIdNotNull();
        Response<SettleOrderDetail> resp = settleOrderDetailReadService.findSettleOrderDetailById(settleOrderDetailId);
        if (!resp.isSuccess()) {
            log.error("failed to find SettleOrderDetail by id = {}, cause : {}",
                    settleOrderDetailId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        SettleOrderDetail detail = resp.getResult();
        Shop shop = findShopById(detail.getSellerId());
        checkPermissionForSeller(detail.getSellerId(), detail);
        return new SettleOrderDetailDto(detail, shop.getName(), shop.getType());
    }

    /**
     * 退款单明细
     * @param criteria 查询条件
     * @return 退款单信息
     */
    @RequestMapping(
            value = "/refund-order-detail-paging",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Paging<SettleRefundOrderDetailDto> pagingSettleRefundOrderDetails(VegaSettleRefundOrderDetailCriteria criteria) {
        checkShopIdNotNull();
        criteria.setSellerId(currentShopId());

        Response<Paging<SettleRefundOrderDetail>> resp = settleRefundOrderDetailReadService.pagingSettleRefundOrderDetails(criteria);
        if (!resp.isSuccess()) {
            log.error("failed to paging SettleRefundOrderDetail by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return generateSettleRefundOrderDetailDto(resp.getResult());
    }

    /**
     * 退款单明细详情
     * @param settleRefundOrderDetailId 退款单明细ID
     * @return 详情
     */
    @RequestMapping(
            value = "/refund-order-detail/{settleRefundOrderDetailId}",
            method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SettleRefundOrderDetailDto findSettleRefundOrderDetailById(@PathVariable("settleRefundOrderDetailId") Long settleRefundOrderDetailId) {
        checkShopIdNotNull();
        Response<SettleRefundOrderDetail> resp = settleRefundOrderDetailReadService.findSettleRefundOrderDetailById(settleRefundOrderDetailId);
        if (!resp.isSuccess()) {
            log.error("failed to find SettleOrderDetail by id = {}, cause : {}",
                    settleRefundOrderDetailId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        SettleRefundOrderDetail detail = resp.getResult();
        Shop shop = findShopById(detail.getSellerId());
        checkPermissionForSeller(detail.getSellerId(), detail);
        return new SettleRefundOrderDetailDto(detail, shop.getName(), shop.getType());
    }

    /**
     * 权限
     * @param sellerId 店铺id
     * @param viewObject target信息
     */
    private void checkPermissionForSeller(Long sellerId, Object viewObject) {
        ParanaUser user = UserUtil.getCurrentUser();
        if (!Objects.equal(sellerId, user.getShopId())) {
            log.warn("permission deny for user={} to view {}", UserUtil.getCurrentUser(), viewObject);
            throw new JsonResponseException("permission.deny");
        }
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

        Shop shop = findShopById(currentShopId());
        for (SettleOrderDetail detail : paging.getData()) {
            detailDto = new SettleOrderDetailDto();
            detailDto.setOrderDetail(detail);
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

        Shop shop = findShopById(currentShopId());

        for (SettleRefundOrderDetail detail : paging.getData()) {
            detailDto = new SettleRefundOrderDetailDto();
            detailDto.setRefundOrderDetail(detail);
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

    /**
     * 当前登录用户店铺ID
     * @return ID
     */
    private Long currentShopId() {
        return ((ParanaUser)UserUtil.getCurrentUser()).getShopId();
    }

}
