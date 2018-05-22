package com.sanlux.youyuncai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采授权接口返回信息
 * Created by lujm on 2018/5/15.
 */
@Data
public class YouyuncaiTokenReturnStatus implements Serializable {

    private static final long serialVersionUID = -7380139137372854335L;

    /**
     * 失效时间
     */
    private Long expiretime;

    /**
     * 过期时间
     */
    private Long expirein;

    /**
     * 授权时间
     */
    private Long generatetime;

    /**
     * 授权信息
     */
    private String accesstoken;
}
