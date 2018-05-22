package com.sanlux.item.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.item.model.Sku;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 店铺sku
 * Author:cp
 * Created on 8/2/16
 */
@Data
public class ShopSku implements Serializable {

    private static final long serialVersionUID = 8135468522100197893L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    /**
     * ID
     */
    private Long id;

    /**
     * 店铺id
     */
    private Long shopId;

    /**
     * 类目ID
     */
    private Long categoryId;

    /**
     * 商品id
     */
    private Long itemId;

    /**
     * sku id
     */
    private Long skuId;

    /**
     * sku状态,1:上架, -1:下架, -2:冻结, -3:删除
     */
    @Getter
    @Setter
    private Integer status;

    /**
     * 实际售卖价格
     */
    private Integer price;

    /**
     * 其他各种价格, 如市场价, 阶梯价等的json表示形式, 存数据库
     */
    @Getter
    private String extraPriceJson;


    /**
     * 其他各种价格, 如市场价, 阶梯价等, 不存数据库
     */
    @Getter
    private Map<String, Integer> extraPrice;

    /**
     * 库存类型, 0: 不分仓存储, 1: 分仓存储
     */
    private Integer stockType;

    /**
     * 库存
     */
    private Integer stockQuantity;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setExtraPriceJson(String extraPriceJson) throws Exception {
        this.extraPriceJson = extraPriceJson;
        if (Strings.isNullOrEmpty(extraPriceJson)) {
            this.extraPrice = Collections.emptyMap();
        } else {
            this.extraPrice = objectMapper.readValue(extraPriceJson, JacksonType.MAP_OF_INTEGER);
        }
    }

    public void setExtraPrice(Map<String, Integer> extraPrice) throws Exception {
        this.extraPrice = extraPrice;
        if (extraPrice == null) {
            this.extraPriceJson = null;
        } else {
            this.extraPriceJson = objectMapper.writeValueAsString(extraPrice);
        }
    }

    public static ShopSku from(Sku sku) throws Exception {
        ShopSku shopSku = new ShopSku();
        shopSku.setShopId(sku.getShopId());
        shopSku.setItemId(sku.getItemId());
        shopSku.setSkuId(sku.getId());
        shopSku.setPrice(sku.getPrice());
        shopSku.setStatus(sku.getStatus());
        shopSku.setExtraPrice(sku.getExtraPrice());
        shopSku.setStockType(sku.getStockType());
        shopSku.setStockQuantity(sku.getStockQuantity());
        return shopSku;
    }
}
