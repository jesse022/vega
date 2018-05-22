package com.sanlux.trade.impl.manager;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.trade.impl.dao.OrderDispatchRelationDao;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.component.PayInfoDigestor;
import io.terminus.parana.order.dto.PersistedOrderInfos;
import io.terminus.parana.order.impl.dao.*;
import io.terminus.parana.order.impl.manager.OrderManager;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 8/24/16
 * Time: 4:59 PM
 */
@Slf4j
@Primary
@Component
public class VegaOrderManager extends OrderManager {

    private OrderDispatchRelationDao orderDispatchRelationDao;

    private final ShopOrderDao shopOrderDao;

    private final SkuOrderDao skuOrderDao;

    private final OrderReceiverInfoDao orderReceiverInfoDao;

    private final OrderInvoiceDao orderInvoiceDao;

    private final OrderPaymentDao orderPaymentDao;

    private final PaymentDao paymentDao;



    @Autowired
    public VegaOrderManager(ShopOrderDao shopOrderDao, SkuOrderDao skuOrderDao, OrderReceiverInfoDao orderReceiverInfoDao,
                            OrderInvoiceDao orderInvoiceDao, OrderDispatchRelationDao orderDispatchRelationDao, OrderPaymentDao orderPaymentDao, PaymentDao paymentDao) {

        super(shopOrderDao, skuOrderDao, orderReceiverInfoDao, orderInvoiceDao);
        this.shopOrderDao = shopOrderDao;
        this.skuOrderDao = skuOrderDao;
        this.orderReceiverInfoDao = orderReceiverInfoDao;
        this.orderInvoiceDao = orderInvoiceDao;
        this.orderDispatchRelationDao = orderDispatchRelationDao;
        this.orderPaymentDao = orderPaymentDao;
        this.paymentDao = paymentDao;
    }


    @Transactional
    public void updateOrderTags(Long orderId,Map<String,String> tags){
        shopOrderDao.updateTags(orderId,tags);
        skuOrderDao.updateTagsByOrderId(orderId,tags);
    }

    @Transactional
    public void changeShopOrderShipFee(ShopOrder update){
        //更新订单金额
        shopOrderDao.update(update);
        //设置订单对应的payment的channel追加_back字符串
        List<OrderPayment> orderPayments = orderPaymentDao.findByOrderIdAndOrderType(update.getId(),OrderLevel.SHOP.getValue());
        for (OrderPayment orderPayment : orderPayments){
            Payment payment = paymentDao.findById(orderPayment.getPaymentId());
            if(Arguments.isNull(payment)){
                log.error("not find payment by id:{}",orderPayment.getPaymentId());
                continue;
            }

            Payment updatePayment = new Payment();
            updatePayment.setId(payment.getId());
            updatePayment.setChannel(payment.getChannel()+"_back");
            OrderLevel orderLevel = OrderLevel.fromInt(OrderLevel.SHOP.getValue());
            String payInfoMd5 = PayInfoDigestor.digest(Lists.newArrayList(update.getId(),orderPayment.getId()), orderLevel, updatePayment.getChannel(), 0);
            updatePayment.setPayInfoMd5(payInfoMd5);
            paymentDao.update(updatePayment);
            orderPaymentDao.delete(orderPayment.getId());
        }

    }

    @Transactional
    public void shopOrderStatusChangedForReject(Long orderId,Long receiveShopId,String receiveShopName,Integer status,
                                                Integer currentStatus,Long orderDispatchRelationId){

        //更新订单中的shopId
        shopOrderDao.updateShopInfoById(orderId, receiveShopId, receiveShopName);
        //更新订单状态
        shopOrderDao.updateStatus(orderId,status);
        //更新子订单中的shopId
        skuOrderDao.updateShopInfoIdByOrderId(orderId,receiveShopId,receiveShopName);
        //更新子订单中的状态
        skuOrderDao.updateStatusByOrderId(orderId,currentStatus,status);
        //删除关联关系
        orderDispatchRelationDao.delete(orderDispatchRelationId);
    }


