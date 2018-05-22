package com.sanlux.web.front.controller.trade;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.sanlux.common.constants.SystemConstant;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.dto.VegaOrderDetail;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.web.front.core.trade.VegaOrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@RestController
@Slf4j
@RequestMapping(value = "/api/vega/order")
public class VegaOrderReader {

    @Autowired
    private VegaOrderReadLogic orderReadLogic;

    @Autowired
    private FlowPicker flowPicker;

    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;



    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<OrderGroup>> findBy(@RequestParam Map<String, String> orderCriteria ,
                                               @RequestParam(value = "supplierIsBuyer",defaultValue = "0",required = false) String supplierIsBuyer) {
        Response<Paging<OrderGroup>> response = orderReadLogic.pagingOrder(orderCriteria);
        if(response.isSuccess()){
            ParanaUser user = UserUtil.getCurrentUser();
            OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);

            if( !Objects.equals(supplierIsBuyer,"1")){
                if (userType.equals(OrderUserType.SUPPLIER)) {
                    //供应商作为卖家登录 要展示供货价,订单相关金额要替换以供货价为基础的金额
                    for (OrderGroup orderGroup : response.getResult().getData()) {
                        replaceOrderPrice(orderGroup);
                    }
                } else {
                    // 一级,二级经销商作为卖家身份登录
                    for (OrderGroup orderGroup : response.getResult().getData()) {
                        setOrderSellerPrice(orderGroup, userType);
                    }
                }
            }
        }

