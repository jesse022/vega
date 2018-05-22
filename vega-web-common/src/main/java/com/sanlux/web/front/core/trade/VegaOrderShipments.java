package com.sanlux.web.front.core.trade;

import com.sanlux.trade.dto.KdNiaoLogisticsDto;
import com.sanlux.web.front.core.trade.service.LogisticsService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.express.model.ExpressCompany;
import io.terminus.parana.express.service.ExpressCompanyReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

/**
 * Created by liangfujie on 16/12/9
 */
@Slf4j
@RestController
@RequestMapping("/api/vega")
public class VegaOrderShipments {

    @Autowired
    private LogisticsService logisticsService;

    @RpcConsumer
    private ExpressCompanyReadService expressCompanyReadService;


    /**
     * 获取退款单快递信息
     *
     * @param shipmentCorpCode 快递公司编号
     * @param shipmentSerialNo 快递单号
     * @return
     */

    @RequestMapping(value = "/refund/logistics", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public KdNiaoLogisticsDto findRefundLogisticsInfo(String shipmentCorpCode, String shipmentSerialNo) {
        KdNiaoLogisticsDto kdNiaoLogisticsDto = new KdNiaoLogisticsDto();
        if (StringUtils.isEmpty(shipmentCorpCode)){
            log.error("shipmentCorpCode should not be null!");
            throw new JsonResponseException("logistics.shipment.corpCode.null");

        }
        if (StringUtils.isEmpty(shipmentSerialNo)){
            log.error("shipmentSerialNo should not be null!");
            throw new JsonResponseException("logistics.shipment.serialNo.null");

        }
        
        try {
            String steps = logisticsService.getLogisticsInfo(shipmentCorpCode, shipmentSerialNo);
            if (Arguments.isNull(steps)) {
                log.error("steps is null,please check params,shipmentCorpCode{},shipmentSerialNo{}", shipmentCorpCode, shipmentSerialNo);
                throw new JsonResponseException("get.refund.logistics.info.failed");
            }
            Response<ExpressCompany> expressCompanyResponse = expressCompanyReadService.findExpressCompanyByCode(shipmentCorpCode);
            if (!expressCompanyResponse.isSuccess()) {
                log.error("find expressCompanyName failed,cause{}", expressCompanyResponse.getError());
                throw new JsonResponseException("find.express.company.name.failed");
            }

            ExpressCompany expressCompany = expressCompanyResponse.getResult();
            kdNiaoLogisticsDto.setShipmentCorpName(expressCompany.getName());
            kdNiaoLogisticsDto.setShipmentSerialNo(shipmentSerialNo);
            kdNiaoLogisticsDto.setSteps(steps);
            return kdNiaoLogisticsDto;

        } catch (Exception e) {
            log.error("find refund logistics info failed,cause{}", e.getMessage());
            throw new JsonResponseException("get.refund.logistics.info.failed");
        }

    }
}
