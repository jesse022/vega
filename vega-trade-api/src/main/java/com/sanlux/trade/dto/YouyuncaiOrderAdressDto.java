package com.sanlux.trade.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 友云采地址信息Dto
 * Created by lujm on 2018/3/7.
 */
@Data
public class YouyuncaiOrderAdressDto implements Serializable{
    private static final long serialVersionUID = -4764860462184243669L;

    private area area0;

    private area area1;

    private area area2;

    private area area3;

    private String detailAddress;

    @Data
    public class area implements Serializable{
        private static final long serialVersionUID = -5969603007025700843L;

        private String code;

        private String name;
    }
}
