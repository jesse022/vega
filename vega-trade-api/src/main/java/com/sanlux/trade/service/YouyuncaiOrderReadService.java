package com.sanlux.trade.service;

import com.sanlux.trade.dto.YouyuncaiOrderCriteria;
import com.sanlux.trade.model.YouyuncaiOrder;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * 友云采订单读服务接口
 * Created by lujm on 2018/3/9.
 */
public interface YouyuncaiOrderReadService {
    /**
     * 根据集乘网订单Id获取友云采订单信息
     * @param orderId 集乘网订单Id
     * @return 友云采订单信息
     */
    Response<YouyuncaiOrder> findByOrderId(Long orderId);

    /**
     * 友云采订单分页查询
     * @param youyuncaiOrderCriteria 查询条件,包含
     * orderId       集乘网订单Id
     * userId        买家用户Id
     * orderCode     友云采订单Id
     * invoiceState  开票状态
     * hasInvoiced   是否已开票
     * startAt       开始时间
     * endAt         截止时间
     * pageNo        分页页数
     * pageSize      分页每页显示条数
     * @return 分页结果
     */
    Response<Paging<YouyuncaiOrder>> paging(YouyuncaiOrderCriteria youyuncaiOrderCriteria);
}
