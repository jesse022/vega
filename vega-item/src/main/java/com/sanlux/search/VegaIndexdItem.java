package com.sanlux.search;

import io.terminus.parana.search.dto.IndexedItem;
import lombok.Data;

/**
 * Created by jesselu on 2017/2/20.
 */
@Data
public class VegaIndexdItem extends IndexedItem {

    private static final long serialVersionUID = -5437239089644221859L;

    private String displayName;
}
