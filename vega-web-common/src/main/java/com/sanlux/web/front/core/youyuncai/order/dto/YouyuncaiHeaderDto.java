package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 友云采请求header
 * Created by lujm on 2018/2/27.
 */
@Data
public class YouyuncaiHeaderDto implements Serializable {

    private static final long serialVersionUID = 5532608078834930154L;

    /**
     * 集乘网|友云采登录认证ID
     */
    @NotEmpty(message = "clientId不能为空")
    private String clientId;

    /**
     * 集乘网|友云采登录认证密码
     */
    @NotEmpty(message = "clientSecret不能为空")
    private String clientSecret;
}
