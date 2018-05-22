package com.sanlux.shop.impl.dao;

import com.sanlux.shop.model.VegaShopExtraBasicInfo;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * 经销商基础信息扩展表Dao类
 * Created by lujm on 2017/12/19.
 */
@Repository
public class VegaShopExtraBasicInfoDao extends MyBatisDao<VegaShopExtraBasicInfo> {

    /**
     * 根据店铺Id获取经销商基础信息扩展表信息
     * @param shopId 店铺Id
     * @return VegaShopExtraBasicInfo
     */
    public VegaShopExtraBasicInfo findByShopId(Long shopId) {
        return getSqlSession().selectOne(sqlId("findByShopId"), shopId);
    }

    /**
     * 根据店铺Id获取经销商基础信息扩展表信息
     * @param macAddress 店铺客户端唯一地址
     * @return VegaShopExtraBasicInfo
     */
    public VegaShopExtraBasicInfo findByMacAddress(String macAddress) {
        return getSqlSession().selectOne(sqlId("findByMacAddress"), macAddress);
    }


    /**
     * 根据店铺Id修改经销商基础信息扩展表信息
     * @param vegaShopExtraBasicInfo vegaShopExtraBasicInfo
     * @return 是否成功
     */
    public Boolean updateByShopId(VegaShopExtraBasicInfo vegaShopExtraBasicInfo) {
        return getSqlSession().update(sqlId("updateByShopId"), vegaShopExtraBasicInfo) == 1;
    }
}
