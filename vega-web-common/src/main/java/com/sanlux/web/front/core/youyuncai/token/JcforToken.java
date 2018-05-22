package com.sanlux.web.front.core.youyuncai.token;

import lombok.Data;

import java.io.Serializable;

/**
 * 集乘网提供给友云采的token
 * Created by lujm on 2018/2/27.
 */
@Data
public final class JcforToken implements Serializable {

    private static final long serialVersionUID = -1917387340099245876L;

    /**
     * 认证Id
     */
    private String clientId;

    /**
     * 认证密码
     */
    private String clientSecret;

    public JcforToken () {
        String clientId = "youyuncai";
        String clientSecret = "E86364E227C62B0B"; // youyuncai1234 MD5

        setClientId(clientId);
        setClientSecret(clientSecret);
    }
}
