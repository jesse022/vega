package com.sanlux.user.dto.scope;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/8
 */
@Data
public class DeliveryScopeCityDto implements Serializable {


    private static final long serialVersionUID = 7583303027073627572L;

    /**
     * 市ID
     */
    private Integer cityId;

    /**
     * 市名
     */
    private String city;

    /**
     * 区list
     */
    private List<DeliveryScopeRegionDto> regionMap;


}
