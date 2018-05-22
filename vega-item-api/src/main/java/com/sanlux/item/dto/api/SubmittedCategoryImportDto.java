package com.sanlux.item.dto.api;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * Created by lujm on 2018/3/27.
 */
@Data
public class SubmittedCategoryImportDto implements Serializable {

    private static final long serialVersionUID = 6503378703416197590L;

    @Valid
    @NotNull
    private SubmittedHeader header;

    @Valid
    @NotNull
    private List<CategoryCreateApiDto> body;
}
