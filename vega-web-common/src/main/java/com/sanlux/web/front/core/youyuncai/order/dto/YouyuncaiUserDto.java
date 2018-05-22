package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采用户Dto
 * Created by lujm on 2018/2/27.
 */
@Data
public class YouyuncaiUserDto implements Serializable {

    private static final long serialVersionUID = -985304911661316443L;

    /**
     * 友云采企业用户编码
     */
    private String youyuncaiUserCode;

    /**
     * 友云采企业用户名
     */
    private String youyuncaiUserName;

    /**
     * 友云采企业编码
     */
    private String youyuncaiGroupCode;

    /**
     * 友云采企业名称
     */
    private String youyuncaiGroupName;

    /**
     * 友云采企业机构编码
     */
    private String youyuncaiOrgCode;

    /**
     * 友云采企业机构名称
     */
    private String youyuncaiOrgName;

    /**
     * 联系人名称
     */
    private String userName;

    /**
     * 可直接登录集乘网初始密码，账号为手机号
     */
    private String userPassword;

    /**
     * 联系人手机号
     */
    private String userMobile;
}
