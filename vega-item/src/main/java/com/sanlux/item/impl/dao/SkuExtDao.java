package com.sanlux.item.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import io.terminus.parana.item.model.Sku;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/10/28
 */
@Repository
public class SkuExtDao extends MyBatisDao<Sku> {

    public Integer batchUpdateStockByShopIdAndId(List<Sku> skus) {
        return getSqlSession().update(sqlId("batchUpdateStockByShopIdAndId"), skus);
    }

    public List<Sku> findByOuterSkuIds (List<String> outerSkuIds) {
        return getSqlSession().selectList(sqlId("findByOuterSkuIds"), outerSkuIds);
    }

    /**
     * 获取店铺所有已冻结的SKU信息
     * @param shopId 店铺Id
     * @return SKU信息
     */
    public List<Sku> findFrozenItemsByShopId (Long shopId) {
        return getSqlSession().selectList(sqlId("findFrozenItemsByShopId"), ImmutableMap.of("shopId", shopId));
    }

    /**
     * 获取特定状态下所有SKU信息
     * @return SKU信息
     */
    public List<Sku> findSpeciallyStatusSkus (Integer status) {
        return getSqlSession().selectList(sqlId("findSpeciallyStatusSkus"), ImmutableMap.of("status", status));
    }

    /**
     * 根据商品Ids查询所有SKU信息(包括已经删除的)
     * @param itemIds 商品Ids
     * @return SKU信息
     */
    public List<Sku> findAllByItemIds(List<Long> itemIds) {
        return this.getSqlSession().selectList(this.sqlId("findAllByItemIds"), itemIds);
    }

    public List<Sku> findAllSkuWithPrice () {
        return getSqlSession().selectList(sqlId("findAllSkuWithPrice"));
    }

    public Boolean batchUpdateOuterSkuId (List<Sku> skus) {
        return getSqlSession().update(sqlId("batchUpdateOuterSkuId"), skus) > 0;
    }

    /**
     * 批量逻辑删除商品,并更新outer_sku_id,后面加当前时间戳
     * @param itemIds 商品IDs
     * @return 是否成功
     */
    public Boolean batchDeleteByItemIds (List<Long> itemIds) {
        return getSqlSession().update(sqlId("batchDeleteByItemIds"), itemIds) > 0;
    }

    /**
     * 修改冻结状态商品的供货价格,并把商品状态设置成指定状态
     *
     * @param sku sku
     * @return 是否成功
     */
    public Boolean updateSellerPriceById (Sku sku) {
        return getSqlSession().update(sqlId("updateSellerPriceById"), sku) > 0;
    }

    /**
     * 查询SKU同步信息
     * @param criteria 查询条件
     * @return skuList
     */
    public Paging<Sku> pagingBySkuSync(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        Long total = getSqlSession().selectOne(sqlId("countBySkuSync"), criteria);
        if (total <= 0){
            return new Paging<>(0L, Collections.<Sku>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        List<Sku> datas = getSqlSession().selectList(sqlId("pagingBySkuSync"), criteria);
        return new Paging<>(total, datas);
    }

}
