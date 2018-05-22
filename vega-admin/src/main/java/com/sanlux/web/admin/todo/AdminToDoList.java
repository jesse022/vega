package com.sanlux.web.admin.todo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.helper.DateHelper;
import com.sanlux.item.service.VegaItemReadService;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.model.CreditAlterResume;
import com.sanlux.shop.service.CreditAlterResumeReadService;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.pay.util.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 运营后台待办提醒
 * Created by lujm on 2017/2/6
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/todo")
public class AdminToDoList {

    @RpcConsumer
    private VegaItemReadService vegaItemReadService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private CreditAlterResumeReadService creditAlterResumeReadService;
    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService vegaSellerTradeDailySummaryReadService;
    @RpcConsumer
    private UserReadService userReadService;


    /**
     * 获取待办列表数量接口
     *
     * @return Map
     */
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> count() {
        try {
            Map<String, Object> countMap = Maps.newHashMap();
            Long itemCheckCount=getCount(countItemCheck());//商品上架待审核数量
            Long sendOrderCount=getCount(countSendOrder());//待派送订单数量
            Long secondDealerApprovalCount=getCount(countSecondDealerApproval());//待审核二级经销商数量
            Long repaymentApprovalCount=getCount(countRepayment());//一级经销商待还款待审核数量
            Long firstDealerPaymentCount=getCount(countFirstDealerPayment());//一级经销商待打款数量
            Long secondDealerPaymentCount=getCount(countSecondDealerPayment());//二级经销商待打款数量
            Long todayRegisterUserCount=getCount(countTodayRegisterUser());//当日新增会员数
            Long countTodayPaymentOrder = getCount(countTodayPaymentOrder()); //今日付款订单数量
            Long countTotal = itemCheckCount + sendOrderCount + secondDealerApprovalCount +
                    repaymentApprovalCount + firstDealerPaymentCount +
                    secondDealerPaymentCount + todayRegisterUserCount + countTodayPaymentOrder;//待办提醒总数


            countMap.put("itemCheck",itemCheckCount);
            countMap.put("SendOrder",sendOrderCount);
            countMap.put("SecondDealerApproval", secondDealerApprovalCount);
            countMap.put("repaymentApproval", repaymentApprovalCount);
            countMap.put("FirstDealerPayment",firstDealerPaymentCount);
            countMap.put("SecondDealerPayment",secondDealerPaymentCount );
            countMap.put("TodayRegisterUser", todayRegisterUserCount);
            countMap.put("todayPaymentOrder", countTodayPaymentOrder);
            countMap.put("NowDate", DateHelper.formatDate(new Date()));//当前日期'yyyy-mm-dd'
            countMap.put("countTotal", countTotal);

            return countMap;
        } catch (Exception e) {
            log.error("admin count fail, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }


    /**
     * 获取商品上架待审核数量
     *
     * @return 数量
     */
    private Long countItemCheck() {
        Response<Long> longResponse=vegaItemReadService.countItemCheck();
        if (!longResponse.isSuccess()) {
            log.error("fail to count item where status = 0, cause:{}", longResponse.getError());
            return null;
        }
        return longResponse.getResult();
    }

    /**
     * 获取运营待派送订单数量
     *
     * @return 数量
     */
    private Long countSendOrder() {
        OrderCriteria orderCriteria = new OrderCriteria();
        orderCriteria.setShopId(DefaultId.PLATFROM_SHOP_ID);
        orderCriteria.setStatusStr(String.valueOf(VegaOrderStatus.PAID_WAIT_CHECK.getValue()));
        Response<Paging<OrderGroup>> ordersR = vegaOrderReadService.findBy(orderCriteria);
        if (!ordersR.isSuccess()) {
            log.error("count admin send order fail, cause:{}", ordersR.getError());
            return null;
        }
        return ordersR.getResult().getTotal();
    }

    /**
     * 获取二级经销商待审核数量
     *
     * @return 数量
     */
    private Long countSecondDealerApproval() {
        Response<Long> longResponse=vegaShopReadService.countSecondDealerApproval();
        if (!longResponse.isSuccess()) {
            log.error("count.second.dealer.approval.fail, cause:{}", longResponse.getError());
            return null;
        }
        return longResponse.getResult();
    }

    /**
     * 获取一级经销商还款待审核数量
     *
     * @return 数量
     */
    private Long countRepayment() {
        Map<String, Object> criteria = Maps.newHashMap();
        Long total=0L;
        Response<Paging<CreditAlterResume>> resp = creditAlterResumeReadService.pagingDistinctShopID(0, 20, criteria);
        if (!resp.isSuccess()) {
            log.error("failed to find credit alter resume by criteria = {}, cause: {}",
                    criteria, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        List<VegaShop> vegaShops = fromCreditToShop(resp.getResult().getData());
        if (!CollectionUtils.isEmpty(vegaShops)) {
            total=(long)vegaShops.size();
        }
        return total;
    }

    /**
     * 获取一级经销商待打款数量
     *
     * @return 数量
     */
    private Long countFirstDealerPayment() {
        Response<Long> longResponse=vegaSellerTradeDailySummaryReadService.countDealerPayment(VegaShopType.DEALER_FIRST.value());
        if (!longResponse.isSuccess()) {
            log.error("count.dealer.payment.fail.by.first, cause:{}", longResponse.getError());
            return null;
        }
        return longResponse.getResult();
    }

    /**
     * 获取二级经销商待打款数量
     *
     * @return 数量
     */
    private Long countSecondDealerPayment() {
        Response<Long> longResponse=vegaSellerTradeDailySummaryReadService.countDealerPayment(VegaShopType.DEALER_SECOND.value());
        if (!longResponse.isSuccess()) {
            log.error("count.dealer.payment.fail.by.second, cause:{}", longResponse.getError());
            return null;
        }
        return longResponse.getResult();
    }

    /**
     * 获取当日新会员注册数量
     *
     * @return 数量
     */
    @SuppressWarnings("unchecked")
    private Long countTodayRegisterUser() {
        Date ctf = LocalDate.fromDateFields(new Date()).toDate();
        Date ctt = LocalDate.fromDateFields(new Date()).plusDays(1).toDate();
        Response<Paging<User>> resp = userReadService.paging(null, null, null, null, null, null, ctf, ctt, null, null);
        if (!resp.isSuccess()) {
            log.error("count admin today register user fail, cause:{}", resp.getError());
            return null;
        }
        return resp.getResult().getTotal();
    }

    /**
     * 获取今日付款订单数量
     *
     * @return 数量
     */
    private Long countTodayPaymentOrder() {
        OrderCriteria orderCriteria =new OrderCriteria();
        Date startAt = DateTime.now().withTimeAtStartOfDay().toDate();
        Date endAt = DateTime.now().plusDays(1).withTimeAtStartOfDay().toDate();
        orderCriteria.setStartAt(startAt);
        orderCriteria.setEndAt(endAt);
        Response<Long> longResponse=vegaOrderReadService.countTodayPaymentOrder(orderCriteria);
        if (!longResponse.isSuccess()) {
            log.error("count.today.payment.order.fail, cause:{}", longResponse.getError());
            return null;
        }
        return longResponse.getResult();
    }

    private Long getCount(Long object) {
        return Arguments.isNull(object) ? 0 : object;
    }

    private List<VegaShop> fromCreditToShop(List<CreditAlterResume> creditAlterResumes) {
        List<Long> ShopIds = Lists.transform(creditAlterResumes, CreditAlterResume::getShopId);
        Response<List<VegaShop>> vegaShopListByShopIds = vegaShopReadService.findFirstDealerByShopIds(ShopIds);
        if (!vegaShopListByShopIds.isSuccess()) {
            log.error("fromCreditToShop fail, creditAlterResumes:{}, cause:{}",
                    creditAlterResumes, vegaShopListByShopIds.getError());
            return Collections.emptyList();
        }
        return vegaShopListByShopIds.getResult();
    }

}
