package com.sanlux.youyuncai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2018/2/1.
 */
@Data
public class SkuDto implements Serializable {

    private static final long serialVersionUID = -2835395794633834174L;

    /**
     * 商品编码,必须
     */
    private String skuCode;

    /**
     * 商品名称,必须
     */
    private String name;

    /**
     * 商品标题,必须
     */
    private String subject;

    /**
     * 品牌,非必须
     */
    private String brands;

    /**
     * 商品分类编码,必须
     */
    private String productClass;

    /**
     * 商品状态,必须 0：未上架 1：销售中
     */
    private String statusCode;

    /**
     * 单位,必须
     */
    private String cunit;

    /**
     * 库存总量,非必须
     */
    private String onhand;

    /**
     * 重量,非必须
     */
    private String weight;

    /**
     * 运费方案,非必须
     */
    private String freightPlan;

    /**
     * 其他信息(备注),非必须
     */
    private String memo;

    /**
     * 标准含税单价,必须
     */
    private String taxPrice;

    /**
     * 标准税率,必须
     */
    private String taxrate;

    /**
     * 标准无税单价,必须
     */
    private String price;

    /**
     * 商品图片,必须,商品基本图片url，数组
     */
    private Object[] pictures;

    /**
     * 详细信息,必须,富文本格式字段
     */
    private String detailInfo;

    /**
     * 规格参数,必须,json字符串
     */
    private String parameter;

    /**
     * 售后服务,非必须
     */
    private String qualityService;

    /**
     * 包装清单,非必须
     */
    private String packingList;

    /**
     * 货期,非必须
     */
    private String deliveryTime;

    /**
     * 商品链接URL,必须
     */
    private String punchoutUrl;

    /**
     * 分词结果,必须
     */
    private String keywords;


    public enum TaxRate {

        STANDARD("0.17");

        private final String value;

        public final String value() {
            return value;
        }

        TaxRate(String value) {
            this.value = value;
        }
    }

}
