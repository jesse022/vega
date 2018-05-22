package com.sanlux.web.front.core.youyuncai.order.dto;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * Created by lujm on 2018/3/30.
 */
@Data
public class YouyuncaiUserSkuPriceDto implements Serializable{

    private static final long serialVersionUID = 3935038847634103119L;

    /**
     * 集乘网用户Id
     */
    @NotEmpty(message = "custUserCode不能为空")
    @Pattern(regexp = "^\\+?[1-9][0-9]*$", message = "集乘网用户Id必须为数字")
    private String custUserCode;

    /**
     * skuId
     */
    @NotEmpty(message = "skuCode不能为空")
    @Pattern(regexp = "^\\+?[1-9][0-9]*$", message = "集乘网skuId必须为数字")
    private String skuCode;

    /**
     * 价格
     */
    private Integer price;


}
