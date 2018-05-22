package com.sanlux.web.admin.trade;

import com.sanlux.pay.allinpay.enums.AlinnpayRefundHandleStatus;
import com.sanlux.web.admin.trade.job.AllinpayRefundQueryJob;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.dto.RefundDetail;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.web.core.order.RefundReadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@Controller
@Slf4j
public class VegaAdminRefundReader {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private VegaOrderComponent  vegaOrderComponent;
    @Autowired
    private AllinpayRefundQueryJob allinpayRefundQueryJob;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private RefundReadService refundReadService;


    @RequestMapping(value = "/api/admin/vega/refund/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RefundDetail findDetail(@RequestParam Long refundId,
                                   @RequestParam Integer pageNo,
                                   @RequestParam Integer size) {
        Response<RefundDetail> detailRes = refundReadService.findDetailById(refundId,pageNo,size);
        if (!detailRes.isSuccess()) {
            log.error("fail to find refund detail by id={},error:{}",
                    refundId, detailRes.getError());
            throw new JsonResponseException(detailRes.getError());
        }

        return detailRes.getResult();
    }


    @RequestMapping(value = "/api/admin/vega/refund/query", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public AlinnpayRefundHandleStatus refundQuery(@RequestParam Long refundId) {
        Response<Refund> refundRes = refundReadService.findById(refundId);
        if (!refundRes.isSuccess()) {
            log.error("fail to find refund by id={},error:{}",
                    refundId, refundRes.getError());
            throw new JsonResponseException(refundRes.getError());
        }

        Response<AlinnpayRefundHandleStatus> response = allinpayRefundQueryJob.refundQuery(refundRes.getResult());

        if(!response.isSuccess()){
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }





}
