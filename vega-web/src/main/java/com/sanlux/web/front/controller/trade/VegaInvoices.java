package com.sanlux.web.front.controller.trade;

import com.google.common.base.Objects;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.service.InvoiceReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 买家发票管理扩展Controller
 *
 * Created by lujm on 2017/6/30.
 */
@Slf4j
@RestController
@RequestMapping("api/buyer/invoice")
public class VegaInvoices {
    @RpcConsumer
    private InvoiceReadService invoiceReadService;


    /**
     * 根据发票Id获取发票详情
     * @param invoiceId 发票Id
     * @return 发票详情
     */
    @RequestMapping(value = "/{invoiceId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Invoice> findUserInvoiceById(@PathVariable("invoiceId") Long invoiceId) {
        final Long userId = UserUtil.<ParanaUser>getCurrentUser().getId();
        Response<Invoice> findResp = invoiceReadService.findInvoiceById(invoiceId);
        if (!findResp.isSuccess()) {
            log.error("fail to find invoice by id:{},cause:{}", invoiceId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        if(!Objects.equal(findResp.getResult().getUserId(), userId)) {
            log.error("the invoice(id={}) is not belong to buyer(id={})", invoiceId, userId);
            throw new JsonResponseException("invoice.not.belong.to.buyer");
        }
        return Response.ok(findResp.getResult());
    }

    /**
     * 根据用户Id,获取买家默认发票信息
     * @param userId 用户Id
     * @return 发票信息
     */
    @RequestMapping(value = "/get/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Invoice> findDefaultInvoiceByUserId(@PathVariable("userId") Long userId) {
        Response<List<Invoice>> findResp = invoiceReadService.findByUserId(userId);
        if (!findResp.isSuccess()) {
            log.error("fail to find invoice by userId:{},cause:{}", userId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        List<Invoice> invoices = findResp.getResult();
        if (!Arguments.isNullOrEmpty(invoices)) {
            for (Invoice invoice : invoices) {
                if (invoice.getIsDefault()) {
                    return Response.ok(invoice);
                }
            }
            return Response.ok(invoices.get(0));
        }
        return Response.fail("user.invoice.find.fail");
    }
}
