package com.sanlux.user.dto.scope;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/8/8
 */
@Data
public class DeliveryScopeRegionDto implements Serializable {


    private static final long serialVersionUID = 6745193716645759756L;

    /**
     * 区id
     */
    private Integer regionId;

    /**
     * 区
     */
    private String region;
}
