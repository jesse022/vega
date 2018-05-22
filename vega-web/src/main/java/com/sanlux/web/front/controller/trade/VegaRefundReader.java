package com.sanlux.web.front.controller.trade;

import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.web.front.core.trade.VegaOrderComponent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.dto.RefundDetail;
import io.terminus.parana.order.dto.RefundList;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.web.core.order.RefundReadLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@Controller
@Slf4j
public class VegaRefundReader {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private VegaOrderComponent  vegaOrderComponent;

    @RpcConsumer
    private RefundReadService refundReadService;



    @RequestMapping(value = "/api/vega/seller/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<RefundList> findForSeller(@RequestParam Map<String, String> refundCriteria) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        refundCriteria.put("shopId", shopId.toString());
        Response<Paging<RefundList>> findResp = refundReadLogic.refundPaging(refundCriteria);
        if (!findResp.isSuccess()) {
            log.error("fail to find refund for seller(shop id={}),criteria={},cause:{}",
                    shopId, refundCriteria, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }

        ParanaUser user = UserUtil.getCurrentUser();
        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);
        //供应商登录 要展示供货价,订单相关金额要替换以供货价为基础的金额
        if(userType.equals(OrderUserType.SUPPLIER)){

            List<RefundList> refundLists = findResp.getResult().getData();
            for (RefundList refundList : refundLists){
                vegaOrderComponent.replaceOrderPrice(refundList.getRefund(),refundList.getSkuOrders());
            }
        }
        return findResp.getResult();
    }



    @RequestMapping(value = "/api/vega/refund/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RefundDetail findDetail(@RequestParam Long refundId,
                                   @RequestParam(value = "supplierIsBuyer",defaultValue = "0",required = false) String supplierIsBuyer,
                                   @RequestParam Integer pageNo,
                                   @RequestParam Integer size) {
        Response<RefundDetail> detailRes = refundReadService.findDetailById(refundId,pageNo,size);
        if (!detailRes.isSuccess()) {
            log.error("fail to find refund detail by id={},error:{}",
                    refundId, detailRes.getError());
            throw new JsonResponseException(detailRes.getError());
        }

        RefundDetail detail = detailRes.getResult();
        ParanaUser user = UserUtil.getCurrentUser();
        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);

        //供应商作为卖家登录 要展示供货价,订单相关金额要替换以供货价为基础的金额
        if(userType.equals(OrderUserType.SUPPLIER) && !Objects.equals(supplierIsBuyer,"1")){
            vegaOrderComponent.replaceOrderPrice(detail.getRefund(),detail.getSkuOrderPaging().getData());
        }

        return detail;
    }




}
