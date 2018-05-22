package com.sanlux.item.dto.api;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/16.
 */
@Data
public class SubmittedItemImportDto implements Serializable {

    private static final long serialVersionUID = -8683504438417840031L;

    @Valid
    @NotNull
    private SubmittedHeader header;

    @Valid
    @NotNull
    private List<ItemCreateApiDto> body;

}
