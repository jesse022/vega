package com.sanlux.item.dto.toolmall;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 土猫网API类目返回信息
 * Created by lujm on 2018/4/16.
 */
@Data
public class ToolMallCategoryReturn implements Serializable{
    private static final long serialVersionUID = -5634200111881869189L;

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
     * 返回数据结果集
     */
    private List<CategoryReturnDate> data;


    @Data
    public static  class CategoryReturnDate implements Serializable {

        private static final long serialVersionUID = 8817823493518263237L;

        /**
         * 分类Id
         */
        private Integer id;

        /**
         * 类目级别
         */
        private Integer grade;

        /**
         * 分类名称
         */
        private String name;

        /**
         * 图片地址
         */
        private String image;

        /**
         * 子节点结果集
         */
        private List<CategoryReturnDate> children;
    }
}
