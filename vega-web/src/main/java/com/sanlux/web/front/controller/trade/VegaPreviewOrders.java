package com.sanlux.web.front.controller.trade;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.trade.dto.PreOrderInfo;
import com.sanlux.trade.dto.PreOrderSku;
import com.sanlux.trade.dto.PurchaseSkuOrderCriteria;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.service.PurchaseOrderReadService;
import com.sanlux.trade.service.PurchaseSkuOrderReadService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import com.sanlux.web.front.service.PurchaseOrderService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.SubmittedSku;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;


/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/17/16
 * Time: 5:05 PM
 */
@Slf4j
@RestController
@RequestMapping("/api/vega")
public class VegaPreviewOrders {


    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;

    @Autowired
    private PurchaseOrderService purchaseOrderService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @Autowired
    private ReceiveShopParser receiveShopParser;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;


    private final static TypeReference<List<SubmittedSku>> LIST_OF_SUBMITTED_SKU =
            new TypeReference<List<SubmittedSku>>() {
            };



    /**
     * 订单预览 订单信息
     *
     * @param purchaseOrderId  采购单id
     * @param receiverInfoId  收货地址id
     * @return  (接单人shopId 平台或一级经销商、订单原价)
     */
    @RequestMapping(value = "/order/info", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public PreOrderInfo previewOrderInfo(@RequestParam("purchaseOrderId") Long purchaseOrderId ,
                                        @RequestParam(value = "receiverInfoId",required = false) Long receiverInfoId){

        final ParanaUser buyer = UserUtil.getCurrentUser();
        PreOrderInfo preOrderInfo = new PreOrderInfo();

        //获取采购单信息 并验证是否存在
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderById(purchaseOrderId);

        //采购单下的所有商品
        List<PurchaseSkuOrder> purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderId(purchaseOrder.getId());

        //获取当前登录用户身份
        String roleName = getUserRoleName(buyer);
        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);

        //接单店铺id
        Long receiveShopId = getReceiveShopId(roleName,buyer,receiverInfoId,purchaseSkuOrders);
        //根据接单店铺id 和 买家身份来决定商品价格
        preOrderInfo.setReceiveShopId(receiveShopId);
        //计算价格
        Long totalFee =0L;
        for (PurchaseSkuOrder purchaseSkuOrder: purchaseSkuOrders){
            Sku sku = findSkuById(purchaseSkuOrder.getSkuId());
            //计算真实价格
            Response<Integer> skuPriceResp =
                    receiveShopParser.findSkuPrice(sku.getId(), receiveShopId, buyer.getId(), orderUserType);
            if (!skuPriceResp.isSuccess()) {
                log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, orderUserType:{}, cause:{}",
                        sku.getId(), receiveShopId, buyer.getId(), orderUserType, skuPriceResp.getError());
                throw new JsonResponseException(skuPriceResp.getError());
            }
            totalFee+=Long.valueOf(skuPriceResp.getResult())*purchaseSkuOrder.getQuantity();
        }
        preOrderInfo.setTotalFee(totalFee);

