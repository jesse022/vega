/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.controller.trade;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.enums.VegaShopType;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.ShopSkuReadService;
import com.sanlux.trade.dto.*;
import com.sanlux.trade.model.PurchaseOrder;
import com.sanlux.trade.model.PurchaseSkuOrder;
import com.sanlux.trade.service.PurchaseOrderReadService;
import com.sanlux.trade.service.PurchaseOrderWriteService;
import com.sanlux.trade.service.PurchaseSkuOrderReadService;
import com.sanlux.trade.service.PurchaseSkuOrderWriteService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ItemCacher;
import io.terminus.parana.cart.service.CartReadService;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;

/**
 * @author : songrenfei
 */
@Slf4j
@RestController
@RequestMapping("/api/purchase")
public class PurchaseOrders {

    @RpcConsumer
    private PurchaseOrderReadService purchaseOrderReadService;

    @RpcConsumer
    private PurchaseSkuOrderReadService purchaseSkuOrderReadService;

    @RpcConsumer
    private PurchaseOrderWriteService purchaseOrderWriteService;

    @RpcConsumer
    private PurchaseSkuOrderWriteService purchaseSkuOrderWriteService;

    @RpcConsumer
    private CartReadService cartReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @Autowired
    private ItemCacher itemCacher;

    @Autowired
    private ReceiveShopParser receiveShopParser;


    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();

