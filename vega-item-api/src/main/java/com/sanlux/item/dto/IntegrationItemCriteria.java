package com.sanlux.item.dto;

import com.sanlux.item.model.IntegrationItem;
import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
@Data
public class IntegrationItemCriteria extends PagingCriteria implements Serializable {

    private static final long serialVersionUID = 6151883913486016210L;

    /**
     * 积分商品ID
     */
    private Long id;

    /**
     * 积分商品名称
     */
    private String name;

    /**
     * 积分商品状态
     */
    private Integer status;

    /**
     * 积分
     */
    private Integer integrationPrice;

    /**
     * 排序依据 integrationPrice id
     */
    private String sortBy;

    /**
     * 1:ASC  2:DESC
     */
    private Integer sortType;
}
