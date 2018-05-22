package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/18
 */
@Data
public class KdNiaoLogisticsDto implements Serializable {
    private static final long serialVersionUID = -5595247112548501010L;

    private Long shipmentId;
    private String shipmentSerialNo;
    private String shipmentCorpName;
    private String steps;
    private String extra;


}
