package com.sanlux.store.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by lujm on 2017/3/15.
 */
@Data
public class StoreImports implements Serializable {

    private static final long serialVersionUID = -5922743454756072945L;

    private Long id;

    private String batchNo;

    private Integer sonBatchNo;

    private Long userId;

    private String userName;

    private Integer status;

    private Integer type;

    private String result;

    private Date createdAt;

    private Date updatedAt;
}
