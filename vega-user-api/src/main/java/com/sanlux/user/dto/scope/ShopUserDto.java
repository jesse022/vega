package com.sanlux.user.dto.scope;

import com.sanlux.user.model.ShopUser;
import com.sanlux.user.model.ShopUserExtras;
import lombok.Data;

import java.io.Serializable;

/**
 * 经销商添加专属会员Dto
 *
 * Created by lujm on 2017/9/1.
 */
@Data
public class ShopUserDto implements Serializable {

    private static final long serialVersionUID = 4357277489742712150L;

    private ShopUser shopUser; //会员信息

    private ShopUserExtras shopUserExtras; //会员扩展信息


    private String mobile; // 会员手机号

    private String extra; // 备注信息

    private Long shopId; // 所属店铺Id

    private String shopName; // 所属店铺名称


    private String rankName; // 会员等级名称

    private Long rankId; // 会员等级Id

    private String serviceManagerName; // 所属业务经理名称

    private Long serviceManagerId; // 所属业务经理Id
}
