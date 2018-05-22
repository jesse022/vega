package com.sanlux.user.dto;

import com.sanlux.user.model.ShopUser;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/8/8.
 */
@Data
public class UserDetail implements Serializable {
    private static final long serialVersionUID = 3361938093153480178L;

    private ShopUser shopUser;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 用户状态
     */
    private Integer status;


    /**
     * 等级名称
     */
    private String rankName;

    /**
     * 用户等级ID,先存一个以后可能会用到.
     */
    private Long rankId;





}
