package com.sanlux.web.admin.trade.job;

import com.google.common.base.Objects;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/18/16
 * Time: 10:21 AM
 */
@Slf4j
public class VegaOrderNotPaidExpireJob{

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private HostLeader hostLeader;
    @RpcConsumer
    private ShopReadService shopReadService;

    @Value("${order.auto.cancel.in.minutes}")
    private Integer expireMinutes;

    static final Integer BATCH_SIZE = 100;     // 批处理数量
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");



    @Scheduled(cron = "${order.auto.cancel.job.cron: 0 0/10 * * * ?}")
    public void handOrderPaidExpire() {

        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("[CRON-JOB] [HANDLE-ORDER-PAID-EXPIRE] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<ShopOrder> notPaidOrders = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,notPaidOrders);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,notPaidOrders);
        }

        for (ShopOrder shopOrder : notPaidOrders){
            if(hasOrderExpire(shopOrder.getCreatedAt(),expireMinutes)){
                Response<Boolean> handleRes = orderWriteService.shopOrderStatusChanged(shopOrder.getId(),
                        shopOrder.getStatus(),
                        getNewOrderStatus(shopOrder.getShopId()));
                if(!handleRes.isSuccess()){
                    log.error("hand expire not paid shop order:(id{}) fail,error:{}",shopOrder.getId(),handleRes.getError());
                }
            }
        }

        stopwatch.stop();
        log.info("[CRON-JOB] [HANDLE-ORDER-PAID-EXPIRE] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));


    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,List<ShopOrder> notPaidOrders) {

        Map<String, Object> criteria = Maps.newHashMap();
        List<Integer> status = Lists.newArrayList(VegaOrderStatus.NOT_PAID_FIRST_DEALER.getValue(),
                VegaOrderStatus.NOT_PAID_PLATFORM.getValue(),VegaOrderStatus.NOT_PAID_SECOND_DEALER.getValue());
        criteria.put("status", status);
        Response<Paging<ShopOrder>> pagingRes = vegaOrderReadService.pagingShopOrder(pageNo, size, criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging shop order fail,criteria:{},error:{}",criteria,pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<ShopOrder> paging = pagingRes.getResult();
        List<ShopOrder> shopOrders = paging.getData();

        if (paging.getTotal().equals(0L)  || CollectionUtils.isEmpty(shopOrders)) {
            return Boolean.FALSE;
        }
        notPaidOrders.addAll(shopOrders);

        int current = shopOrders.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }




    private boolean hasOrderExpire(Date createdAt, Integer expireMinutes) {

        return new DateTime(createdAt).isBefore(DateTime.now().minusMinutes(expireMinutes));
    }

    /**
     * 根据店铺Id获取超期关闭状态
     * @param shopId 店铺id
     *
     * @return 订单状态
     */
    private Integer getNewOrderStatus(Long shopId) {
        //默认平台接单
        Integer newOrderStatus = VegaOrderStatus.TIMEOUT_CANCEL.getValue();
        Response<Shop> shopRes = shopReadService.findById(shopId);
        if (!shopRes.isSuccess()) {
            log.error("fail to find shop by shop id:{},cause:{}", shopId, shopRes.getError());
            throw new JsonResponseException(500, shopRes.getError());
        }

        //接单店铺为一级
        if (Objects.equal(VegaShopType.DEALER_FIRST.value(), shopRes.getResult().getType())) {
            newOrderStatus = VegaOrderStatus.TIMEOUT_FIRST_DEALER_CANCEL.getValue();
        }
        //接单店铺为二级
        if (Objects.equal(VegaShopType.DEALER_SECOND.value(), shopRes.getResult().getType())) {
            newOrderStatus = VegaOrderStatus.TIMEOUT_SECOND_DEALER_CANCEL.getValue();
        }
        return newOrderStatus;
    }

}
