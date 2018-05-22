package com.sanlux.item.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/10/17
 */
@Data
public class VegaItemUploaded implements Serializable {


    private static final long serialVersionUID = -1039162507612971714L;

    private Long shopId;

    private List<ItemsToCreate> items;
}
