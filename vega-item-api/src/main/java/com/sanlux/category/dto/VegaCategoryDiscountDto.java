package com.sanlux.category.dto;

import lombok.Data;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.util.List;
import java.util.Map;

/**
 * 经销商授权类目表中的经销商针对类目设置的折扣表
 * Created by cuiwentao
 * on 16/8/4
 */
@Data
public class VegaCategoryDiscountDto implements Serializable{


    private static final long serialVersionUID = 7977804054230372409L;
    /**
     * 类目ID
     */
    private Long categoryId;

    /**
     * 类目名称
     */
    private String categoryName;

    /**
     * 上级类目ID
     */
    private Long categoryPid;

    /**
     * 类目级别
     */
    private Integer categoryLevel;

    /**
     * 一级经销商类目折扣
     */
    private Float categoryDiscount;

    /**
     * 类目折扣是否启用
     */
    private Boolean isUse;

    /**
     * 类目会员折扣
     */
    private List<MemberDiscountDto> categoryMemberDiscount;

    public static VegaCategoryDiscountDto form (Long categoryId, String categoryName,
                                                Long categoryPid, Integer categoryLevel,
                                                List<MemberDiscountDto> categoryMemberDiscount,
                                                Boolean isUse) {

        VegaCategoryDiscountDto vegaCategoryDiscount = new VegaCategoryDiscountDto();

        vegaCategoryDiscount.setCategoryId(categoryId);
        vegaCategoryDiscount.setCategoryName(categoryName);
        vegaCategoryDiscount.setCategoryPid(categoryPid);
        vegaCategoryDiscount.setCategoryLevel(categoryLevel);
        vegaCategoryDiscount.setCategoryMemberDiscount(categoryMemberDiscount);
        vegaCategoryDiscount.setIsUse(isUse);

        return vegaCategoryDiscount;
    }
}
