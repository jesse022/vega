package com.sanlux.search;

import com.google.common.collect.Lists;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.item.impl.dao.ShopSkuDao;
import com.sanlux.item.model.ShopSku;
import io.terminus.parana.attribute.dto.GroupedSkuAttribute;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.BrandCacher;
import io.terminus.parana.cache.CategoryAttributeCacher;
import io.terminus.parana.category.impl.dao.ShopCategoryItemDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ItemAttribute;
import io.terminus.parana.search.item.impl.BaseIndexedItemFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by cuiwentao
 * on 16/8/29
 */

@Component
public class VegaIndexedItemFactory extends BaseIndexedItemFactory<VegaIndexdItem> {

    private final ShopSkuDao shopSkuDao;

    @Autowired
    public VegaIndexedItemFactory(BackCategoryCacher backCategoryCacher,
                                  ShopCategoryItemDao shopCategoryItemDao,
                                  BrandCacher brandCacher,
                                  ShopSkuDao shopSkuDao,
                                  CategoryAttributeCacher categoryAttributeCacher) {
        super(backCategoryCacher, shopCategoryItemDao, brandCacher,categoryAttributeCacher);
        this.shopSkuDao = shopSkuDao;
    }

    @Override
    public VegaIndexdItem create(Item item, ItemAttribute itemAttribute, Object... others) {

        VegaIndexdItem indexedItem = super.create(item, itemAttribute, others);

        List<Integer> shopSkuPrices = findShopSkuPrices(DefaultId.PLATFROM_SHOP_ID, item.getId());
        if (!CollectionUtils.isEmpty(shopSkuPrices)) {
            indexedItem.setPrice(Collections.min(shopSkuPrices));
        }

        // 搜索引擎全部保存销售属性,去掉原先的其他属性
        List<GroupedSkuAttribute> skuAttrs = itemAttribute.getSkuAttrs();
        if(!CollectionUtils.isEmpty(skuAttrs)) {
            List<String> attributes = Lists.newArrayList();
            for (GroupedSkuAttribute groupedSkuAttribute : skuAttrs) {
                List<SkuAttribute> skuAttributes = groupedSkuAttribute.getSkuAttributes();
                for (SkuAttribute skuAttribute :skuAttributes ) {
                    attributes.add(skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal());
                }

            }
            indexedItem.setAttributes(attributes);
        } else {
            indexedItem.setAttributes(Collections.emptyList());
        }

        String itemName = indexedItem.getName();
        indexedItem.setDisplayName(itemName);
        indexedItem.setName(itemName.toLowerCase());
        return indexedItem;
    }

    private List<Integer> findShopSkuPrices(Long shopId, Long itemId) {

        List<ShopSku> shopSkus = shopSkuDao.findByShopIdAndItemId(shopId,itemId);
        return Lists.transform(shopSkus, ShopSku::getPrice);
    }


}
