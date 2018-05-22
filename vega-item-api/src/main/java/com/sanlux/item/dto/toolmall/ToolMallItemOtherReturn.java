package com.sanlux.item.dto.toolmall;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/4/19.
 */
@Data
public class ToolMallItemOtherReturn implements Serializable {

    private static final long serialVersionUID = 4144634853982340041L;

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

        private static final long serialVersionUID = 2767911569021008183L;

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
        private List<OtherListDate> list;
    }

    @Data
    public static  class OtherListDate implements Serializable {
        private static final long serialVersionUID = -4317796360371319084L;

        /**
         * 土猫网商品编码（土猫网唯一）
         */
        private String toolmallCode;

        /**
         * SKU 编码(对应集乘网skuId)
         */
        private String skuCode;

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
         * 库存
         */
        private Integer stock;

        /**
         * 是否上架：0-下架；1-上架
         */
        private Integer isMarketable;
    }
}
