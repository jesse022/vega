package com.sanlux.category.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 类目会员折扣
 * Created by cuiwentao
 * on 16/8/15
 */
@Data
public class MemberDiscountDto implements Serializable{

    private static final long serialVersionUID = -9008694949749575044L;

    /**
     * 会员ID
     */
    private Long memberLevelId;

    /**
     * 会员名称
     */
    private String memberLevelName;

    /**
     * 会员折扣
     */
    private Integer discount;

    public static MemberDiscountDto form(Long memberLevelId, String memberLevelName, Integer discount) {

        MemberDiscountDto memberDiscount = new MemberDiscountDto();
        memberDiscount.setMemberLevelId(memberLevelId);
        memberDiscount.setMemberLevelName(memberLevelName);
        memberDiscount.setDiscount(discount);
        return memberDiscount;
    }
}
