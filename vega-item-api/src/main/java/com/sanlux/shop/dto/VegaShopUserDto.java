/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import com.sanlux.shop.model.VegaShopExtra;
import io.terminus.parana.shop.model.Shop;
import lombok.Data;

import java.io.Serializable;

/**
 * 该DTO只用于运营添加用户
 *
 * @author : panxin
 */
@Data
public class VegaShopUserDto implements Serializable{

    private static final long serialVersionUID = -8136942248358793246L;

    private Shop shop;

    private VegaShopExtra shopExtra;

    private String userName;

    private String password;

    private String mobile;

    public VegaShopUserDto() {

    }

    public VegaShopUserDto(Shop shop, VegaShopExtra shopExtra) {
        this.shop = shop;
        this.shopExtra = shopExtra;
    }

}
