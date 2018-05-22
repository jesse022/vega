package com.sanlux.web.admin.trade;

import com.google.common.collect.Lists;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.trade.dto.KdNiaoLogisticsDto;
import com.sanlux.web.front.core.trade.service.LogisticsService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.web.core.express.dto.OrderExpressTrack;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/12/12
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/vega")
public class VegaAdminLogistics {

    @Autowired
    private LogisticsService logisticsService;

    @RpcConsumer
    private ShipmentReadService shipmentReadService;



    /**
     * 获取快递信息
     *
     * @param orderId   订单ID
     * @param orderType 订单类型
     * @return 快递的JSON信息
     */
    @RequestMapping(value = "/logistics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<KdNiaoLogisticsDto> findLogisticsInfo(@RequestParam("orderId") Long orderId, @RequestParam("orderType") Integer orderType) {

        List<OrderExpressTrack> lists = logisticsService.findExpressTrack(orderId, orderType);
        if (Arguments.isNull(lists)) {
            log.error("kdNiao info is null,orderId{},orderType{}", orderId, orderType);
            throw new JsonResponseException("kdNiao.info.null");
        }
        List<KdNiaoLogisticsDto> logisticsDtoList = Lists.newArrayList();
        try {
            for (OrderExpressTrack orderExpressTrack : lists) {
                KdNiaoLogisticsDto kdNiaoDto = new KdNiaoLogisticsDto();
                kdNiaoDto.setShipmentCorpName(orderExpressTrack.getShipmentCorpName());
                kdNiaoDto.setShipmentId(orderExpressTrack.getShipmentId());
                kdNiaoDto.setShipmentSerialNo(orderExpressTrack.getShipmentSerialNo());

                Response<Shipment> shipmentResponse = shipmentReadService.findById(kdNiaoDto.getShipmentId());
                if (!shipmentResponse.isSuccess()) {
                    log.error("shipment find failed , id{}", kdNiaoDto.getShipmentId());
                    throw new JsonResponseException(shipmentResponse.getError());
                }
                String steps = logisticsService.getLogisticsInfo(shipmentResponse.getResult().getShipmentCorpCode(),
                        kdNiaoDto.getShipmentSerialNo());
                Map<String,String> mapExtra = shipmentResponse.getResult().getExtra();
                if(mapExtra!=null){
                    String extra = mapExtra.get(SystemConstant.SHIPMENT_EXTRA_COMMENT);
                    kdNiaoDto.setExtra(extra);
                }
                kdNiaoDto.setSteps(steps);
                logisticsDtoList.add(kdNiaoDto);
            }
            return logisticsDtoList;
        } catch (Exception e) {
            log.error("find kdNiao info failed,,cause{}", e.getMessage());
            throw new JsonResponseException("find.kdNiao.failed");
        }

    }

}