        return response;
    }

    /**
     * 一级经销商查询所属二级经销商订单
     *
     * @param orderCriteria 查询条件
     * @return 分页信息
     */
    @RequestMapping(value = "/paging-second-shop-order", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<OrderGroup>> pagingBySecondShopOrder(@RequestParam Map<String, String> orderCriteria) {
        Long userId = UserUtil.getUserId();
        List<Long> ShopIds = Lists.newArrayList();
        Response<Shop> shopResponse = shopReadService.findByUserId(userId);
        if (!shopResponse.isSuccess()) {
            log.error("shopId find by userId fail ,userId {}, case {}", userId, shopResponse.getError());
            return Response.ok(Paging.empty(OrderGroup.class));
        } else {
            if (orderCriteria != null && !Strings.isNullOrEmpty(orderCriteria.get("shopName"))) {
                // 根据当前用户店铺ID,下级店铺名称查询下级经销商信息
                Response<List<VegaShopExtra>> resp = vegaShopReadService.findShopByPidAndName(shopResponse.getResult().getId(), orderCriteria.get("shopName"));
                if (!resp.isSuccess()) {
                    log.error("failed to find vega shop extra by name like ({}), cause : {}", orderCriteria.get("shopName"), resp.getError());
                }else {
                    ShopIds = Lists.transform(resp.getResult(), new Function<VegaShopExtra, Long>() {
                        @Override
                        public Long apply(VegaShopExtra vegaShopExtra) {
                            return vegaShopExtra.getShopId();
                        }
                    });
                }
                if (Arguments.isNullOrEmpty(ShopIds)) {
                    return Response.ok(Paging.empty());
                }
                orderCriteria.remove("shopName");//清空店铺名称查询条件
            }
        }
        if(orderCriteria != null) {
            String pagingSecondShopOrder = Arguments.isNullOrEmpty(ShopIds) ? getShopIdByShopPid(shopResponse.getResult().getId()) : Joiners.COMMA.join(ShopIds);
            if(Strings.isNullOrEmpty(pagingSecondShopOrder)){
                return Response.ok(Paging.empty());
            }
            orderCriteria.put("pagingSecondShopOrder", pagingSecondShopOrder);//塞入ShopIds
        }
        Response<Paging<OrderGroup>> response = orderReadLogic.pagingOrder(orderCriteria);
        return response;
    }


    @RequestMapping(value = "/buyer/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Response<Paging<OrderGroup>> findForBuyer(@RequestParam Map<String, String> orderCriteria) {
        ParanaUser user = UserUtil.getCurrentUser();
        orderCriteria.put("buyerId", user.getId().toString());
        orderCriteria.put("supplierIsBuyer", "1");//塞入买家标识
        Response<Paging<OrderGroup>> response = orderReadLogic.pagingOrder(orderCriteria);
        return response;
    }




    @RequestMapping(value = "/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public VegaOrderDetail detail(@RequestParam("id") Long shopOrderId,
                                  @RequestParam(value = "supplierIsBuyer",defaultValue = "0",required = false) String supplierIsBuyer,
                                  @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {

        ParanaUser user = UserUtil.getCurrentUser();
        OrderUserType userType = UserTypeHelper.getOrderUserTypeByUser(user);
        //ShopOrder shopOrder = getShopOrderById(shopOrderId);
        //checkSeeAuth(user,shopOrder);

        Response<VegaOrderDetail> resp = vegaOrderReadService.pagingVegaOrderDetailByShopId(shopOrderId, flowPicker, pageNo, pageSize);
        if (!resp.isSuccess()) {
            log.error("vega order detail fail, shopOrderId error:{}", shopOrderId, resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        //供应商作为卖家登录 要展示供货价,订单相关金额要替换以供货价为基础的金额
        if(userType.equals(OrderUserType.SUPPLIER) && !Objects.equals(supplierIsBuyer,"1")){
            replaceOrderPrice(resp.getResult());
        }

        return resp.getResult();

    }



    private ShopOrder getShopOrderById(Long shopOrderId) {
        Response<ShopOrder> shopOrderResp = shopOrderReadService.findById(shopOrderId);
        if (!shopOrderResp.isSuccess()) {
            log.error("fail to find shop order by id:{},cause:{}", shopOrderId, shopOrderResp.getError());
            throw new JsonResponseException(shopOrderResp.getError());
        }
        return shopOrderResp.getResult();
    }


    private void  checkSeeAuth(ParanaUser user,ShopOrder shopOrder){
        if(!com.google.common.base.Objects.equal(shopOrder.getBuyerId(), user.getId())&&!com.google.common.base.Objects.equal(shopOrder.getShopId(), user.getShopId())){
            log.error("buyerId and shopId mismatch, buyerId{}, shopId{} error:{}", shopOrder.getBuyerId(),
                    shopOrder.getShopId(), "buyerId and shopId mismatch");
            throw new JsonResponseException("buyer.shop.id.mismatch");
        }
    }




    private void replaceOrderPrice(VegaOrderDetail detail){

        //替换shopOrder的价格
        Long fee =0l;
        //替换当前页面skuOrder的价格
        for (VegaOrderDetail.SkuOrderAndOperation  skuOrderAndOperation :detail.getSkuOrderPaging().getData()){
            SkuOrder skuOrder = skuOrderAndOperation.getSkuOrder();
            Map<String,String> tags = skuOrder.getTags();
            String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
            if(!Strings.isNullOrEmpty(orderSkuSellerPrice)){
                skuOrder.setOriginFee(skuOrder.getQuantity()*Long.valueOf(orderSkuSellerPrice));
            }
            skuOrder.setFee(skuOrder.getOriginFee());//todo 暂不考虑优惠
            fee+=skuOrder.getFee();

        }
        ShopOrder shopOrder = detail.getShopOrder();
        shopOrder.setOriginFee(fee);
        fee+=shopOrder.getShipFee();
        shopOrder.setFee(fee);
    }


    /**
     * 根据订单替换为供货价信息
     * modify by lujm on 2017/04/13
     * 修改内容:解决供应商订单管理订单商品超出5条时,查看总价信息不对问题
     * @param orderGroup 订单信息
     */
    private void replaceOrderPrice(OrderGroup orderGroup) {
        ShopOrder shopOrder = orderGroup.getShopOrder();
        //替换shopOrder的价格
        Long fee = 0l;
        //替换当前页面skuOrder的价格
        for (OrderGroup.SkuOrderAndOperation skuOrderAndOperation : orderGroup.getSkuOrderAndOperations()) {
            SkuOrder skuOrder = skuOrderAndOperation.getSkuOrder();
            Map<String, String> tags = skuOrder.getTags();
            String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
            if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                skuOrder.setOriginFee(skuOrder.getQuantity() * Long.valueOf(orderSkuSellerPrice));
            }
            skuOrder.setFee(skuOrder.getOriginFee());//todo 暂不考虑优惠
            fee += skuOrder.getFee();
        }

        //订单商品超出5条时,获取订单总价需要遍历所有订单商品
        if (orderGroup.getSkuOrderAndOperations().size() == 5) {
            Response<VegaOrderDetail> resp = vegaOrderReadService.pagingVegaOrderDetailByShopId(orderGroup.getShopOrder().getId(), flowPicker, 1, 20);
            if (!resp.isSuccess()) {
                log.error("vega order detail fail, shopOrderId error:{}", orderGroup.getShopOrder().getId(), resp.getError());
                shopOrder.setOriginFee(fee);
                fee += shopOrder.getShipFee();
                shopOrder.setFee(fee);
            } else {
                VegaOrderDetail vegaOrderDetail = resp.getResult();
                Long feeNew = 0l;
                for (VegaOrderDetail.SkuOrderAndOperation skuOrderAndOperation : vegaOrderDetail.getSkuOrderPaging().getData()) {
                    SkuOrder skuOrderNew = skuOrderAndOperation.getSkuOrder();
                    Map<String, String> tags = skuOrderNew.getTags();
                    String orderSkuSellerPrice = tags.get(SystemConstant.ORDER_SKU_SELLER_PRICE);
                    if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                        skuOrderNew.setOriginFee(skuOrderNew.getQuantity() * Long.valueOf(orderSkuSellerPrice));
                    }
                    skuOrderNew.setFee(skuOrderNew.getOriginFee());//todo 暂不考虑优惠
                    feeNew += skuOrderNew.getFee();

                }
                shopOrder.setOriginFee(feeNew);
                feeNew += shopOrder.getShipFee();
                shopOrder.setFee(feeNew);
            }
        } else {
            shopOrder.setOriginFee(fee);
            fee += shopOrder.getShipFee();
            shopOrder.setFee(fee);
        }
    }

    /**
     * 塞入订单服务商成本价
     *
     * @param orderGroup orderGroup
     */
    private void setOrderSellerPrice(OrderGroup orderGroup, OrderUserType orderUserType) {
        ShopOrder shopOrder = orderGroup.getShopOrder();
        Map<String, String> shopOrderTags = shopOrder.getTags();
        Long orderSellerPrice = 0L;

        //塞入当前页面skuOrder的服务商成本价
        for (OrderGroup.SkuOrderAndOperation skuOrderAndOperation : orderGroup.getSkuOrderAndOperations()) {
            SkuOrder skuOrder = skuOrderAndOperation.getSkuOrder();
            Map<String, String> tags = skuOrder.getTags();
            String orderSkuSellerPrice = tags.get(Objects.equals(orderUserType, OrderUserType.DEALER_FIRST) ?
                    SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE : SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE);
            tags.remove(SystemConstant.ORDER_SKU_SELLER_PRICE); // 先清空供货价
            if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                tags.put(SystemConstant.ORDER_SKU_SELLER_PRICE, orderSkuSellerPrice);
            }
            skuOrder.setTags(tags);
        }

        // 塞入订单总的成本价
        Response<VegaOrderDetail> resp = vegaOrderReadService.findVegaOrderDetailByShopOrderId(orderGroup.getShopOrder().getId());
        if (!resp.isSuccess()) {
            log.error("vega order detail fail, shopOrderId error:{}", orderGroup.getShopOrder().getId(), resp.getError());
        } else {
            VegaOrderDetail vegaOrderDetail = resp.getResult();
            for (SkuOrder skuOrder : vegaOrderDetail.getSkuOrders()) {
                Map<String, String> tags = skuOrder.getTags();
                String orderSkuSellerPrice = tags.get(Objects.equals(orderUserType, OrderUserType.DEALER_FIRST) ?
                        SystemConstant.ORDER_SKU_FIRST_SELLER_PRICE : SystemConstant.ORDER_SKU_SECOND_SELLER_PRICE);
                if (!Strings.isNullOrEmpty(orderSkuSellerPrice)) {
                    orderSellerPrice += skuOrder.getQuantity() * Long.valueOf(orderSkuSellerPrice);
                }
            }
            shopOrderTags.put(SystemConstant.ORDER_SELLER_PRICE, orderSellerPrice.toString());
            shopOrder.setTags(shopOrderTags);
        }
    }

    /**
     * 根据上级店铺Id获取下级店铺Ids
     *
     * @param shopPid 上级店铺Id
     * @return 下级店铺Id
     */
    private String getShopIdByShopPid(Long shopPid) {
        List<Long> ShopIds = Lists.newArrayList();
        Response<List<VegaShopExtra>> vegaShopExtrasResp = vegaShopReadService.findVegaShopExtrasByShopPid(shopPid);
        if (!vegaShopExtrasResp.isSuccess()) {
            log.error("find children shop fail,shopPid:{}, cause:{}", shopPid, vegaShopExtrasResp.getError());
        } else {
            ShopIds = Lists.transform(vegaShopExtrasResp.getResult(), new Function<VegaShopExtra, Long>() {
                @Override
                public Long apply(VegaShopExtra vegaShopExtra) {
                    return vegaShopExtra.getShopId();
                }
            });
        }
        return Arguments.isNullOrEmpty(ShopIds) ? "" : Joiners.COMMA.join(ShopIds);
    }

}