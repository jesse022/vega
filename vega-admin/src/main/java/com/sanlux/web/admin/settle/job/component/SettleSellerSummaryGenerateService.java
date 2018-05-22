package com.sanlux.web.admin.settle.job.component;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.SettleConstants;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.pay.credit.constants.CreditPayConstants;
import com.sanlux.shop.criteria.VegaShopCriteria;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.enums.VegaDirectPayInfoStatus;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.service.VegaDirectPayInfoReadService;
import com.sanlux.trade.settle.component.VegaSummaryRule;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.settle.dto.paging.SettleOrderDetailCriteria;
import io.terminus.parana.settle.dto.paging.SettleRefundOrderDetailCriteria;
import io.terminus.parana.settle.enums.SummaryType;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.service.SettleOrderDetailReadService;
import io.terminus.parana.settle.service.SettleRefundOrderDetailReadService;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 11/7/16
 * Time: 10:40 PM
 */
@Service
@Slf4j
public class SettleSellerSummaryGenerateService {

    @RpcConsumer
    private SettleOrderDetailReadService settleOrderDetailReadService;
    @RpcConsumer
    private SettleRefundOrderDetailReadService settleRefundOrderDetailReadService;
    @Autowired
    private VegaSummaryRule summaryRule;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private VegaDirectPayInfoReadService vegaDirectPayInfoReadService;
    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService sellerTradeDailySummaryReadService;
    @RpcConsumer
    private VegaSellerTradeDailySummaryWriteService sellerTradeDailySummaryWriteService;
    @RpcConsumer
    private ShopReadService shopReadService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public Response<Boolean> generateSellerTradeDailySummary(Date sumAt) {
        try {
            //sumAt = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
            Date endAt = new DateTime(sumAt.getTime()).plusDays(1).toDate();

            Integer pageNo = 1;
            Integer pageSize = 200;
            VegaShopCriteria shopCriteria = new VegaShopCriteria();
            shopCriteria.setPageNo(pageNo);
            shopCriteria.setPageSize(pageSize);

            // 遍历店铺信息, 再根据店铺ID查询订单详情, 再手动汇总
            while (true) {
                Response<Paging<VegaShop>> resp = vegaShopReadService.paging(shopCriteria);
                if (!resp.isSuccess()) {
                    log.error("failed to paging shopIds, pageNo = {}, pageSize = {}, cause : {}",
                            pageNo, pageSize, resp.getError());
                    throw new JsonResponseException(500, resp.getError());
                }
                Paging<VegaShop> paging = resp.getResult();
                List<VegaShop> vegaShops = paging.getData();

                for (VegaShop vegaShop : vegaShops) {
                    Long shopId = vegaShop.getShop().getId();
                    Integer shopType = vegaShop.getShop().getType();

                    Paging<SettleOrderDetail> orderDetailPaging = settleOrderDetailPaging(sumAt, endAt, shopId);
                    Paging<SettleRefundOrderDetail> refundOrderDetailPaging = settleRefundOrderDetailPaging(sumAt, endAt, shopId);

                    if (orderDetailPaging.getData().isEmpty() && refundOrderDetailPaging.getData().isEmpty()) {
                        log.debug("shop id = {}, settle (refund) order detail data is empty, sumAt = {}.",
                                shopId, sumAt);
                        continue;
                    }

                    log.debug("SettleOrderDetail : {}", orderDetailPaging.getData());
                    log.debug("SettleRefundOrderDetail : {}", refundOrderDetailPaging.getData());

                    List<VegaSellerTradeDailySummary> forwardList = generateSellerTradeDailySummary(orderDetailPaging, shopType);
                    List<VegaSellerTradeDailySummary> backwardList = generateSellerTradeDailyRefundSummary(refundOrderDetailPaging, shopType);

                    // 因为一次只会生成一个店铺的日汇总, 在这直接计算经销商佣金即可
                    // checkDealerCommissionSummary(forwardList, backwardList);

                    List<VegaSellerTradeDailySummary> allSummaryList = summaryRule.sellerDaily(forwardList, backwardList);

                    log.debug("all SummaryList = {}", allSummaryList);

                    String transStatus = checkTransStatus(shopId, sumAt, shopType);
                    Map<String, String> extra = null;

                    log.info("Summary Shop = {}", vegaShop.getShop());
                    log.info("Summary Detail = {}", allSummaryList.toString());

                    for (VegaSellerTradeDailySummary summary : allSummaryList) {
                        extra = MoreObjects.firstNonNull(summary.getExtra(), Maps.newHashMap());
                        extra.put(SettleConstants.TRANS_STATUS, transStatus);

                        summary.setTransStatus(Integer.valueOf(transStatus));
                        summary.setExtra(extra);
                        summary.setSummaryType(shopType);
                        summary.setSumAt(sumAt);
                    }

                    log.info("UpdatedSummary Detail = {}", allSummaryList.toString());

                    sellerTradeDailySummaryWriteService.generateSellerTradeDailySummary(allSummaryList);
                }

                Long total = paging.getTotal();
                shopCriteria.setPageNo(pageNo);
                if (pageNo * pageSize > total) {
                    break;
                }
                pageNo++;
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("generateSellerTradeDailySummary fail, sumAt={}, cause={}",
                    sumAt, Throwables.getStackTraceAsString(e));
            return Response.fail("generate.seller.trade.daily.summary.fail");
        }
    }

    /**
     * 订单日汇总
     *
     * @param paging     信息
     * @param shopType   汇总店铺类型
     * @return 信息
     */
    private List<VegaSellerTradeDailySummary> generateSellerTradeDailySummary(Paging<SettleOrderDetail> paging, Integer shopType) {
        List<SettleOrderDetail> orderDetails = paging.getData();
        if (orderDetails.isEmpty()) {
            return Collections.emptyList();
        }

        List<VegaSellerTradeDailySummary> summaryList = Lists.newArrayList();
        VegaSellerTradeDailySummary summary = null;
        // Map<String, String> extra = null;
        Long dealerCommission = 0L;

        summary = new VegaSellerTradeDailySummary();
        for (SettleOrderDetail order : orderDetails) {

            summary.setSellerId(order.getSellerId());
            summary.setSellerName(order.getSellerName());
            summary.setSummaryType(SummaryType.FORWARD.value());
            summary.setRefundFee(0L);
            summary.setRefundOrderCount(0);
            summary.setSumAt(new Date());

            summary.setOrderCount(MoreObjects.firstNonNull(summary.getOrderCount(), 0) + 1);
            summary.setOriginFee(MoreObjects.firstNonNull(summary.getOriginFee(), 0L) + order.getOriginFee());
            summary.setSellerDiscount(MoreObjects.firstNonNull(summary.getSellerDiscount(), 0L) + order.getSellerDiscount());
            summary.setPlatformDiscount(MoreObjects.firstNonNull(summary.getPlatformDiscount(), 0L) + order.getPlatformDiscount());
            summary.setShipFee(MoreObjects.firstNonNull(summary.getShipFee(), 0L) + order.getShipFee());
            summary.setShipFeeDiscount(MoreObjects.firstNonNull(summary.getShipFeeDiscount(), 0L) + order.getShipFeeDiscount());
            summary.setActualPayFee(MoreObjects.firstNonNull(summary.getActualPayFee(), 0L) + order.getActualPayFee());
            summary.setGatewayCommission(MoreObjects.firstNonNull(summary.getGatewayCommission(), 0L) + order.getGatewayCommission());
            summary.setPlatformCommission(MoreObjects.firstNonNull(summary.getPlatformCommission(), 0L) + order.getPlatformCommission());

            // 1.专属会员信用额度支付不计入商家应收款,不需要打款操作,订单明细里正常显示
            // 2.普通信用支付接单店铺是一级经销商时(对应为二级下单,一级接单),也不计入商家应收款,不需要打款操作,订单明细里正常显示
            if (!Objects.equal(CreditPayConstants.MEMBER_PAY_CHANNEL, order.getChannel())
                    && !Objects.equal(CreditPayConstants.MEMBER_WAP_PAY_CHANNEL, order.getChannel())
                    ) {
                if (!Objects.equal(VegaShopType.DEALER_FIRST.value(), shopType)
                        || (Objects.equal(VegaShopType.DEALER_FIRST.value(), shopType)
                            && !Objects.equal(CreditPayConstants.PAY_CHANNEL, order.getChannel())
                            && !Objects.equal(CreditPayConstants.WAP_PAY_CHANNEL, order.getChannel()))
                        ) {
                    summary.setSellerReceivableFee(MoreObjects.firstNonNull(summary.getSellerReceivableFee(), 0L) + order.getSellerReceivableFee());
                }
            }

            // extra = MoreObjects.firstNonNull(order.getExtra(), Maps.newHashMap());
            // dealerCommission += Long.valueOf(String.valueOf(MoreObjects.firstNonNull(
            //         extra.get(SettleConstants.DEALER_COMMISSION), 0L)));
            dealerCommission += order.getCommission1();
        }
        summary.setCommission1(dealerCommission);

        // extra = Maps.newHashMap();
        // extra.put(SettleConstants.DEALER_COMMISSION_SUMMARY, String.valueOf(dealerCommission));
        // summary.setExtra(extra);

        summaryList.add(summary);
        return summaryList;
    }

    /**
     * 退款单日汇总
     *
     * @param paging     信息
     * @param shopType   汇总店铺类型
     * @return 信息
     */
    private List<VegaSellerTradeDailySummary> generateSellerTradeDailyRefundSummary(Paging<SettleRefundOrderDetail> paging, Integer shopType) {
        List<SettleRefundOrderDetail> orderDetails = paging.getData();
        if (orderDetails.isEmpty()) {
            return Collections.emptyList();
        }

        List<VegaSellerTradeDailySummary> summaryList = Lists.newArrayList();
        VegaSellerTradeDailySummary summary = null;
        // Map<String, String> extra = null;
        Long dealerCommission = 0L;

        summary = new VegaSellerTradeDailySummary();
        for (SettleRefundOrderDetail order : orderDetails) {

            summary.setSellerId(order.getSellerId());
            summary.setSellerName(order.getSellerName());
            summary.setSummaryType(SummaryType.BACKWARD.value());
            summary.setActualPayFee(0L);
            summary.setOrderCount(0);
            summary.setSumAt(new Date());

            summary.setRefundOrderCount(MoreObjects.firstNonNull(summary.getRefundOrderCount(), 0) + 1);
            summary.setRefundFee(MoreObjects.firstNonNull(summary.getRefundFee(), 0L) + order.getActualRefundFee());
            summary.setOriginFee(MoreObjects.firstNonNull(summary.getOriginFee(), 0L) + order.getOriginFee());
            summary.setSellerDiscount(MoreObjects.firstNonNull(summary.getSellerDiscount(), 0L) + order.getSellerDiscount());
            summary.setPlatformDiscount(MoreObjects.firstNonNull(summary.getPlatformDiscount(), 0L) + order.getPlatformDiscount());
            summary.setShipFee(MoreObjects.firstNonNull(summary.getShipFee(), 0L) + order.getShipFee());
            summary.setShipFeeDiscount(MoreObjects.firstNonNull(summary.getShipFeeDiscount(), 0L) + order.getShipFeeDiscount());
            summary.setGatewayCommission(MoreObjects.firstNonNull(summary.getGatewayCommission(), 0L) + order.getGatewayCommission());
            summary.setPlatformCommission(MoreObjects.firstNonNull(summary.getPlatformCommission(), 0L) + order.getPlatformCommission());

            // 1.专属会员信用支付退款不计入商家应扣款,不需要打款操作,订单明细里正常显示
            // 2.普通信用支付接单店铺是一级经销商时(对应为二级下单,一级接单),也不计入商家应扣款,不需要打款操作,订单明细里正常显示
            if (!Objects.equal(CreditPayConstants.MEMBER_PAY_CHANNEL, order.getChannel())
                    && !Objects.equal(CreditPayConstants.MEMBER_WAP_PAY_CHANNEL, order.getChannel())
                    ) {
                if (!Objects.equal(VegaShopType.DEALER_FIRST.value(), shopType)
                        || (Objects.equal(VegaShopType.DEALER_FIRST.value(), shopType)
                        && !Objects.equal(CreditPayConstants.PAY_CHANNEL, order.getChannel())
                        && !Objects.equal(CreditPayConstants.WAP_PAY_CHANNEL, order.getChannel()))
                        ) {
                    summary.setSellerReceivableFee(MoreObjects.firstNonNull(summary.getSellerReceivableFee(), 0L) + order.getSellerDeductFee());
                }
            }

            // extra = MoreObjects.firstNonNull(order.getExtra(), Maps.newHashMap());
            // dealerCommission += Long.valueOf(String.valueOf(MoreObjects.firstNonNull(
            //       extra.get(SettleConstants.DEALER_COMMISSION), 0L)));
            dealerCommission += order.getCommission1();
        }
        summary.setCommission1(dealerCommission);

        // extra = Maps.newHashMap();
        // extra.put(SettleConstants.DEALER_COMMISSION_SUMMARY, String.valueOf(dealerCommission));
        // summary.setExtra(extra);

        summaryList.add(summary);
        return summaryList;
    }

    /**
     * 查询汇总信息
     *
     * @param sellerId    商家ID
     * @param sumAt       汇总日期
     * @param summaryType 汇总类型
     * @return 信息
     */
    private Optional<VegaSellerTradeDailySummary> findSellerTradeDailySummaryBySellerId(Long sellerId,
                                                                                    Date sumAt,
                                                                                    Integer summaryType) {
        Response<VegaSellerTradeDailySummary> resp = sellerTradeDailySummaryReadService.
                findBySellerIdAndSumAtAndSummaryType(sellerId, sumAt, summaryType);
        if (!resp.isSuccess()) {
            log.error("failed to find SellerTradeDailySummary by sellerId = {}, sumAt = {}, summaryType = {}, " +
                    "cause : {}", sellerId, sumAt, summaryType, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        VegaSellerTradeDailySummary summary = resp.getResult();
        return Optional.fromNullable(summary);
    }

    /**
     * 查询打款信息
     *
     * @param summaryId 汇总ID
     * @return 信息
     */
    private Optional<VegaDirectPayInfo> findDirectInfoBySummaryId(Long summaryId) {
        Response<VegaDirectPayInfo> resp = vegaDirectPayInfoReadService.findByOrderId(summaryId);
        if (!resp.isSuccess()) {
            log.error("failed to find VegaDirectPayInfo by orderId = {}, cause : {}",
                    summaryId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        VegaDirectPayInfo directPayInfo = resp.getResult();
        if (directPayInfo == null) {
            return Optional.absent();
        }
        return Optional.of(directPayInfo);
    }

    /**
     * 查询打款状态
     *
     * @param sellerId    商家ID
     * @param sumAt       汇总日期
     * @param summaryType 汇总类型(店铺类型)
     * @return 状态
     */
    private String checkTransStatus(Long sellerId,
                                    Date sumAt,
                                    Integer summaryType) {
        // 查询商家对应汇总信息
        Optional<VegaSellerTradeDailySummary> summaryOptional = findSellerTradeDailySummaryBySellerId(
                sellerId, sumAt, summaryType);
        if (!summaryOptional.isPresent()) {
            return String.valueOf(VegaDirectPayInfoStatus.WAIT_PAY.value());
        }

        // 查询汇总信息对应的打款信息
        VegaSellerTradeDailySummary summary = summaryOptional.get();
        Optional<VegaDirectPayInfo> payInfoOptional = findDirectInfoBySummaryId(summary.getId());
        if (payInfoOptional.isPresent()) {
            return String.valueOf(payInfoOptional.get().getStatus());
        }

        // 查不到则是未打款状态
        return String.valueOf(VegaDirectPayInfoStatus.WAIT_PAY.value());
    }

    private Paging<SettleOrderDetail> settleOrderDetailPaging(Date checkStart, Date checkEnd, Long shopId) {
        Integer pageNo = 1;
        Integer pageSize = 50;
        SettleOrderDetailCriteria criteria = new SettleOrderDetailCriteria();
        criteria.setSumAtStart(checkStart);
        criteria.setSumAtEnd(checkEnd);
        criteria.setSellerId(shopId);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);

        Paging<SettleOrderDetail> paging = null;
        List<SettleOrderDetail> orderDetails = Lists.newArrayList();
        while (true) {
            Response<Paging<SettleOrderDetail>> resp = settleOrderDetailReadService.pagingSettleOrderDetails(criteria);
            if(!resp.isSuccess()){
                log.error("paging settle order detail fail checkStart : {}, checkEnd : {}, shopId : {}, error : {}",
                        DFT.print(new DateTime(checkStart)), DFT.print(new DateTime(checkEnd)),shopId);
                throw new ServiceException(resp.getError());
            }
            paging = resp.getResult();
            orderDetails.addAll(paging.getData());

            Long total = paging.getTotal();
            criteria.setPageNo(pageNo);
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }
        return new Paging<>((long) orderDetails.size(), orderDetails);
    }

    private Paging<SettleRefundOrderDetail> settleRefundOrderDetailPaging(Date checkStart, Date checkEnd, Long shopId) {
        Integer pageNo = 1;
        Integer pageSize = 50;
        SettleRefundOrderDetailCriteria criteria = new SettleRefundOrderDetailCriteria();
        criteria.setSumAtStart(checkStart);
        criteria.setSumAtEnd(checkEnd);
        criteria.setSellerId(shopId);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);

        Paging<SettleRefundOrderDetail> paging = null;
        List<SettleRefundOrderDetail> orderDetails = Lists.newArrayList();
        while (true) {
            Response<Paging<SettleRefundOrderDetail>> resp = settleRefundOrderDetailReadService
                    .pagingSettleRefundOrderDetails(criteria);
            if(!resp.isSuccess()){
                log.error("paging settle order detail fail checkStart : {}, checkEnd : {},shopId : {}, error : {}",
                        DFT.print(new DateTime(checkStart)), DFT.print(new DateTime(checkEnd)),shopId);
                throw new ServiceException(resp.getError());
            }
            paging = resp.getResult();
            orderDetails.addAll(paging.getData());

            Long total = paging.getTotal();
            criteria.setPageNo(pageNo);
            if (pageNo * pageSize > total) {
                break;
            }
            pageNo++;
        }

        return new Paging<>((long) orderDetails.size(), orderDetails);
    }

}