    /**
     * 获取当前登录用户采购单列表
     */
    @RequestMapping(value = "/order", method = RequestMethod.GET)
    public List<PurchaseOrder> getPurchaseOrders() {
        ParanaUser vegaLoginUser = UserUtil.getCurrentUser();
        Response<List<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdNotTemp(vegaLoginUser.getId(), 0);
        if(!response.isSuccess()){
            log.error("find purchase order by buyer id:{} fail,error:{}",vegaLoginUser.getId(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 判断当前用户是否已有同名的采购单
     * @return true 存在 false 不存在
     */
    @RequestMapping(value = "/name", method = RequestMethod.GET)
    public Boolean checkPurchaseIsExist(@RequestParam String name) {
        ParanaUser vegaLoginUser = UserUtil.getCurrentUser();
        Response<Optional<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdAndName(vegaLoginUser.getId(),name);
        if(!response.isSuccess()){
            log.error("find purchase order by buyer id:{} and purchase name: {} fail,error:{}",vegaLoginUser.getId(),name,response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult().isPresent();

    }




    /**
     * 创建采购单
     * @param name 采购单名称
     * @return 采购单id
     */
    @RequestMapping(value = "/order", method = RequestMethod.POST)
    public Long createPurchaseOrder(@RequestParam String name) {

        if(Strings.isNullOrEmpty(name)){
            log.error("create purchase order fail name is null");
            throw new JsonResponseException("purchase.order.name.is.null");
        }

        //封装基础信息
        PurchaseOrder purchaseOrder = new PurchaseOrder();
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        purchaseOrder.setBuyerId(paranaUser.getId());
        purchaseOrder.setBuyerName(paranaUser.getName());
        purchaseOrder.setSkuQuantity(0);//初始化为0
        purchaseOrder.setIsTemp(Boolean.FALSE);//非临时采购单
        purchaseOrder.setName(name);
        purchaseOrder.setCreatedAt(new Date());
        purchaseOrder.setUpdatedAt(new Date());

        Response<Long> response = purchaseOrderWriteService.createPurchaseOrder(purchaseOrder);
        if(!response.isSuccess()){
            log.error("create purchase order :{} fail,error:{}",purchaseOrder,response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();
    }

    /**
     * 修改采购单
     * @param id 采购单id
     * @param name 采购名称
     * @return 是否修改成功
     */
    @RequestMapping(value = "/order/{id}", method = RequestMethod.PUT)
    public Boolean updatePurchaseOrder(@PathVariable(value = "id") Long id,@RequestParam String name) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        //检测当前采购单是否存在
        getPurchaseOrderById(id);

        PurchaseOrder purchaseOrder = new PurchaseOrder();
        purchaseOrder.setId(id);
        purchaseOrder.setName(name);
        if(Strings.isNullOrEmpty(purchaseOrder.getName())){
            log.error("update purchase order fail name is null");
            throw new JsonResponseException("purchase.order.name.is.null");
        }

        Response<Optional<PurchaseOrder>> existRes = purchaseOrderReadService.findByBuyerIdAndName(paranaUser.getId(),purchaseOrder.getName());
        if(!existRes.isSuccess()){
            log.error("find purchase order by user id:{} name:{} fail,error:{}",paranaUser.getId(),name,existRes.getError());
            throw new JsonResponseException(existRes.getError());
        }
        if(existRes.getResult().isPresent()){
            throw new JsonResponseException("purchase.name.exist");
        }

        Response<Boolean> response = purchaseOrderWriteService.updatePurchaseOrder(purchaseOrder);
        if(!response.isSuccess()){
            log.error("update purchase order :{} fail,error:{}",purchaseOrder,response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();
    }

    /**
     * 删除采购单
     * @param id 采购单id
     * @return 是否删除成功
     */
    @RequestMapping(value = "/order/{id}", method = RequestMethod.DELETE)
    public Boolean deletePurchaseOrder(@PathVariable(value = "id") Long id) {

        //检测是否存在
        getPurchaseOrderById(id);
        Response<Boolean> response = purchaseOrderWriteService.deletePurchaseOrderById(id);
        if(!response.isSuccess()){
            log.error("delete purchase order by id:{} fail,error:{}",id,response.getError());
            throw new JsonResponseException(response.getError());
        }

        return response.getResult();
    }






    /**
     * 查询用户采购单
     * @param criteria 查询条件
     * @return 采购单信息
     */
    @RequestMapping(value = "/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<PurchaseOrder> pagingPurchaseOrder(PurchaseOrderCriteria criteria) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        criteria.setBuyerId(paranaUser.getId());
        criteria.setIsTemp(Boolean.FALSE);
        criteria.setSkuQuantity(0); // 剔除友云采专属采购单

        Response<Paging<PurchaseOrder>> pagingResponse = purchaseOrderReadService.paging(criteria);
        if(!pagingResponse.isSuccess()){
            log.error("paging purchase order by criteria:{},error:{}",criteria,pagingResponse.getError());
        }
        return pagingResponse.getResult();
    }

    /**
     * 采购商品清单单分页
     * @param criteria 查询条件
     * @return 采购单信息
     */
    @RequestMapping(value = "/sku/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RichPurchaseSkuOrder> pagingPurchaseSkuOrder(PurchaseSkuOrderCriteria criteria) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        criteria.setBuyerId(paranaUser.getId());
        Response<Paging<PurchaseSkuOrder>> pagingResponse = purchaseSkuOrderReadService.paging(criteria);
        if(!pagingResponse.isSuccess()){
            log.error("find paging purchase sku order criteria:{} fail,error:{}",criteria,pagingResponse.getError());
            throw new JsonResponseException(pagingResponse.getError());
        }

        if(pagingResponse.getResult().getTotal()==0){
            return Paging.<RichPurchaseSkuOrder>empty();
        }

        //封装商品信息
        return getRichPurchaseSkuOrderPaging(pagingResponse.getResult());

    }


    /**
     * 采购商品清单单分页 for 购物车
     * @param criteria 查询条件
     * @return 采购单信息
     */
    @RequestMapping(value = "/order/cart/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RichPurchaseOrder pagingCartPurchaseOrder(PurchaseSkuOrderCriteria criteria) {
        return pagingCartPurchaseOrder(criteria, 0);
    }

    /**
     * 采购商品清单单分页 for 购物车 (获取友云采专属采购单)
     * @param criteria 查询条件
     * @return 采购单信息
     */
    @RequestMapping(value = "/order/cart/paging-youyuncai", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RichPurchaseOrder pagingCartPurchaseOrderForYouyuncai(PurchaseSkuOrderCriteria criteria) {
        return pagingCartPurchaseOrder(criteria, 1);
    }


    /**
     * 登录用户修改采购单商品的数量,若添加或者减少商品数量后,
     * 最终数量小于0,就将商品从采购商品清单中删除.
     *
     * @param purchaseOrderId       采购单id
     * @param skuId       商品id
     * @param quantity    商品数量,可以为负数
     * @return 最新采购单中商品的数量
     */
    @RequestMapping(value = "/change",method = RequestMethod.PUT)
    @ResponseBody
    public Integer changePurchaseSkuOrder(@RequestParam("purchaseOrderId") Long purchaseOrderId,@RequestParam("skuId") Long skuId,
                              @RequestParam(value = "quantity", defaultValue = "1", required = false) Integer quantity) {
        return changeCarPurchaseSkuOrder(purchaseOrderId, skuId, quantity);
    }

    /**
     * 友云采专属采购单添加商品接口
     * @param skuId  skuId
     * @param quantity 添加数量
     * @return 修改后数量
     */
    @RequestMapping(value = "/change-youyuncai",method = RequestMethod.PUT)
    @ResponseBody
    public Response<Integer> changePurchaseSkuOrderForYouyuncai(@RequestParam("skuId") Long skuId,
                                                      @RequestParam(value = "quantity", defaultValue = "1", required = false) Integer quantity) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        Response<List<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdNotTemp(paranaUser.getId(), 1);
        if(!response.isSuccess()){
            log.error("find you yun cai purchase order by buyer id:{} fail,error:{}",paranaUser.getId(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<PurchaseOrder> purchaseOrders = response.getResult();

        if(Arguments.isNullOrEmpty(purchaseOrders)){
            log.warn("current user: {} not exist you yun cai purchase order");
            return Response.fail("purchase.order.not.exist");
        }

        return Response.ok(changeCarPurchaseSkuOrder(purchaseOrders.get(0).getId(), skuId, quantity));
    }




    /**
     * 临时采购单 详情页立即采购
     * @param skuId       商品id
     * @param quantity    商品数量,可以为负数
     * @return 采购单id
     */
    @RequestMapping(value = "/change/temp",method = RequestMethod.PUT)
    @ResponseBody
    public Long changeTempPurchaseSkuOrder(@RequestParam("skuId") Long skuId,
                                          @RequestParam(value = "quantity", defaultValue = "1", required = false) Integer quantity) {
        // 获取商品详情
        Response<Sku> findSku = skuReadService.findSkuById(skuId);
        if (!findSku.isSuccess()) {
            log.error("when changing cart, fail to find sku(id={}) for user(id={}), cause:{}",
                    skuId, UserUtil.getUserId(), findSku.getError());
            throw new JsonResponseException(findSku.getError());
        }

        // 检查商品是否上架
        Sku sku = findSku.getResult();
        if (!Objects.equals(sku.getStatus(), 1)) {
            throw new JsonResponseException("item.not.available");
        }

        ParanaUser paranaUser = UserUtil.getCurrentUser();

        // 更改采购单
        Response<Long> tryChange = purchaseSkuOrderWriteService.changeTempPurchaseSkuOrder(sku, quantity, paranaUser.getId(), paranaUser.getName());
        if (!tryChange.isSuccess()) {


            log.error("fail to change cart by skuId={}, quantity={}, userId={}, error code:{}",
                    skuId, quantity, UserUtil.getUserId(), tryChange.getError());
            throw new JsonResponseException(tryChange.getError());
        }
        return tryChange.getResult();
    }


    /**
     * 登录用户批量修改采购单商品的数量
     * @param purchaseOrderId       采购单id
     * @param data   采购商品信息
     * @return 是否操作成功
     */
    @RequestMapping(value = "/batch-change",method = RequestMethod.PUT)
    @ResponseBody
    public Boolean batchChangePurchaseSkuOrder(@RequestParam("purchaseOrderId") Long purchaseOrderId,@RequestParam("data") String data) {

        List<PurchaseSkuOrder> purchaseSkuOrders = getPurchaseSkuOrderFromData(data);

        if(CollectionUtils.isEmpty(purchaseSkuOrders)){
            throw new JsonResponseException("item.info.available");
        }

        Map<Sku,Integer> skuAndQuantityMap = Maps.newHashMap();

        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders){
            // 获取商品详情
            Response<Sku> findSku = skuReadService.findSkuById(purchaseSkuOrder.getSkuId());
            if (!findSku.isSuccess()) {
                log.error("when changing cart, fail to find sku(id={}) for user(id={}), cause:{}",
                        purchaseSkuOrder.getSkuId(), UserUtil.getUserId(), findSku.getError());
                throw new JsonResponseException(findSku.getError());
            }

            // 检查商品是否上架
            Sku sku = findSku.getResult();
            if (!Objects.equals(sku.getStatus(), 1)) {
                throw new JsonResponseException("item.not.available");
            }

            skuAndQuantityMap.put(sku,purchaseSkuOrder.getQuantity());
        }

        ParanaUser paranaUser = UserUtil.getCurrentUser();

        // 更改采购单
        Response<Boolean> tryChange = purchaseSkuOrderWriteService.batchChangePurchaseSkuOrder(skuAndQuantityMap, purchaseOrderId, paranaUser.getId(), paranaUser.getName());
        if (!tryChange.isSuccess()) {
            log.error("fail to change cart by skuAndQuantityMap:{}, userId={}, error code:{}",
                    skuAndQuantityMap, UserUtil.getUserId(), tryChange.getError());
            throw new JsonResponseException(tryChange.getError());
        }
        return tryChange.getResult();
    }




    @RequestMapping(value = "/batchDelete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public void batchDelete(@RequestParam("skuIds") String skuData,@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        List<String> stringSkus = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(skuData);
        List<Long> skuIds = new ArrayList<Long>();
        for(String skuId : stringSkus) {
            skuIds.add(Long.valueOf(skuId));
        }
        if(skuIds.isEmpty()) {
            return;
        }
        Response<Boolean> deleteR = purchaseSkuOrderWriteService.batchDeleteByPurchaseOrderIdAndySkuId(skuIds, purchaseOrderId);
        if(!deleteR.isSuccess()) {
            log.error("fail to batch delete purchase sku order by skuIds={},purchaseOrderId={},error code={}",
                    skuIds, purchaseOrderId, deleteR.getError());
            throw new JsonResponseException(deleteR.getError());
        }
    }


    /**
     * 从采购单中过滤掉失效的商品 保证下单预览页展示的商品都是有效的
     */
    @RequestMapping(value = "/filter", method = RequestMethod.PUT)
    public void filterInvalidSku(@RequestParam("purchaseOrderId") Long purchaseOrderId) {
        //检测当前采购单是否存在
        getPurchaseOrderById(purchaseOrderId);
        //已选中的商品
        List<PurchaseSkuOrder> purchaseSkuOrders =  getPurchaseSkuOrdersByPurchaseOrderById(purchaseOrderId);

        if(CollectionUtils.isEmpty(purchaseSkuOrders)){
            log.error("not find purchase sku order by purchase order id:{}",purchaseOrderId);
            throw new JsonResponseException("not.valid.purchase.sku.order");
        }
        List<Long> invalidIds = Lists.newArrayList();
        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders){
            Sku sku = findSkuById(purchaseSkuOrder.getSkuId());
            if(!sku.getStatus().equals(1)){
                invalidIds.add(purchaseSkuOrder.getId());
            }
        }

        //把失效的置为未勾选
        Response<Boolean> updateRes = purchaseSkuOrderWriteService.batchUpdateStatus(invalidIds, 0);
        if(!updateRes.isSuccess()){
            log.error("batch update purchase sku order status where ids:{} status:{} fail,error:{}",invalidIds,0,updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

        //如果没有一个勾选的 则报错
        if(Objects.equals(invalidIds.size(),purchaseSkuOrders.size())){
            throw new JsonResponseException("not.valid.purchase.sku.order");
        }

    }


    private List<PurchaseSkuOrder> getPurchaseSkuOrdersByPurchaseOrderById(Long purchaseOrderId){

        //获取选中的商品
        Response<List<PurchaseSkuOrder>> skuRes = purchaseSkuOrderReadService.finByPurchaseOrderIdAndStatus(purchaseOrderId, 1);
        if(!skuRes.isSuccess()){
            log.error("find purchase sku order by purchase order id:{} status:{} fail,error:{}",purchaseOrderId,1,skuRes.getError());
            throw new JsonResponseException(skuRes.getError());
        }

        return skuRes.getResult();

    }

    private Sku findSkuById(Long skuId) {
        Response<Sku> rSku = skuReadService.findSkuById(skuId);
        if (!rSku.isSuccess()) {
            log.error("failed to find sku(id={}), error code:{}", skuId, rSku.getError());
            throw new JsonResponseException(rSku.getError());
        }
        return rSku.getResult();
    }


    protected Paging<RichPurchaseSkuOrder> getRichPurchaseSkuOrderPaging(Paging<PurchaseSkuOrder> paging) {
        //获取当前用户信息
        final ParanaUser buyer = UserUtil.getCurrentUser();
        String roleName = getUserRoleName(buyer);
        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);

        //获取接单店铺id
        Long receiveShopId = getReceiveShopId(roleName, buyer);
        List<PurchaseSkuOrder> purchaseSkuOrders = paging.getData();
        // 拼装
        List<RichPurchaseSkuOrder> richPurchaseSkuOrders = new ArrayList<>();

        for (PurchaseSkuOrder purchaseSkuOrder : purchaseSkuOrders) {
            // 获取店铺详情,若查找失败,就跳过
            Response<Shop> findShop = shopReadService.findById(purchaseSkuOrder.getShopId());
            if (!findShop.isSuccess() || findShop.getResult() == null) {
                log.warn("fail to find shop by id={} when view purchase sku order id:{}, error code:{}, skip",
                        purchaseSkuOrder.getShopId(), purchaseSkuOrder.getId(), findShop.getError());
                continue;
            }
            Shop shop = findShop.getResult();

            // 获取sku,商品名,商品主图
            Response<Sku> findSku = skuReadService.findSkuById(purchaseSkuOrder.getSkuId());
            if (!findSku.isSuccess() || findSku.getResult() == null) {
                log.warn("fail to find sku by id={}, error code={}, skip",
                        purchaseSkuOrder.getSkuId(), findSku.getError());
                continue;
            }
            Sku sku = findSku.getResult();

            //取产品真实价格
            Integer skuPrice = getRealSkuPrice(purchaseSkuOrder.getSkuId(), receiveShopId, buyer.getId(), orderUserType);
            //产品原价(取平台店铺的销售价)
            Response<Optional<ShopSku>> findShopSku = shopSkuReadService.findByShopIdAndSkuId((long) VegaShopType.PLATFORM.value(), purchaseSkuOrder.getSkuId());
            if (!findShopSku.isSuccess()) {
                log.error("fail to find shop sku by shopId={},and skuId={},cause:{}",
                        VegaShopType.PLATFORM.value(), purchaseSkuOrder.getSkuId(), findShopSku.getError());
                continue;
            }
            try {
                sku.setExtraPrice(ImmutableMap.of("platformPrice", findShopSku.getResult().get().getPrice(), "originPrice", skuPrice));
            } catch (Exception e) {
                log.error("get sku Extra Price fail id:{} ,error:{}", purchaseSkuOrder.getSkuId(), e.getMessage());
                continue;
            }

            // 商品失效,就删除这条购物车
            Item item = itemCacher.findItemById(sku.getItemId());
            if (Objects.equals(item.getStatus(), -3)) {
                log.warn("item id={} already deleted, remove from cart", item.getId());
            }

            RichPurchaseSkuOrder richPurchaseSkuOrder = new RichPurchaseSkuOrder();
            richPurchaseSkuOrder.setSku(sku);
            richPurchaseSkuOrder.setPurchaseSkuOrder(purchaseSkuOrder);
            richPurchaseSkuOrder.setItemName(item.getName());
            richPurchaseSkuOrder.setItemStatus(item.getStatus());
            richPurchaseSkuOrder.setItemImage(item.getMainImage());
            richPurchaseSkuOrder.setShopName(shop.getName());

            richPurchaseSkuOrders.add(richPurchaseSkuOrder);
        }
        Paging<RichPurchaseSkuOrder> purchaseSkuOrderPaging = new Paging<RichPurchaseSkuOrder>();
        purchaseSkuOrderPaging.setData(richPurchaseSkuOrders);
        purchaseSkuOrderPaging.setTotal(paging.getTotal());
        return purchaseSkuOrderPaging;
    }


    private PurchaseOrder getPurchaseOrderById(Long id){

        //检测是否存在
        Response<Optional<PurchaseOrder>> existRes = purchaseOrderReadService.findPurchaseOrderById(id);
        if(!existRes.isSuccess()){
            log.error("find purchase order by id:{} fail,error:{}",id,existRes.getError());
            throw new JsonResponseException("purchase.order.name.is.null");
        }

        if(!existRes.getResult().isPresent()){
            log.error("not find purchase order by id:{}",id);
            throw new JsonResponseException("purchase.order.not.exist");
        }
        return existRes.getResult().get();
    }


    private List<PurchaseSkuOrder> getPurchaseSkuOrderFromData(String data) {
        try {
            return JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(List.class, PurchaseSkuOrder.class));
        }catch (Exception e) {
            log.error("fail to get purchaseSkuOrder from data={},cause:{}",data, Throwables.getStackTraceAsString(e));
        }
        return null;
    }

    /**
     * 获取接单店铺ID
     * add by lujm on 2017/2/16
     * @param roleName 买家角色
     * @param buyer 买家信息
     * @return 店铺ID
     */
    private Long getReceiveShopId(String roleName,ParanaUser buyer){
        //一级下单
        if(StringUtils.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            return 0L;
        }
        //二级下单
        if(StringUtils.equals(roleName,VegaUserRole.DEALER_SECOND.name())){
            Response<Long> longResponse = receiveShopParser.getShopPidForDealerSecond(buyer.getId());
            if(longResponse.isSuccess()){
                return longResponse.getResult();
            }else{
                log.error("get shop pid by dealer second user id:{}",buyer.getId());
                throw new JsonResponseException(longResponse.getError());
            }
        }
        //普通用户下单
        if(StringUtils.equals(roleName, UserRole.BUYER.name())){
            Response<Long> shoIdRes = receiveShopParser.getShopIdByrBuyer(buyer);
            if(!shoIdRes.isSuccess()){
                log.error("get shop id for buyer where user info id:{} error:{}",buyer.getId(),shoIdRes.getError());
                throw new JsonResponseException(shoIdRes.getError());
            }
            return shoIdRes.getResult();
        }
        throw new JsonResponseException("not.matching.receive.shop");
    }

    /**
     * 获取商品真实价格
     * add by lujm on 2017/2/16
     * @param skuId skuId
     * @param receiveShopId 接单店铺
     * @param userId 用户ID
     * @param orderUserType 用户类型
     * @return 价格
     */
    private Integer getRealSkuPrice(Long skuId, Long receiveShopId, Long userId, OrderUserType orderUserType){
        Response<Integer> skuPriceResp =
                receiveShopParser.findSkuPrice(skuId,receiveShopId, userId, orderUserType);
        if (!skuPriceResp.isSuccess()){
            log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                    skuId, receiveShopId, userId, skuPriceResp.getError());
            throw new JsonResponseException(skuPriceResp.getError());
        }
        return skuPriceResp.getResult();
    }

    /**
     * 采购商品清单单分页 for 购物车 功能封装
     * @param criteria 查询条件
     * @param purchaseTags 采购单标识 1:友云采专属采购单 0:默认
     * @return 采购单信息
     */
    private RichPurchaseOrder pagingCartPurchaseOrder(PurchaseSkuOrderCriteria criteria, Integer purchaseTags) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        RichPurchaseOrder richPurchaseOrder = new RichPurchaseOrder();

        //当前用户所有采购单
        Response<List<PurchaseOrder>> response = purchaseOrderReadService.findByBuyerIdNotTemp(paranaUser.getId(), purchaseTags);
        if(!response.isSuccess()){
            log.error("find purchase order by buyer id:{} fail,error:{}",paranaUser.getId(),response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<PurchaseOrder> purchaseOrders = response.getResult();

        if(Arguments.isNullOrEmpty(purchaseOrders)){
            log.warn("current user: {} not exist purchase order");
            return richPurchaseOrder;
        }

        if(Arguments.isNull(criteria.getPurchaseId())){
            criteria.setPurchaseId(purchaseOrders.get(0).getId());
        }
        richPurchaseOrder.setPurchaseOrders(purchaseOrders);
        richPurchaseOrder.setCurrentPurchaseOrderId(criteria.getPurchaseId());

        PurchaseOrder purchaseOrder =  getPurchaseOrderById(richPurchaseOrder.getCurrentPurchaseOrderId());
        richPurchaseOrder.setCurrentPurchaseOrderName(purchaseOrder.getName());
        criteria.setBuyerId(paranaUser.getId());
        Response<Paging<PurchaseSkuOrder>> pagingResponse = purchaseSkuOrderReadService.paging(criteria);
        if(!pagingResponse.isSuccess()){
            log.error("find paging purchase sku order criteria:{} fail,error:{}",criteria,pagingResponse.getError());
            throw new JsonResponseException(pagingResponse.getError());
        }

        if(pagingResponse.getResult().getTotal()==0){
            log.warn("current purchase order: (id:{}) not exist sku",criteria.getPurchaseId());
            return richPurchaseOrder;
        }

        richPurchaseOrder.setPaging(getRichPurchaseSkuOrderPaging(pagingResponse.getResult()));

        return richPurchaseOrder;

    }

    /**
     * 添加单个商品到采购单功能封装
     * 最终数量小于0,就将商品从采购商品清单中删除.
     *
     * @param purchaseOrderId       采购单id
     * @param skuId       商品id
     * @param quantity    商品数量,可以为负数
     * @return 最新采购单中商品的数量
     */
    private Integer changeCarPurchaseSkuOrder(Long purchaseOrderId,Long skuId, Integer quantity) {
        // 获取商品详情
        Response<Sku> findSku = skuReadService.findSkuById(skuId);
        if (!findSku.isSuccess()) {
            log.error("when changing cart, fail to find sku(id={}) for user(id={}), cause:{}",
                    skuId, UserUtil.getUserId(), findSku.getError());
            throw new JsonResponseException(findSku.getError());
        }

        // 检查商品是否上架
        Sku sku = findSku.getResult();
        if (!Objects.equals(sku.getStatus(), 1)) {
            throw new JsonResponseException("item.not.available");
        }

        /*if (MoreObjects.firstNonNull(sku.getStockQuantity(), 0) - quantity < 0) {
            throw new JsonResponseException("stock.empty");
        }*/
        ParanaUser paranaUser = UserUtil.getCurrentUser();

        // 更改采购单
        Response<Integer> tryChange = purchaseSkuOrderWriteService.changePurchaseSkuOrder(sku, quantity,purchaseOrderId,paranaUser.getId(),paranaUser.getName());
        if (!tryChange.isSuccess()) {
            log.error("fail to change cart by skuId={}, quantity={}, userId={}, error code:{}",
                    skuId, quantity, UserUtil.getUserId(), tryChange.getError());
            throw new JsonResponseException(tryChange.getError());
        }
        return tryChange.getResult();
    }


}
