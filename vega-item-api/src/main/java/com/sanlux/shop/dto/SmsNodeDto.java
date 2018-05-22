package com.sanlux.shop.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 短信节点dto
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 9/12/16
 * Time: 1:41 PM
 */
@Data
public class SmsNodeDto implements Serializable{

    private static final long serialVersionUID = 91633587161814128L;

    /**
     * 信用额度节点
     */
    private List<SmsNode> creditNodes;
    /**
     * 交易节点
     */
    private List<SmsNode> tradeNodes;

}
