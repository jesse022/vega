package com.sanlux.user.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2017/2/23.
 */
@Data
public class Nation implements Serializable {
    private static final long serialVersionUID = -46571316630623417L;
    private Long id;
    /**
     * 行政区划代码
     */
    private String code;

    /**
     * 省份
     */
    private String province;
    /**
     * 城市
     */
    private String city;
    /**
     * 区县
     */
    private String district;

    /**
     * 上级代码
     */
    private String parent;

    /**
     * 七鱼客服ID
     */
    private String staffId;

    /**
     * 七鱼客服组ID
     */
    private String groupId;
}
