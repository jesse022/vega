package com.sanlux.item.dto.api;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * Created by lujm on 2018/3/16.
 */
@Data
public class SubmittedHeader implements Serializable{

    private static final long serialVersionUID = -609068030476205850L;

    /**
     * 认证ID
     */
    @NotEmpty(message = "clientId.can.not.empty")
    private String clientId;

    /**
     * 认证密码
     */
    @NotEmpty(message = "clientSecret.can.not.empty")
    private String clientSecret;

}
