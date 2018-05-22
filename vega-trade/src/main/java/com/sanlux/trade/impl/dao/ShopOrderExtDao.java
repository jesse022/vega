package com.sanlux.trade.impl.dao;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sanlux.trade.dto.fsm.VegaOrderStatus;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Constants;
import io.terminus.parana.order.model.ShopOrder;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 店铺订单扩展Dao
 *
 * Created by lujm on 2017/06/01.
 */
@Repository
public class ShopOrderExtDao extends MyBatisDao<ShopOrder> {


    /**
     * 根据买家用户Ids和起止时间查询店铺订单信息
     *
     * @param map 查询条件
     * @return List<ShopOrder>
     */
    public List<ShopOrder> findByBuyerIds(Map<String, Object> map) {
        return getSqlSession().selectList(sqlId("findByBuyerIds"), map);
    }

    /**
     * 买家订单分页列表,不显示删除标志的订单
     *
     * @param offset 起始偏移
     * @param limit 分页大小
     * @param criteria Map查询条件
     * @return 查询到的分页对象
     */
    public Paging<ShopOrder> pagingByBuyer(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        //不显示已删除的订单
        if(Objects.isNull(criteria.get("status"))) {
            criteria.put("statusNotIn", ImmutableList.of(
                    VegaOrderStatus.BUYER_DELETE.getValue()//买家已删除
            ));
        }

        Long total = sqlSession.selectOne(sqlId("countExt"), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<ShopOrder>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        List<ShopOrder> datas = sqlSession.selectList(sqlId("pagingExt"), criteria);
        return new Paging<>(total, datas);
    }

    /**
     * 一级经销商查询二级经销商订单
     *
     * @param offset 起始偏移
     * @param limit 分页大小
     * @param criteria Map查询条件
     * @return 查询到的分页对象
     */
    public Paging<ShopOrder> pagingSecondShopOrder(Integer offset, Integer limit, Map<String, Object> criteria, List<Long> ShopIds) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        //塞入二级经销商Ids
        if (Objects.isNull(criteria.get("shopId")) && !Arguments.isNullOrEmpty(ShopIds)) {
            criteria.put("shopIds", ShopIds);
        }

        Long total = sqlSession.selectOne(sqlId("countExt"), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<ShopOrder>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        List<ShopOrder> datas = sqlSession.selectList(sqlId("pagingExt"), criteria);
        return new Paging<>(total, datas);
    }

    /**
     * 分页按时间顺序查看今日付款订单
     * @param offset   翻页起始编号
     * @param limit    翻页每页条数
     * @param criteria 查询条件
     * @return 查询分页结果
     */
    public Paging<ShopOrder> pagingTodayPaymentOrder(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        Long total = sqlSession.selectOne(sqlId("countTodayPaymentOrder"), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<ShopOrder>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        List<ShopOrder> datas = sqlSession.selectList(sqlId("pagingTodayPaymentOrder"), criteria);
        return new Paging<>(total, datas);
    }

    /**
     * 获取今日付款订单数量
     * @param criteria 查询条件
     * @return 查询结果
     */
    public Long countTodayPayment(Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        return sqlSession.selectOne(sqlId("countTodayPaymentOrder"), criteria);
    }

}
