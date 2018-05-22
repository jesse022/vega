package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采checkIn接口Dto
 * Created by lujm on 2018/2/28.
 */
@Data
public class YouyuncaiCheckInDto implements Serializable {

    private static final long serialVersionUID = -5094782978867972716L;

    /**
     * 友云采用户cookie
     */
    private String buyerCookie;

    /**
     * 集乘网用户Id
     */
    private String custUserCode;

    /**
     * 友云采企业用户编码
     */
    private String userCode;

    /**
     * 友云采企业用户名
     */
    private String userName;

    /**
     * checkin时需跳转到的电商url
     */
    private String checkinRedirectUrl;

    /**
     * checkout时需跳转回的云采的url
     */
    private String checkoutRedirectUrl;
}
