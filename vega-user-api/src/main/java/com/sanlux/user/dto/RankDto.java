package com.sanlux.user.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/8/12.
 */
@Data
public class RankDto implements Serializable {

    private static final long serialVersionUID = 5757318133178533430L;
    private Long id;
    private String name;

}
