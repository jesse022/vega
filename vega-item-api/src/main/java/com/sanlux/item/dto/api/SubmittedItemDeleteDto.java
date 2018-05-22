package com.sanlux.item.dto.api;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/27.
 */
@Data
public class SubmittedItemDeleteDto implements Serializable {
    private static final long serialVersionUID = 4605471276986388927L;

    @Valid
    @NotNull
    private SubmittedHeader header;


    @NotEmpty(message = "skuOutId.can.not.empty")
    private List<String> body;
}
