package com.sanlux.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 信用额度短信节点
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/12/16
 * Time: 1:42 PM
 */
@Data
public class SmsNode implements Serializable{


    private static final long serialVersionUID = 6415739774690866151L;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 节点描述
     */
    private String nodeDesc;

    /**
     * 是否选中 0 未选中 1 已选中
     */
    private Integer isChecked;
}
