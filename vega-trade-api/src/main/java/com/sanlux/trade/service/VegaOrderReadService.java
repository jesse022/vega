package com.sanlux.trade.service;

import com.sanlux.trade.dto.VegaOrderDetail;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.dto.OrderGroup;
import io.terminus.parana.order.model.ShopOrder;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by liangfujie on 16/8/26
 */
public interface VegaOrderReadService {

    Response<VegaOrderDetail> pagingVegaOrderDetailByShopId(Long shopOrderId, FlowPicker flowPicker,Integer pageNo,Integer pageSize);

    Response<VegaOrderDetail> findVegaOrderDetailByShopOrderId(Long shopOrderId);

    /**
     * 根据买家用户Ids和起止时间查询店铺订单信息,只用于导出业务经理专属会员订单信息
     *
     * @param startAt 起始时间
     * @param endAt   截止时间
     * @param buyerIds 买家userIds
     * @return 订单信息
     */
    Response<List<ShopOrder>> findShopOrderByBuyerIds(Date startAt, Date endAt, List<Long> buyerIds);

    /**
     * 根据店铺Ids和下单时间查询已付款状态订单,用于运营报表统计
     *
     * @param startAt 起始时间
     * @param endAt   截止时间
     * @param shopIds  店铺Ids
     * @return 订单信息
     */
    Response<List<VegaOrderDetail>> findShopOrderByShopIds(Date startAt, Date endAt, List<Long> shopIds);

    /**
     * 根据订单Ids获取订单及发货信息
     *
     * @param orderIds  订单ds
     * @return 订单信息
     */
    Response<List<VegaOrderDetail>> findShopOrderAndReceiverInfoByOrderIds(List<Long> orderIds);

    Response<Paging<ShopOrder>> pagingShopOrder(Integer pageNo,Integer pageSize,Map<String, Object> criteria);

    Response<VegaOrderDetail.SkuOrderAndOperation> findSkuOrderDetailById(Long id,FlowPicker flowPicker);
    /**
     * 订单分页列表
     *
     * @param  orderCriteria 查询条件:
     *                       buyerId 买家id
     *                       buyerName 买家名称(全匹配)
     *                       shopId 店铺id
     *                       shopName 店铺名称(全匹配)
     *                       companyId 公司id
     *                       status 订单状态列表
     *                       invoiced 是否开具过发票
     *                       startAt 创建时间, 起始时间
     *                       endAt 创建时间, 截止时间
     * @return 分页订单
     */
    Response<Paging<OrderGroup>> findBy(OrderCriteria orderCriteria);

    /**
     * 根据订单Ids获取订单分页列表
     * @param shopOrderIds   订单Ids
     * @param skuOrderLimit  子订单查询条数
     * @return 订单分页信息
     */
    Response<List<OrderGroup>> findByShopOrderIds(List<Long> shopOrderIds, Integer skuOrderLimit);

    /**
     * 买家订单分页列表,不显示删除标志的订单
     *
     * @param  orderCriteria 查询条件:
     *                       orderId 订单号
     *                       shopName 店铺名称(全匹配)
     *                       status 订单状态列表
     *                       startAt 创建时间, 起始时间
     *                       endAt 创建时间, 截止时间
     * @return 分页订单
     */
    Response<Paging<OrderGroup>> findByBuyer(OrderCriteria orderCriteria);

    /**
     * 一级经销商查询二级经销商订单列表
     *
     * @param orderCriteria 查询条件
     * @param ShopIds 二级店铺Ids
     * @return
     */
    Response<Paging<OrderGroup>> pagingSecondShopOrder(OrderCriteria orderCriteria, List<Long> ShopIds);

    /**
     * 分页按时间先后顺序查看今日(可以设置起始时间)付款订单
     * @param orderCriteria 查询条件
     * @return 分页订单
     */
    Response<Paging<OrderGroup>> pagingTodayPaymentOrder(OrderCriteria orderCriteria);

    /**
     * 获取今日付款订单数量
     * @param orderCriteria 查询条件
     * @return 订单数量
     */
    Response<Long> countTodayPaymentOrder (OrderCriteria orderCriteria);


}
