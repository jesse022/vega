package com.sanlux.web.front.core.trade;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.VegaNoteType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.*;
import io.terminus.parana.web.core.component.order.CommonRefundLogic;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/13/16
 * Time: 4:35 PM
 */
@Component
@Slf4j
public class VegaOrderComponent {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @Autowired
    private CommonRefundLogic commonRefundLogic;



    /**
     * 获取店铺订单id
     * @param orderId 店铺订单或sku订单id
     * @param orderType 订单类型
     * @return 店铺订单id
     */
    public Response<Long> getShopOrderId(Long orderId, Integer orderType){
        if(orderType.equals(OrderLevel.SHOP.getValue())){
            return Response.ok(orderId);
        }else {
            Response<SkuOrder> skuOrderRes = skuOrderReadService.findById(orderId);
            if(!skuOrderRes.isSuccess()){
                log.error("find sku order by id:{} fail,error:{}",orderId,skuOrderRes.getError());
                return Response.fail(skuOrderRes.getError());
            }
            Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(skuOrderRes.getResult().getOrderId());
            if(!shopOrderRes.isSuccess()){
                log.error("find shop order by id:{} fail,error:{}",orderId,shopOrderRes.getError());
                return Response.fail(shopOrderRes.getError());
            }

            return Response.ok(shopOrderRes.getResult().getId());
        }

    }



    public void replaceOrderPrice(Refund refund,List<SkuOrder> skuOrders){


        Response<List<OrderRefund>> orderRefundRes = refundReadService.findOrderIdsByRefundId(refund.getId());
        if(!orderRefundRes.isSuccess()){
            log.error("find order ids by refund id:{} fail,error:{}",refund.getId(),orderRefundRes.getError());
            return;
        }
        List<OrderRefund> orderRefunds = orderRefundRes.getResult();
        OrderRefund orderRefund = orderRefunds.get(0);
        //替换shopOrder的价格
        Long fee =0l;
        //替换当前页面skuOrder的价格
        for (SkuOrder skuOrder  : skuOrders){
            Map<String,String> tags = skuOrder.getTags();
            String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
            if(!Strings.isNullOrEmpty(orderSkuSellerPrice)){
                skuOrder.setOriginFee(skuOrder.getQuantity()*Long.valueOf(orderSkuSellerPrice));
            }
            skuOrder.setFee(skuOrder.getOriginFee());//todo 暂不考虑优惠
            fee+=skuOrder.getFee();
        }

        ShopOrder shopOrder = getShopOrderById(skuOrders.get(0).getOrderId());
        Integer shipFee =shopOrder.getShipFee();

        switch (orderRefund.getOrderLevel()) {
            case SHOP:
                if(Arguments.notNull(refund.getTags())&&refund.getTags().containsKey(SystemConstant.IS_SHIPPED)){
                    //未发货 运费全退
                    if(refund.getTags().get(SystemConstant.IS_SHIPPED).equals(SystemConstant.NOT_SHIPPED)){
                        fee+=shipFee;
                    }
                }

                break;
            case SKU:

                if(Arguments.notNull(refund.getTags())&&refund.getTags().containsKey(SystemConstant.IS_SHIPPED)){
                    //未发货 运费拆分
                    if(refund.getTags().get(SystemConstant.IS_SHIPPED).equals(SystemConstant.NOT_SHIPPED)){

                        fee+=getSkuShipfee(shopOrder,skuOrders.get(0));

                    }
                }

                break;
            default:
                log.error("order refund (id={}) level invalid", orderRefund.getId());
        }

        refund.setFee(fee);

    }


    private ShopOrder getShopOrderById(Long shopOrderId){
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(shopOrderId);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",shopOrderId,shopOrderRes.getError());

            throw new JsonResponseException(shopOrderRes.getError());
        }

