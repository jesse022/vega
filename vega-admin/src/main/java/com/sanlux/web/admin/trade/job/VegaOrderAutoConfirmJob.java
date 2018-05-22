package com.sanlux.web.admin.trade.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import com.sanlux.trade.enums.VegaShipmentStatus;
import com.sanlux.trade.service.VegaOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 超时自动确认收货job
 * Created by lujm on 2017/3/27.
 */
@Slf4j
@Component
public class VegaOrderAutoConfirmJob {
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @Autowired
    private HostLeader hostLeader;

    @Value("${order.auto.confirm.in.minutes}")
    private Integer expireMinutes;

    static final Integer BATCH_SIZE = 100;     // 每批处理数量
    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    @Scheduled(cron = "${order.auto.confirm.job.cron: 0 0 2,22 * * ?}")//每天凌晨2点,晚上10点执行
    public void handOrderExpire() {

        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("[CRON-JOB] [HANDLE-ORDER-AUTO-CONFIRM-EXPIRE] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<ShopOrder> canAutoConfirmOrders = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE, canAutoConfirmOrders);
        while (next) {
            pageNo++;
            next = batchHandle(pageNo, BATCH_SIZE, canAutoConfirmOrders);
        }
        for (ShopOrder shopOrder : canAutoConfirmOrders) {
            Response<List<Shipment>> shipmentResp =
                    shipmentReadService.findByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP);
            if (!shipmentResp.isSuccess()) {
                log.error("fail to find shipment by order id={} and order lever={},cause:{}",
                        shopOrder.getId(), OrderLevel.SHOP, shipmentResp.getError());
            } else {
                List<Shipment> shipments = shipmentResp.getResult();

                if (hasOrderExpire(shipments.get(0).getCreatedAt(), expireMinutes)) {
                    Response<Boolean> handleRes = orderWriteService.shopOrderStatusChanged(shopOrder.getId(),
                            shopOrder.getStatus(),
                            VegaOrderStatus.CONFIRMED.getValue());
                    if (!handleRes.isSuccess()) {
                        log.error("hand expire auto paid shop order:(id{}) fail,error:{}", shopOrder.getId(), handleRes.getError());
                    } else {
                        Response updateResp = shipmentWriteService.updateStatusByOrderIdAndOrderLevel(shopOrder.getId(), OrderLevel.SHOP, VegaShipmentStatus.SUCCESS_CONFIRM.value());
                        if (!updateResp.isSuccess()) {
                            log.error("fail to update shipment status by order id:{} and order level:{},cause:{}", shopOrder.getId(), OrderLevel.SHOP, updateResp.getError());
                        }
                    }
                }
            }
        }

        stopwatch.stop();
        log.info("[CRON-JOB] [HANDLE-ORDER-AUTO-CONFIRM-EXPIRE] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size, List<ShopOrder> canAutoConfirmOrders) {

        Map<String, Object> criteria = Maps.newHashMap();
        List<Integer> status = Lists.newArrayList(VegaOrderStatus.SHIPPED.getValue());//已发货
        criteria.put("status", status);
        Response<Paging<ShopOrder>> pagingRes = vegaOrderReadService.pagingShopOrder(pageNo, size, criteria);
        if (!pagingRes.isSuccess()) {
            log.error("paging shop order fail,criteria:{},error:{}", criteria, pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<ShopOrder> paging = pagingRes.getResult();
        List<ShopOrder> shopOrders = paging.getData();

        if (paging.getTotal().equals(0L) || CollectionUtils.isEmpty(shopOrders)) {
            return Boolean.FALSE;
        }
        canAutoConfirmOrders.addAll(shopOrders);

        int current = shopOrders.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }


    private boolean hasOrderExpire(Date createdAt, Integer expireMinutes) {
        return new DateTime(createdAt).isBefore(DateTime.now().minusMinutes(expireMinutes));
    }
}
