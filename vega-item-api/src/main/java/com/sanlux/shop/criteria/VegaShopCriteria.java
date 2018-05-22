/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.shop.criteria;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
public class VegaShopCriteria extends PagingCriteria implements Serializable{
    private static final long serialVersionUID = 1297018130605006938L;

    private Long id;

    private String name;

    private Long userId;

    private Integer status;

    private Integer type;

    // 以下为VegaShopExtra分页条件

    private Long shopId;

    private Long shopPid;

    private String shopName;

    private String userName;

    private Integer shopStatus;

    private Integer shopType;

    private Integer shopAuthorize;

    private Integer isOldMember;

}
