package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采下单请求Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class SubmittedOrderDto  implements Serializable{
    private static final long serialVersionUID = -8757337228914061153L;

    private YouyuncaiHeaderDto header;

    private YouyuncaiOrderDto body;
}