        return preOrderInfo;

    }


    /**
     * 订单预览 商品分页信息
     *
     * @param purchaseOrderId  采购单id
     * @param receiverInfoId  收货地址id
     * @param pageNo  页码
     * @param size  页面大小
     * @return  商品分页信息
     */
    @RequestMapping(value = "/mobile/order/sku/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<PreOrderSku> previewSkuPaging(@RequestParam("purchaseOrderId") Long purchaseOrderId ,
                                        @RequestParam(value = "receiverInfoId",required = false) Long receiverInfoId,
                                       @RequestParam(value = "pageNo",required = false) Integer pageNo,
                                       @RequestParam(value = "size",required = false) Integer size) {

        List<PreOrderSku> preOrderSkus = Lists.newArrayList();
        final ParanaUser buyer = UserUtil.getCurrentUser();

        //获取当前登录用户身份
        String roleName = getUserRoleName(buyer);

        List<Shop> shops = getShopsByPurchaseOrderId(purchaseOrderId);
        for (Shop shop : shops) {
            PreOrderSku preOrderSku = new PreOrderSku();
            preOrderSku.setRoleName(roleName);
            //preOrderSku.setShops(shops);
            preOrderSku.setCurrentShopName(shop.getName());
            preOrderSku.setCurrentShopId(shop.getId());
            preOrderSkus.add(assemblySkuInfo(purchaseOrderId,receiverInfoId,shop.getId(),pageNo,size,buyer,roleName,preOrderSku));
        }

        return preOrderSkus;
    }


    /**
     * 订单预览 商品分页信息
     *
     * @param purchaseOrderId  采购单id
     * @param receiverInfoId  收货地址id
     * @param shopId  店铺id for 一级经销商下单需拆单,根据不同的店铺id查询各个店铺下的商品
     * @param pageNo  页码
     * @param size  页面大小
     * @return  商品分页信息
     */
    @RequestMapping(value = "/order/sku/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public PreOrderSku mobilePreviewSkuPaging(@RequestParam("purchaseOrderId") Long purchaseOrderId ,
                                        @RequestParam(value = "receiverInfoId",required = false) Long receiverInfoId,
                                        @RequestParam(value = "shopId",required = false) Long shopId,
                                        @RequestParam(value = "pageNo",required = false) Integer pageNo,
                                        @RequestParam(value = "size",required = false) Integer size) {

        final ParanaUser buyer = UserUtil.getCurrentUser();
        PreOrderSku preOrderSku = new PreOrderSku();

        //获取当前登录用户身份
        String roleName = getUserRoleName(buyer);
        preOrderSku.setRoleName(roleName);

        //如果是一级经销商下单则需要拆单 二级和普通用户不需要拆单 平台接单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            List<Shop> shops = getShopsByPurchaseOrderId(purchaseOrderId);
            preOrderSku.setShops(shops);
            if(Arguments.isNull(shopId)){

                shopId = shops.get(0).getId();//默认第一次进入预览页第一个订单tab取第一家店铺的
            }
        }
        return assemblySkuInfo(purchaseOrderId,receiverInfoId,shopId,pageNo,size,buyer,roleName,preOrderSku);
    }


    public PreOrderSku assemblySkuInfo(Long purchaseOrderId ,
                                       Long receiverInfoId,
                                       Long shopId,
                                       Integer pageNo,
                                       Integer size,
                                       ParanaUser buyer,
                                       String roleName,
                                       PreOrderSku preOrderSku) {
        //获取采购单信息 并验证是否存在
        PurchaseOrder purchaseOrder = purchaseOrderService.getPurchaseOrderById(purchaseOrderId);

        //采购单下的所有商品
        List<PurchaseSkuOrder> purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderId(purchaseOrder.getId());



        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);


        //接单人店铺id
        Long receiveShopId = getReceiveShopId(roleName,buyer,receiverInfoId,purchaseSkuOrders);
        //当前订单商品金额
        Long originFee = countOrderOirginFee(purchaseOrderId, shopId, receiveShopId, orderUserType,buyer);
        preOrderSku.setOriginFee(originFee);

        //封装查询条件
        PurchaseSkuOrderCriteria criteria = new PurchaseSkuOrderCriteria();
        criteria.setPurchaseId(purchaseOrderId);
        criteria.setBuyerId(buyer.getId());
        criteria.setShopId(shopId);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(size);
        criteria.setStatus(1);
        Response<Paging<PurchaseSkuOrder>> pagingResponse = purchaseSkuOrderReadService.paging(criteria);
        if(!pagingResponse.isSuccess()){
            log.error("paging purchase sku order by criteria:{} fail,error:{}",criteria,pagingResponse.getError());
            throw new JsonResponseException(pagingResponse.getError());
        }
        Paging<RichSku> richSkuPaging = transPurchaseOrder(pagingResponse.getResult(),buyer,orderUserType,receiverInfoId,receiveShopId);
        preOrderSku.setPaging(richSkuPaging);

        return preOrderSku;

        //orderRuleEngine.canPreView(richOrder); todo


    }










    private List<Shop> getShopsByPurchaseOrderId(Long purchaseOrderId){


        Response<List<Long>> shopIdsRes = purchaseSkuOrderReadService.findShopIdsByByPurchaseOrderId(purchaseOrderId);
        if(!shopIdsRes.isSuccess()){
            log.error("find shop ids by purchase order id:{} fail,error:{}",purchaseOrderId,shopIdsRes.getError());
            throw new JsonResponseException(shopIdsRes.getError());
        }

        if(CollectionUtils.isEmpty(shopIdsRes.getResult())){
            log.error("find shop ids by purchase order id:{} but not fount any valid",purchaseOrderId);
            throw new JsonResponseException("not.valid.purchase.sku.order");
        }

        Response<List<Shop>> shopsRes = shopReadService.findByIds(shopIdsRes.getResult());
        if(!shopIdsRes.isSuccess()){
            log.error("find shop by ids:{} fail,error:{}",shopIdsRes.getResult(),shopsRes.getError());
            throw new JsonResponseException(shopsRes.getError());
        }

        return shopsRes.getResult();
    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
    }

    private Shop findShopById(Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            log.error("failed to find shop(id={}), error code:{}", shopId, rShop.getError());
            throw new JsonResponseException(rShop.getError());
        }
        return rShop.getResult();
    }

    private Item findItemById(Long itemId) {
        Response<Item> rItem = itemReadService.findById(itemId);
        if (!rItem.isSuccess()) {
            log.error("failed to find item(id={}), error code:{}", itemId, rItem.getError());
            throw new JsonResponseException(rItem.getError());
        }
        return rItem.getResult();
    }


    /**
     * 封装商品信息
     * @param paging 采购清单商品
     * @param user 当前登录用户
     *                 商品价格:
     *                 (一级经销商 平台发货 供货价) (二级经销商 一级发货 在一级销售范围内折扣价，不在供货价)
     *                 (普通用户 平台或一级发货 平台发则零售价，一级经销商发则取最优折扣 )
     *                 运费:
     *                 (一级经销商 取供应商运费模板,取不到则为0)(二级经销商 取一级设置的运费模板,取不到商品则为0,取到的就按设置的运费模板计算)
     *                 (普通用户 如果平台接单则运费全部为0,如果一级接单则取一级设置的运费模板,取不到商品则为0,取到的就按设置的运费模板计算)
     *
     * @param receiverInfoId 收货地址
     * @return 分页商品信息
     */
    private Paging<RichSku> transPurchaseOrder(Paging<PurchaseSkuOrder> paging,ParanaUser user,OrderUserType orderUserType,Long receiverInfoId,Long receiveShopId){

        //todo 处理收货地址为空的情况 如果没有收货地址则商品按买家身份 显示对应的价格
        if(Arguments.notNull(receiverInfoId)){
            ReceiverInfo receiverInfo = getReceiverInfoById(receiverInfoId);
        }

        Paging<RichSku> richSkuPaging = new Paging<RichSku>();
        richSkuPaging.setTotal(paging.getTotal());
        List<RichSku> richSkus = Lists.newArrayList();
        for (PurchaseSkuOrder skuOrder : paging.getData()){

            Sku sku = findSkuById(skuOrder.getSkuId());

            Item item = findItemById(sku.getItemId());

            RichSku richSku = new RichSku();
            richSku.setItem(item);
            //计算真实价格
            Integer skuPrice =getRealSkuPrice(sku.getId(),receiveShopId, user.getId(), orderUserType);
            sku.setPrice(skuPrice);
            richSku.setSku(sku);
            richSku.setFee(Long.valueOf(skuPrice)*skuOrder.getQuantity());
            richSku.setQuantity(skuOrder.getQuantity());
            richSkus.add(richSku);
        }
        richSkuPaging.setData(richSkus);

        return richSkuPaging;
    }

    private Integer getRealSkuPrice(Long skuId, Long receiveShopId, Long userId,
                                    OrderUserType orderUserType){
        Response<Integer> skuPriceResp =
                receiveShopParser.findSkuPrice(skuId,receiveShopId, userId, orderUserType);
        if (!skuPriceResp.isSuccess()){
            log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                    skuId, receiveShopId, userId, skuPriceResp.getError());
            throw new JsonResponseException(skuPriceResp.getError());
        }

        return skuPriceResp.getResult();
    }


    private Long getReceiveShopId(String roleName,ParanaUser buyer,Long receiverInfoId,List<PurchaseSkuOrder> purchaseSkuOrders){



        //一级下单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            return 0L;
        }
        //二级下单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_SECOND.name())){
            Response<Long> shopPidRes = receiveShopParser.getShopPidForDealerSecond(buyer.getId());
            if(!shopPidRes.isSuccess()){
                log.error("get shop pid by dealer second user id:{}",buyer.getId());
                throw new JsonResponseException(shopPidRes.getError());
            }
            return shopPidRes.getResult();
        }

        //普通用户
        //收货地址不为空,则去匹配经销商
        if(Arguments.notNull(receiverInfoId)){
            ReceiverInfo receiverInfo = getReceiverInfoById(receiverInfoId);
            if(StringUtils.equals(roleName, UserRole.BUYER.name())){
                Response<Long> shoIdRes = receiveShopParser.getShopIdForBuyer(receiverInfo, purchaseSkuOrders, buyer);
                if(!shoIdRes.isSuccess()){
                    log.error("get shop id for buyer where receive info id:{} error:{}",receiverInfo.getId(),shoIdRes.getError());
                    throw new JsonResponseException(shoIdRes.getError());
                }
                return shoIdRes.getResult();
            }
        }else {
            return 0L;//没有收货地址则默认平台接单即商品按零售价展示
        }
        throw new JsonResponseException("not.matching.receive.shop");
    }

    private ReceiverInfo getReceiverInfoById(Long receiverInfoId){

        Response<ReceiverInfo> receiverInfoResp = receiverInfoReadService.findById(receiverInfoId);
        if (!receiverInfoResp.isSuccess()) {
            log.error("fail to find receiverInfo by id:{},cause:{}", receiverInfoId, receiverInfoResp.getError());
            throw new JsonResponseException(receiverInfoResp.getError());
        }

        return receiverInfoResp.getResult();
    }

    /**
     * 计算单个订单的商品金额
     * @param purchaseOrderId 采购单id
     * @param shopId 店铺id(一级经销商下单时需要拆单,所以根据该id获取到对应的商品) 如果为空则说明非一级经销商下单
     * @param receiveShopId 接单店铺id (平台店铺id为0)
     * @return 订单金额
     */
    private Long countOrderOirginFee(Long purchaseOrderId,Long shopId,Long receiveShopId,OrderUserType orderUserType,ParanaUser user){

        List<PurchaseSkuOrder> purchaseSkuOrders;
        if(Arguments.notNull(shopId)){
            purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderAndShopId(purchaseOrderId, shopId);
        }else {
            purchaseSkuOrders = purchaseOrderService.getPurchaseSkuOrdersByPurchaseOrderId(purchaseOrderId);
        }

        if(CollectionUtils.isEmpty(purchaseSkuOrders)){
            log.error("not valid sku to create order where purchase order id:{},shop id:{} receive shop id:{} orderUserType:{} ",purchaseOrderId,shopId,receiveShopId,orderUserType.toString());
            throw new JsonResponseException("not.valid.purchase.sku.order");

        }

        Long originFee =0L;

        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders){
            Sku sku = findSkuById(purchaseSkuOrder.getSkuId());
              //计算真实价格
            Integer skuPrice =getRealSkuPrice(sku.getId(),receiveShopId, user.getId(), orderUserType);
            originFee+=Long.valueOf(skuPrice)*purchaseSkuOrder.getQuantity();
        }

        return originFee;

    }
}
