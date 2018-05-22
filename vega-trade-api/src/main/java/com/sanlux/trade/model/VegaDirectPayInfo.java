package com.sanlux.trade.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by liangfujie on 16/10/28
 */
@Data
public class VegaDirectPayInfo implements Serializable {

    private static final long serialVersionUID = -7298528402210251459L;

    private Long id;

    private Long orderId;

    private String businessId;

    private Integer status;

    private String describe;

    private String extraJson;

    private Date createdAt;

    private Date updatedAt;


}
