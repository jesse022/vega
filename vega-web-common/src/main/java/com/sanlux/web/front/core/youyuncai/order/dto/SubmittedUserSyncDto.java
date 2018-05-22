package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 友云采用户同步请求
 * Created by lujm on 2018/2/27.
 */
@Data
public class SubmittedUserSyncDto implements Serializable{

    private static final long serialVersionUID = -7654825839096368633L;

    private YouyuncaiHeaderDto header;

    private List<YouyuncaiUserDto> body;
}