    /**
     * 重写parana中OrderManager中的create
     * 由于一级经销商下单,订单中的shopId需要为0L即平台接单,由于parana中子单拆单归组、发票、收货地址都是用了店铺Id归组(ListMultimap)
     * 为了复用parana中的归并方法,实例化订单之前的逻辑中一级下单的每个商品的店铺信息就用真实的店铺信息,这里实例化时根据订单类型再替换一下
     *
     * 规则:
     * 如果店铺为供应商类型则替换为平台店铺Id,其他情况下不变。
     *
     * 持久化订单相关的实体
     *
     * @param persistedOrderInfos 待持久化的订单相关的实体
     * @return 店铺订单id列表
     */
    @Override
    @Transactional
    public List<Long> create(PersistedOrderInfos persistedOrderInfos) {

        //持久化店铺订单
        List<ShopOrder> shopOrders = persistedOrderInfos.getShopOrders();
        if (CollectionUtils.isEmpty(shopOrders)) {
            return Collections.emptyList();
        }
        Long originalShopId;
        for (ShopOrder shopOrder : shopOrders) {
            originalShopId = shopOrder.getShopId();
            replaceOrderShopInfo(shopOrder);
            shopOrderDao.create(shopOrder);
            shopOrder.setShopId(originalShopId);//重新塞入真实店铺id便于下边拆子单
        }

        //持久化sku订单
        ListMultimap<Long, SkuOrder> skuOrdersByShopId = persistedOrderInfos.getSkuOrdersByShopId();
        for (ShopOrder shopOrder : shopOrders) {
            Long shopId = shopOrder.getShopId();
            List<SkuOrder> skuOrders = skuOrdersByShopId.get(shopId);
            if (CollectionUtils.isEmpty(skuOrders)) {
                throw new IllegalArgumentException("no skuOrder of shop where shopId=" + shopId);
            }
            for (SkuOrder skuOrder : skuOrders) {
                skuOrder.setOrderId(shopOrder.getId());
                skuOrderDao.create(skuOrder);
            }
        }

        //构建店铺id到店铺订单id的映射
        Map<Long, Long> shopIdToOrderId = Maps.newHashMapWithExpectedSize(shopOrders.size());
        for (ShopOrder shopOrder : shopOrders) {
            shopIdToOrderId.put(shopOrder.getShopId(), shopOrder.getId());
        }

        //持久化(子)订单对应的收货信息
        ListMultimap<Long, OrderReceiverInfo> orderReceiverInfosByShopId
                = persistedOrderInfos.getOrderReceiverInfosByShopId();


        if (!orderReceiverInfosByShopId.isEmpty()) {

            for (Long shopId : orderReceiverInfosByShopId.keySet()) {
                List<OrderReceiverInfo> orderReceiverInfos = orderReceiverInfosByShopId.get(shopId);

                //对应店铺id的sku订单列表
                List<SkuOrder> skuOrders = skuOrdersByShopId.get(shopId);

                for (int i = 0; i < orderReceiverInfos.size(); i++) {
                    OrderReceiverInfo orderReceiverInfo = orderReceiverInfos.get(i);

                    //设置对应的(子)订单id
                    if (orderReceiverInfo.getOrderLevel() == OrderLevel.SHOP) {//店铺级别的收货信息
                        orderReceiverInfo.setOrderId(shopIdToOrderId.get(shopId));
                    } else {
                        //sku级别的收货信息, 此时sku订单和收货信息一一对应
                        orderReceiverInfo.setOrderId(skuOrders.get(i).getId());
                    }
                    if (orderReceiverInfo.getReceiverInfo() != null) {//表示有收货信息
                        orderReceiverInfoDao.create(orderReceiverInfo);
                    }
                }
            }
        }

        //持久化(子)订单对应的发票信息
        ListMultimap<Long, OrderInvoice> orderInvoicesByShopId = persistedOrderInfos.getOrderInvoicesByShopId();
        if (!orderInvoicesByShopId.isEmpty()) {
            for (Long shopId : orderInvoicesByShopId.keySet()) {
                List<OrderInvoice> orderInvoices = orderInvoicesByShopId.get(shopId);
                //对应店铺id的sku订单列表
                List<SkuOrder> skuOrders = skuOrdersByShopId.get(shopId);

                for (int i = 0; i < orderInvoices.size(); i++) {
                    OrderInvoice orderInvoice = orderInvoices.get(i);
                    if (orderInvoice.getOrderLevel() == OrderLevel.SHOP) { //店铺级别的发票信息
                        orderInvoice.setOrderId(shopIdToOrderId.get(shopId));
                    } else {
                        //sku级别的发票信息, 此时sku订单和发票信息一一对应
                        orderInvoice.setOrderId(skuOrders.get(i).getId());
                    }
                    if (orderInvoice.getInvoiceId() != null) {//表示有发票信息
                        orderInvoiceDao.create(orderInvoice);
                    }
                }
            }
        }

        //返回店铺订单id列表
        List<Long> shopOrderIds = Lists.newArrayListWithCapacity(shopOrders.size());
        for (ShopOrder shopOrder : shopOrders) {
            shopOrderIds.add(shopOrder.getId());
        }
        return shopOrderIds;
    }

    private void replaceOrderShopInfo(ShopOrder shopOrder){

        Map<String, String> tagMap =  shopOrder.getTags();
        String roleName = tagMap.get("roleName");
        String platformShopName = tagMap.get("platformShopName");
        if(Objects.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            shopOrder.setShopId(DefaultId.PLATFROM_SHOP_ID);
            shopOrder.setShopName(platformShopName);
        }

    }

    @Transactional
    public void updateOrderPayType(Long orderId,Integer payType){
        ShopOrder shopOrder = new ShopOrder();
        SkuOrder skuOrder = new SkuOrder();
        shopOrder.setId(orderId);
        shopOrder.setPayType(payType);
        skuOrder.setPayType(payType);
        if (shopOrderDao.update(shopOrder)) {
            List<SkuOrder> skuOrders = skuOrderDao.findByOrderId(orderId);
            for (SkuOrder skuOrder1 : skuOrders) {
                skuOrder.setId(skuOrder1.getId());
                skuOrderDao.update(skuOrder);
            }
        }
    }

}
