package com.sanlux.shop.dto;

import io.terminus.parana.shop.model.Shop;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by cuiwentao
 * on 16/11/2
 */
@Data
public class VegaShopWithItemStock implements Serializable {

    private static final long serialVersionUID = -12091169904513344L;

    private Shop shop;

    private Integer stockQuantity;
}
