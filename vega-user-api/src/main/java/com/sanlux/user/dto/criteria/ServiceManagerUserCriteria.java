package com.sanlux.user.dto.criteria;

import lombok.Data;

import java.io.Serializable;

/**
 * 业务经理会员表查询条件
 *
 * Created by lujm on 2017/5/24.
 */
@Data
public class ServiceManagerUserCriteria extends PagingCriteria implements Serializable {
    private static final long serialVersionUID = -4733783120117787323L;

    private Long userId;
    private String userName;
    private String mobile;
    private Long serviceManagerId;
}
