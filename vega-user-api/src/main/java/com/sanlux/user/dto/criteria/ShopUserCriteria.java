package com.sanlux.user.dto.criteria;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询条件
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 4:03 PM
 */
@Data
public class ShopUserCriteria extends PagingCriteria implements Serializable {
    private static final long serialVersionUID = 1195549357692204173L;
    //todo 添加查询条件
    private String userName;
    private String mobile;
    private Long shopId;
    private Integer rank;
}
