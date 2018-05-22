package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/30.
 */
@Data
public class SubmittedUserSkuPriceDto  implements Serializable{
    private static final long serialVersionUID = 2879684444880878600L;

    @Valid
    @NotNull
    private YouyuncaiHeaderDto header;

    @Valid
    @NotNull
    private List<YouyuncaiUserSkuPriceDto> body;
}
