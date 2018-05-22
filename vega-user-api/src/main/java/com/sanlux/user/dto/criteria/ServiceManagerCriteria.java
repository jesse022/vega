package com.sanlux.user.dto.criteria;

import lombok.Data;

import java.io.Serializable;

/**
 * 业务经理表查询条件
 *
 * Created by lujm on 2017/5/23.
 */
@Data
public class ServiceManagerCriteria extends PagingCriteria implements Serializable {

    private static final long serialVersionUID = -2671047821921809793L;

    private Long Id;
    private Long userId;
    private Long shopId;
    private String userName;
    private String mobile;
    private String name;
    private Integer status;
}
