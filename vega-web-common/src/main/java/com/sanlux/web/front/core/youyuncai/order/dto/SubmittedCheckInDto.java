package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采checkIn请求
 * Created by lujm on 2018/2/28.
 */
@Data
public class SubmittedCheckInDto implements Serializable {

    private static final long serialVersionUID = 348004324735584255L;

    private YouyuncaiHeaderDto header;

    private YouyuncaiCheckInDto body;

}
