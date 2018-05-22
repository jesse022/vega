package com.sanlux.user.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by liangfujie on 16/8/10
 */
@Data
public class AdminUserDto implements Serializable {

    private static final long serialVersionUID = 7118250999970556822L;
    private Long id;

    private String name;

    private String mobile;

    private String email;

    private Date createdAt;//注册时间

    private Integer status;

    private Long integration;//用户积分

    private Long growthValue;//用户成长值

    private String userTypeName;//用户类型


}
