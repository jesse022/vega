package com.sanlux.user.dto.criteria;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/11/7
 */
@Data
public class NotifyArticleCriteria extends PagingCriteria implements Serializable{
    private static final long serialVersionUID = -8474548146355223111L;

    private Integer status =1;

    private Integer notifySupplier;

    private Integer notifyDealerFirst;

    private Integer notifyDealerSecond;

}
