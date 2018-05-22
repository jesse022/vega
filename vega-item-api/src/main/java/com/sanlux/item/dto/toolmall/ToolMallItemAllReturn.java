package com.sanlux.item.dto.toolmall;

import com.sanlux.item.dto.api.ItemAttributesDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 土猫网API商品(全部)返回信息
 * Created by lujm on 2018/4/19.
 */
@Data
public class ToolMallItemAllReturn implements Serializable {
    private static final long serialVersionUID = -7718649680633212123L;

    /**
     * 错误码 0:成功
     */
    private Integer code;

    /**
     * 错误信息
     */
    private String message;
    /**
     * 下次查询开始时间
     */
    private String nextBeginTime;

    /**
     * 商品查询类型
     */
    private Integer type;

    /**
     * 返回数据结果集
     */
    private ReturnDate data;

    @Data
    public static  class ReturnDate implements Serializable {
        private static final long serialVersionUID = 1012127374665853813L;

        /**
         * 页码
         */
        private Integer pageNum;

        /**
         * 每页显示条数
         */
        private Integer pageSize;

        /**
         * 数据集合
         */
        private List<AllListDate> list;
    }

    @Data
    public static  class AllListDate implements Serializable {
        private static final long serialVersionUID = -5924773358933109673L;

        /**
         * 土猫网商品编码（土猫网唯一）
         */
        private String toolmallCode;

        /**
         * SKU 编码(对应集乘网skuId)
         */
        private String skuCode;

        /**
         * 商品名称
         */
        private String name;

        /**
         * 商品全程
         */
        private String fullName;

        /**
         * 商品外链地址
         */
        private String link;

        /**
         * 土猫网销售价
         */
        private Float price;

        /**
         * 土猫协议价(对应集乘网供货价)
         */
        private Float agreePrice;

        /**
         * 制造商型号
         */
        private String makeModel;

        /**
         * 商品主图
         */
        private String image;

        /**
         * 单位
         */
        private String unit;

        /**
         * 重量
         */
        private String weghit;

        /**
         * 库存
         */
        private Integer stock;

        /**
         * 是否上架：0-下架；1-上架
         */
        private Integer isMarketable;

        /**
         * 商品介绍
         */
        private String introduction;

        /**
         * 商品简介
         */
        private String brief;

        /**
         * 品牌名称
         */
        private String brandName;

        /**
         * 货品 ID（同一类货品下属同一种商品的不同尺寸颜色等）,对应集乘网itemId
         */
        private String spuId;

        /**
         * 货品名称
         */
        private String spuName;

        /**
         * 商品分类
         */
        private Integer categoryId;

        /**
         * 商品详情图集合列表
         */
        private List<SkuImages> skuImages;

        /**
         * 商品销售属性
         */
        private List<ItemAttributesDto> sellAttrs;

        /**
         * 商品参数属性
         */
        private List<ItemAttributesDto> paramAttrs;
    }

    @Data
    public static  class SkuImages implements Serializable {
        private static final long serialVersionUID = 4978254476229492601L;

        /**
         * 图片标题
         */
        private String title;

        /**
         * 图片来源
         */
        private String source;

        /**
         * 图片大图
         */
        private String large;

        /**
         * 中图
         */
        private String medium;

        /**
         * 尺寸 400 的中图
         */
        private String Medium_400;

        /**
         * 小图
         */
        private String thumbnail;
    }

    @Data
    public static class Attrs implements Serializable {
        private static final long serialVersionUID = 4710529662089736456L;

        /**
         * 属性名称
         */
        private String attrsKey;

        /**
         * 属性值
         */
        private String attrsValue;


    }


}
