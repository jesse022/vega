package com.sanlux.item.component;

import com.sanlux.item.service.VegaDeliveryFeeTemplateReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.delivery.component.DeliveryFeeTemplateChecker;
import lombok.extern.slf4j.Slf4j;

/**
 * Author:cp
 * Created on 8/17/16.
 */
@Slf4j
public class VegaDeliveryFeeTemplateChecker implements DeliveryFeeTemplateChecker {

    @RpcConsumer
    private VegaDeliveryFeeTemplateReadService vegaDeliveryFeeTemplateReadService;

    @Override
    public boolean checkIfHasItemBindTemplate(Long deliveryFeeTemplateId) {
        Response<Boolean> checkResp = vegaDeliveryFeeTemplateReadService.checkIfHasItemBindTemplate(deliveryFeeTemplateId);
        if (!checkResp.isSuccess()) {
            log.error("fail to check if has item bind delivery fee template(id={}),cause:{}",
                    deliveryFeeTemplateId, checkResp.getError());
            throw new ServiceException(checkResp.getError());
        }
        return checkResp.getResult();
    }
}
