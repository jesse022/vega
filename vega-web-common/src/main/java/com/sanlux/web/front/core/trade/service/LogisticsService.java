package com.sanlux.web.front.core.trade.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.sanlux.web.front.core.logistics.KdNiaoHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.web.core.express.dto.ExpressTrack;
import io.terminus.parana.web.core.express.dto.OrderExpressTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/10/18
 */
@Slf4j
@Service
public class LogisticsService {

    private KdNiaoHelper kdNiaoHelper;

    private LoadingCache<Map<String, String>, ExpressTrack> expressTrackCache;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;


    //电商ID
    @Value("${kdNiao.EBusinessID: 1267177 }")
    private String EBusinessID;
    //电商加密私钥，快递鸟提供，注意保管，不要泄漏
    @Value("${kdNiao.AppKey: f89790e1-9182-40d9-8321-ab6ddca94b21 }")
    private String AppKey;
    //请求url
    @Value("${kdNiao.ReqURL: http://api.kdniao.cc/Ebusiness/EbusinessOrderHandle.aspx }")
    private String ReqURL;

    public String getLogisticsInfo(String expCode, String expNo) throws Exception {
        kdNiaoHelper = new KdNiaoHelper(EBusinessID, AppKey, ReqURL);
        return kdNiaoHelper.getOrderTracesByJson(expCode, expNo);
    }


    /**
     * 根据订单ID和类型获取相应订单快递信息
     *
     * @param orderId   订单id
     * @param orderType 订单级别
     * @return 订单相关信息, 注意三力士中只有总单发货, 这里List实则只有一条信息
     */
    public List<OrderExpressTrack> findExpressTrack(Long orderId, Integer orderType) {
        OrderLevel orderLevel = OrderLevel.fromInt(MoreObjects.firstNonNull(orderType, 1));
        Response<List<Shipment>> shipmentResp =
                shipmentReadService.findByOrderIdAndOrderLevel(orderId, orderLevel);
        if (!shipmentResp.isSuccess()) {
            log.error("fail to find shipment by order id={} and order lever={},cause:{}",
                    orderId, orderLevel, shipmentResp.getError());
            throw new JsonResponseException(shipmentResp.getError());
        }
        List<Shipment> shipments = shipmentResp.getResult();
        if (orderLevel == OrderLevel.SHOP && CollectionUtils.isEmpty(shipments)) {
            Response<List<SkuOrder>> skuOrderResp = skuOrderReadService.findByShopOrderId(orderId);
            if (!skuOrderResp.isSuccess()) {
                log.error("fail to find skuOrders by shopOrderId:{},cause:{}",
                        orderId, skuOrderResp.getError());
                throw new JsonResponseException(skuOrderResp.getError());
            }

            List<Long> skuOrderIds = Lists.transform(skuOrderResp.getResult(), SkuOrder::getId);

            Response<List<Shipment>> shipmentResponce =
                    shipmentReadService.findByOrderIdsAndOrderLevel(skuOrderIds, OrderLevel.SKU);
            if (!shipmentResponce.isSuccess()) {
                log.error("fail to find shipments by orderIds:{},level:{},cause:{}",
                        skuOrderIds, OrderLevel.SKU, shipmentResponce.getError());
                throw new JsonResponseException(shipmentResponce.getError());
            }

            shipments.addAll(shipmentResponce.getResult());
        }

        if (CollectionUtils.isEmpty(shipments)) {
            return Collections.emptyList();
        }
        List<OrderExpressTrack> orderExpressTracks = Lists.newArrayListWithCapacity(shipments.size());

        OrderExpressTrack orderExpressTrack;
        for (Iterator shipment = shipments.iterator(); shipment.hasNext(); orderExpressTracks.add(orderExpressTrack)) {
            Shipment shipmentNext = (Shipment) shipment.next();
            orderExpressTrack = new OrderExpressTrack();
            orderExpressTrack.setShipmentId(shipmentNext.getId());
            orderExpressTrack.setShipmentCorpName(shipmentNext.getShipmentCorpName());
            orderExpressTrack.setShipmentSerialNo(shipmentNext.getShipmentSerialNo());

            try {
                ExpressTrack expressTrack = expressTrackCache.getUnchecked(
                        ImmutableMap.of(shipmentNext.getShipmentCorpCode(), shipmentNext.getShipmentSerialNo()));
                orderExpressTrack.setState(expressTrack.getState());
                orderExpressTrack.setOfficialQueryUrl(expressTrack.getOfficialQueryUrl());
                orderExpressTrack.setSteps(expressTrack.getSteps());
            } catch (Exception e) {
                log.error("fail to find order express track for order(id={}),order type:{},cause:{}",
                        orderId, orderType, Throwables.getStackTraceAsString(e));
            }
        }

        return orderExpressTracks;


    }



}
