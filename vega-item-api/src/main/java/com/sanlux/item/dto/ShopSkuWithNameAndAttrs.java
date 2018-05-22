package com.sanlux.item.dto;

import com.sanlux.item.model.ShopSku;
import io.terminus.parana.attribute.dto.SkuAttribute;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/11/14
 */
@Data
public class ShopSkuWithNameAndAttrs  implements Serializable {

    private static final long serialVersionUID = -5608020020719152478L;

    private ShopSku shopSku;

    private String name;

    private List<SkuAttribute> attrs;
}
