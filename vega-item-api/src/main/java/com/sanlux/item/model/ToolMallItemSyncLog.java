package com.sanlux.item.model;

import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 土猫网数据同步日志
 * Created by lujm on 2018/4/18.
 */
@Data
public class ToolMallItemSyncLog implements Serializable {
    private static final long serialVersionUID = -7944721949870468125L;

    private Long id;

    /**
     * 同步类型,1: 商品分类, 2: 商品(整体),3:商品（价格、上下架、库存等）
     */
    private Integer type;

    /**
     * 同步标志,0: 未同步,1: 同步成功, 2: 同步失败
     */
    private Integer status;

    /**
     * 查询条件：开始时间
     */
    private String beginTime;

    /**
     * 查询条件：结束时间
     */
    private String endTime;

    /**
     * 查询条件：页码
     */
    private Integer pageNum;

    /**
     * 查询条件：每页显示条数
     */
    private Integer pageSize;

    /**
     * 查询条件：商品查询类型 1：整体 2：价格 4：库存 8：商品上下架
     */
    private Integer syncType;

    /**
     * vega_item_imports表主键
     */
    private Integer importId;

    /**
     * 土猫网返回json格式数据
     */
    private String itemImportJson;

    /**
     * 失败时的错误原因 (错误报告)
     */
    private String errorResult;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

}
