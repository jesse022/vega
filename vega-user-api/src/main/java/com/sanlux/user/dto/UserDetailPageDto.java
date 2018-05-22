package com.sanlux.user.dto;

import io.terminus.common.model.Paging;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liangfujie on 16/8/12
 */
@Data
public class UserDetailPageDto implements Serializable {
    private static final long serialVersionUID = 2005630208320304907L;
    private Paging<UserDetail> page;
    private Integer shopNowDiscount;
    private List<RankDto> ranks;

}
