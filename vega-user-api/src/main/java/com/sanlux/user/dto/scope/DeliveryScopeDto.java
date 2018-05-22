package com.sanlux.user.dto.scope;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/8
 */
@Data
public class DeliveryScopeDto implements Serializable {


    private static final long serialVersionUID = -5410179541357511806L;

    /**
     * 省份id
     */
    private Integer provinceId;

    /**
     * 省份
     */
    private String province;

    /**
     * 市list
     */
    private List<DeliveryScopeCityDto> citiesMap;

}
