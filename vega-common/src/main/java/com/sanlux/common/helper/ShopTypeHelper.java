/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.common.helper;

import com.google.common.base.Objects;
import com.sanlux.common.enums.VegaShopType;

/**
 * @author : panxin
 */
public class ShopTypeHelper {

    /**
     * 判断是否是供应商
     * @param type 店铺类型
     * @return 结果
     */
    public static Boolean isSupplierShop(Integer type) {
        return Objects.equal(VegaShopType.from(type).value(), VegaShopType.SUPPLIER.value());
    }

    /**
     * 判断是否是一级经销商
     * @param type 店铺类型
     * @return 结果
     */
    public static Boolean isFirstDealerShop(Integer type) {
        return Objects.equal(VegaShopType.from(type).value(), VegaShopType.DEALER_FIRST.value());
    }

    /**
     * 判断是否是二级经销商
     * @param type 店铺类型
     * @return 结果
     */
    public static Boolean isSecondDealerShop(Integer type) {
        return Objects.equal(VegaShopType.from(type).value(), VegaShopType.DEALER_SECOND.value());
    }

    /**
     * 判断是否是平台店铺
     * @param type 店铺类型
     * @return T/F
     */
    public static Boolean isPlatformShop(Integer type) {
        return Objects.equal(VegaShopType.from(type).value(), VegaShopType.PLATFORM.value());
    }

}
