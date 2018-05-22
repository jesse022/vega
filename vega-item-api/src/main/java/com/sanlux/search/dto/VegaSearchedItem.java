package com.sanlux.search.dto;

import io.terminus.parana.search.dto.SearchedItem;
import lombok.Data;

/**
 * Created by cuiwentao
 * on 2/17/17
 */
@Data
public class VegaSearchedItem extends SearchedItem {

    private static final long serialVersionUID = -3970625992595036066L;

    private String displayName;
}