        return shopOrderRes.getResult();
    }

    private List<SkuOrder> getSkuOrderByShopOrderId(Long shopOrderId){
        Response<List<SkuOrder> > skuOrderRes = skuOrderReadService.findByShopOrderId(shopOrderId);
        if(!skuOrderRes.isSuccess()){
            log.error("find sku order by id:{} fail,error:{}",shopOrderId,skuOrderRes.getError());

            throw new JsonResponseException(skuOrderRes.getError());
        }

        return skuOrderRes.getResult();
    }

    private Long getSkuShipfee(ShopOrder shopOrder,SkuOrder skuOrderRefund){

        Long shopOrderOriginFee =0L;
        List<SkuOrder> skuOrders = getSkuOrderByShopOrderId(shopOrder.getId());
        ImmutableMap<Long,Sku> skuIdAndSkuMap = getSkuIdAndSkuMap(skuOrders);
        //替换当前页面skuOrder的价格
        for (SkuOrder skuOrder  : skuOrders){
            Sku sku = skuIdAndSkuMap.get(skuOrder.getSkuId());
            shopOrderOriginFee+=skuOrder.getQuantity()*Long.valueOf(sku.getPrice());
        }

        //运费, 发货前可退,未发货不退
        return new BigDecimal(shopOrder.getShipFee())
                .multiply(new BigDecimal(skuOrderRefund.getOriginFee()))
                .divide(new BigDecimal(shopOrderOriginFee), BigDecimal.ROUND_HALF_UP).longValue();
    }

    private ImmutableMap<Long,Sku> getSkuIdAndSkuMap(List<SkuOrder> skuOrders){

        List<Long> skuIds = Lists.transform(skuOrders, new Function<SkuOrder, Long>() {
            @Nullable
            @Override
            public Long apply(SkuOrder input) {
                return input.getSkuId();
            }
        });

        Response<List<Sku>> skuRes = skuReadService.findSkusByIds(skuIds);
        if(!skuRes.isSuccess()){
            log.error("fail to find sku  by ids {} for order paging error:{}",
                    skuIds, skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        ImmutableMap<Long,Sku> skuIdAndSkuMap = Maps.uniqueIndex(skuRes.getResult(), new Function<Sku, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable Sku input) {
                if (Arguments.isNull(input)) {
                    return 0L;
                }
                return input.getId();
            }
        });

        return skuIdAndSkuMap;
    }


    public void setRefundSellerNote(Long refundId,String sellerNote){

        Refund refund = commonRefundLogic.getRefund(refundId);
        Refund update = new Refund();
        update.setId(refundId);
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }
        extraMap.put(SystemConstant.SELLER_NOTE,sellerNote);
        update.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(update);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) set seller reject note:{} fail,error:{}",refundId,sellerNote,updateRes.getError());
        }
    }



    public void setRefundExpressInfo(Long refundId,String expressCompanyName,String expressNo,String annexUrl){

        Refund refund = commonRefundLogic.getRefund(refundId);
        Refund update = new Refund();
        update.setId(refundId);
        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }
        extraMap.put(SystemConstant.EXPRESS_COMPANY_NAME,expressCompanyName);
        extraMap.put(SystemConstant.EXPRESS_NO,expressNo);
        extraMap.put(SystemConstant.ANNEX_URL,annexUrl);
        update.setExtra(extraMap);
        Response<Boolean> updateRes = refundWriteService.update(update);
        if(!updateRes.isSuccess()){
            log.error("update refund(id:{}) set express company name:{}  no:{} fail,error:{}",refundId,expressCompanyName,expressNo,updateRes.getError());
        }
    }


    public void setOrderRejectSellerNote(Long orderId,String sellerNote){

        ShopOrder shopOrder = getShopOrderById(orderId);
        Map<String,String> extraMap = shopOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }
        extraMap.put(SystemConstant.SELLER_NOTE,sellerNote);
        Response<Boolean> updateRes = orderWriteService.updateOrderExtra(orderId,OrderLevel.SHOP,extraMap);
        if(!updateRes.isSuccess()){
            log.error("update shop order(id:{}) set extra:{} fail,error:{}",orderId,extraMap.toString(),updateRes.getError());
        }
    }

    /**
     * 订单添加备注
     *
     * @param orderId 订单Id
     * @param operationNote 备注内容
     * @param type          类型
     * @return 是否成功
     */
    public Boolean setOrderNote(Long orderId,String operationNote, Integer type){
        ShopOrder shopOrder = getShopOrderById(orderId);
        Map<String,String> extraMap = shopOrder.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            extraMap = Maps.newHashMap();
        }

        VegaNoteType vegaNoteType = VegaNoteType.from(type);
        switch (vegaNoteType) {
            case OPERATION_ORDER_NOTE:
                extraMap.put(SystemConstant.OPERATION_NOTE, operationNote);
                break;
            case SELLER_ORDER_NOTE:
                extraMap.put(SystemConstant.ORDER_SELLER_NOTE, operationNote);
                break;
            case ORDER_INVOICE_NOTE:
                extraMap.put(SystemConstant.ORDER_INVOICE_NOTE, operationNote);
                break;
        }

        Response<Boolean> updateRes = orderWriteService.updateOrderExtra(orderId,OrderLevel.SHOP,extraMap);
        if(!updateRes.isSuccess()){
            log.error("update shop order(id:{}) set extra:{} fail,error:{}",orderId,extraMap.toString(),updateRes.getError());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

}
