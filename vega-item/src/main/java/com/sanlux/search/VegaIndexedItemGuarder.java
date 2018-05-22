package com.sanlux.search;

import com.sanlux.common.constants.DefaultItemStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.search.item.impl.DefaultIndexedItemGuarder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
@Component
public class VegaIndexedItemGuarder extends DefaultIndexedItemGuarder {

    @Override
    public boolean indexable(Item item) {
        return !Objects.equals(item.getStatus(), DefaultItemStatus.ITEM_DELETE);
    }
}
