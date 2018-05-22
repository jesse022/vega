package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采出货通知订单明细Dto
 * Created by lujm on 2018/3/12.
 */
@Data
public class YouyuncaiShipInfoOrderDetailDto  implements Serializable {

    private static final long serialVersionUID = 7005646815191649536L;

    /**
     * 友云采明细号码
     */
    private String lineNumber;

    /**
     * 集乘网明细行号
     */
    private String supplierlineNumber;

    /**
     * 出货数量
     */
    private String quantity;

    /**
     * 出货日
     */
    private String shipSheduleDate;

    /**
     * 运单号码
     */
    private String wayBillNumber;

    /**
     * 物流公司
     */
    private String carrierIdentifier;
}
