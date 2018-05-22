package com.sanlux.user.dto.criteria;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询条件
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 7/21/16
 * Time: 4:03 PM
 */
@Data
public class RankCriteria extends PagingCriteria implements Serializable {


    private static final long serialVersionUID = 5922026147048799073L;

    //todo 添加查询条件
    private String name;
}
