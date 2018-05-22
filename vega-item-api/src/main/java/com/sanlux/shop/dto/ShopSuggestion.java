/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopSuggestion implements Serializable{

    private static final long serialVersionUID = -2909209231498469919L;

    /**
     * 店铺ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 店铺名称
     */
    private String name;

}
