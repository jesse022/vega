package com.sanlux.item.model;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/4/19.
 */
@Data
public class ToolMallItemSync implements Serializable{
    private static final long serialVersionUID = -3587113434358978161L;

    /**
     * 同步类型,1: 商品分类, 2: 商品(整体),3:商品（价格、上下架、库存等）
     */
    private Integer type;

    /**
     * 下次查询开始时间
     */
    private String nextBeginTime;

    /**
     * 扩展字段
     */
    private String extraJson;
}
