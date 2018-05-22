package com.sanlux.item.impl.manager;

import com.google.common.collect.Lists;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.impl.dao.ItemDetailExtDao;
import com.sanlux.item.impl.dao.ItemExtDao;
import com.sanlux.item.impl.dao.SkuExtDao;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.category.impl.dao.ShopCategoryItemDao;
import io.terminus.parana.delivery.impl.dao.ItemDeliveryFeeDao;
import io.terminus.parana.delivery.model.ItemDeliveryFee;
import io.terminus.parana.item.dto.FullItem;
import io.terminus.parana.item.dto.ImageInfo;
import io.terminus.parana.item.impl.dao.ItemAttributeDao;
import io.terminus.parana.item.impl.dao.ItemDao;
import io.terminus.parana.item.impl.dao.ItemDetailDao;
import io.terminus.parana.item.impl.dao.SkuDao;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.ItemAttribute;
import io.terminus.parana.item.model.ItemDetail;
import io.terminus.parana.item.model.Sku;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Created by cuiwentao
 * on 16/10/19
 */
@Slf4j
@Component
public class VegaItemManager {

    private final ItemDao itemDao;

    private final SkuDao skuDao;

    private final ItemDetailDao itemDetailDao;

    private final ItemDeliveryFeeDao itemDeliveryFeeDao;

    private final ItemAttributeDao itemAttributeDao;

    private final ItemExtDao itemExtDao;

    private final ItemDetailExtDao itemDetailExtDao;

    private final SkuExtDao skuExtDao;

    private  final ShopCategoryItemDao shopCategoryItemDao;


    @Autowired
    public VegaItemManager(ItemDao itemDao,
                           SkuDao skuDao,
                           ItemDetailDao itemDetailDao,
                           ItemDeliveryFeeDao itemDeliveryFeeDao,
                           ItemAttributeDao itemAttributeDao,
                           ItemExtDao itemExtDao,
                           ItemDetailExtDao itemDetailExtDao,
                           SkuExtDao skuExtDao,
                           ShopCategoryItemDao shopCategoryItemDao) {
        this.itemDao = itemDao;
        this.skuDao = skuDao;
        this.itemDetailDao = itemDetailDao;
        this.itemDeliveryFeeDao = itemDeliveryFeeDao;
        this.itemAttributeDao = itemAttributeDao;
        this.itemExtDao = itemExtDao;
        this.itemDetailExtDao = itemDetailExtDao;
        this.skuExtDao = skuExtDao;
        this.shopCategoryItemDao = shopCategoryItemDao;
    }

    @Transactional
    public Boolean bulkCreateItem(List<FullItem> fullItems) {
        for (FullItem fullItem : fullItems) {

            Item item = fullItem.getItem();
            itemDao.create(item);
            long itemId = item.getId();

            ItemDetail itemDetail = fullItem.getItemDetail();
            itemDetail.setItemId(itemId);
            itemDetailDao.create(itemDetail);

            ItemDeliveryFee itemDeliveryFee = fullItem.getItemDeliveryFee();
            itemDeliveryFee.setItemId(itemId);
            itemDeliveryFeeDao.create(itemDeliveryFee);

            ItemAttribute itemAttribute = new ItemAttribute();
            itemAttribute.setOtherAttrs(CollectionUtils.isEmpty(fullItem.getGroupedOtherAttributes()) ? null : fullItem.getGroupedOtherAttributes());
            itemAttribute.setSkuAttrs(fullItem.getGroupedSkuAttributes());
            itemAttribute.setItemId(itemId);
            itemAttributeDao.create(itemAttribute);

            for (Sku sku : fullItem.getSkus()) {
                sku.setItemId(itemId);
                skuDao.create(sku);
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 土猫网商品通过API导入需要进行重复性判断(一个item每次只包含一个sku)
     * 1.同一个item下的sku不在同一个批次导入
     * 2.导入数据本身也可能重复
     * @param fullItems  导入商品信息
     * @return 是否成功
     */
    @Transactional
    public Boolean toolMallItemBatchCreateAndUpdateOrDelete(List<FullItem> fullItems) {
        for (FullItem fullItem : fullItems) {

            Item item = fullItem.getItem();

            List<Item> exitItems = itemDao.findByItemCode(item.getItemCode());
            if (Arguments.isNullOrEmpty(exitItems)) {
                // 新增
                itemDao.create(item);
                long itemId = item.getId();

                ItemDetail itemDetail = fullItem.getItemDetail();
                itemDetail.setItemId(itemId);
                itemDetailDao.create(itemDetail);

                ItemDeliveryFee itemDeliveryFee = fullItem.getItemDeliveryFee();
                itemDeliveryFee.setItemId(itemId);
                itemDeliveryFeeDao.create(itemDeliveryFee);

                ItemAttribute itemAttribute = new ItemAttribute();
                itemAttribute.setOtherAttrs(CollectionUtils.isEmpty(fullItem.getGroupedOtherAttributes()) ? null : fullItem.getGroupedOtherAttributes());
                itemAttribute.setSkuAttrs(fullItem.getGroupedSkuAttributes());
                itemAttribute.setItemId(itemId);
                itemAttributeDao.create(itemAttribute);

                for (Sku sku : fullItem.getSkus()) {
                    sku.setItemId(itemId);
                    skuDao.create(sku);
                }
            } else {
                // 修改

                Boolean isFreeze = Boolean.FALSE;
                Boolean isUpdatePrice = Boolean.FALSE;


                Long itemId = exitItems.get(0).getId();
                List<Sku> firstSkus = skuDao.findByItemId(itemId);

                List<SkuAttribute> existSkuAttribute = Lists.newArrayList();
                firstSkus.stream().forEach(sku1 -> existSkuAttribute.addAll(sku1.getAttrs()));
                for (Sku sku : fullItem.getSkus()) {
                    List<Sku> exitSkus = skuDao.findByShopIdAndSkuCode(exitItems.get(0).getShopId(), sku.getSkuCode());

                    List<String> skuSellAttributesKeyList =   Lists.transform(sku.getAttrs(), SkuAttribute::getAttrKey);
                    List<String> oldSkuSellAttributesKeyList =   Lists.transform(firstSkus.get(0).getAttrs(), SkuAttribute::getAttrKey);

                    // 销售属性不修改,业务上前提要求是SKU的销售属性值必须一致,但程序上目前没法进行验证,为确保数据正确,不一致时无法保存
                    if (!skuSellAttributesKeyList.containsAll(oldSkuSellAttributesKeyList)) {
                        // SKU销售属性键值不一致
                        return Boolean.FALSE;
                    }
                    if (existSkuAttribute.containsAll(sku.getAttrs())) {
                        // SKU销售属性已经存在,不能重复创建
                        return Boolean.FALSE;
                    }

                    if (Arguments.isNullOrEmpty(exitSkus)) {
                        sku.setItemId(itemId);
                        skuDao.create(sku);
                    } else {
                        sku.setId(exitSkus.get(0).getId());

                        if (Objects.equals(sku.getStatus(), DefaultItemStatus.ITEM_FREEZE)) {
                            skuDao.updateStatusBySkuId(exitSkus.get(0).getId(), DefaultItemStatus.ITEM_FREEZE);
                            isFreeze = Boolean.TRUE;
                        } else {
                            if (Objects.equals(exitSkus.get(0).getPrice(), sku.getPrice())) {
                                // 价格没有变化,状态还是采用数据库中的状态
                                sku.setStatus(exitSkus.get(0).getStatus());
                                isUpdatePrice = Boolean.TRUE;
                            }
                            skuDao.update(sku);
                        }
                    }
                }


                List<Sku> skuList = Lists.newArrayList();
                List<Sku> exitSkusByItem = skuDao.findByItemId(itemId);
                List<Sku> newSkus = fullItem.getSkus();

                skuList.addAll(exitSkusByItem);
                List<String> newSkuCodes = Lists.transform(newSkus, Sku::getSkuCode);
                skuList.removeIf(sku1 -> newSkuCodes.contains(sku1.getSkuCode()));
                skuList.addAll(newSkus);
                List<Integer> skuPrices = Lists.transform(skuList, Sku::getPrice);


                item.setId(itemId);
                item.setLowPrice(Collections.min(skuPrices));
                item.setHighPrice(Collections.max(skuPrices));

                if (!isUpdatePrice) {
                    // 未修改价格时用数据库中的状态
                    item.setStatus(exitItems.get(0).getStatus());
                }
                if (isFreeze) {
                    // 只要有一个sku冻结,整个商品就变成冻结状态(有人为进行确认)
                    item.setStatus(DefaultItemStatus.ITEM_FREEZE);
                }

                itemDao.update(item);

                ItemDetail exitItemDetail = itemDetailDao.findByItemId(itemId);
                ItemDetail itemDetail = fullItem.getItemDetail();

                List<ImageInfo> exitImagesList = exitItemDetail.getImages();
                List<ImageInfo> newImagesList = itemDetail.getImages();
                List<ImageInfo> imageInfos = Lists.newArrayList();
                imageInfos.addAll(exitImagesList);
                imageInfos.addAll(newImagesList);

                itemDetail.setDetail(exitItemDetail.getDetail().concat(itemDetail.getDetail())); // 详情叠加
                itemDetail.setImages(imageInfos); // 图片叠加
                itemDetail.setItemId(itemId);
                itemDetailDao.update(itemDetail);

                // 运费模板不修改
            }
        }
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean bulkUpdateItem(List<FullItem> fullItems) {
        for (FullItem fullItem : fullItems) {

            Item item = fullItem.getItem();
            itemDao.update(item);

            ItemDetail itemDetail = fullItem.getItemDetail();
            itemDetailDao.update(itemDetail);

            for (Sku sku : fullItem.getSkus()) {
                skuDao.update(sku);
            }
        }
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean batchUpdateRichText (List<Item> items, List<ItemDetail> itemDetails){
        itemExtDao.batchUpdateItemInfoMd5(items);
        itemDetailExtDao.batchUpdateItemDetail(itemDetails);
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean batchUpdateStockQuantity(List<Item> items, List<Sku> skus) {
        skuExtDao.batchUpdateStockByShopIdAndId(skus);
        itemExtDao.batchUpdateStockByShopIdAndId(items);

        return Boolean.TRUE;
    }


    @Transactional
    public void decreaseBy(Long skuId,Integer delta) {
        Sku sku = skuDao.findById(skuId);
        if (sku == null) {
            log.error("sku(id={}) not found", skuId);
            throw new ServiceException("sku.not.found");
        }
        final Long itemId = sku.getItemId();
        Item item = itemDao.findById(itemId);

        //检查是否有库存
        checkStockIfEnough(item, sku, delta);

        skuDao.updateStockQuantity(skuId, delta);

        Item update = new Item();
        update.setId(itemId);
        update.setStockQuantity(item.getStockQuantity()-delta);
        itemDao.update(update);

        //如果商品处于上架状态则检查是否需要下架该商品 // TODO: 2017/6/20 暂定 库存<=0时不下架商品
    }

    @Transactional
    public Boolean batchUpdateSellerPrice(List<FullItem> fullItems) {
        for(FullItem fullItem : fullItems) {
            itemExtDao.updateSellerPriceById(fullItem.getItem());
            List<Sku> skus = fullItem.getSkus();
            for(Sku sku : skus){
                skuExtDao.updateSellerPriceById(sku);
            }
        }
        return Boolean.TRUE;
    }

    private void checkStockIfEnough(Item item, Sku sku, Integer delta) {
        if (item.getStockQuantity() - delta < 0) {
            log.error("stock quantity not enough where item id={},expect quantity:{},but actual quantity:{}",
                    item.getId(), delta, item.getStockQuantity());
            throw new ServiceException("item.stock.quantity.not.enough");
        }
        if (sku.getStockQuantity() - delta < 0) {
            log.error("stock quantity not enough where sku id={},expect quantity:{},but actual quantity:{}",
                    sku.getId(), delta, sku.getStockQuantity());
            throw new ServiceException("sku.stock.quantity.not.enough");
        }
    }

    @Transactional
    public Boolean batchDeleteItemsByShopId(Long shopId, List<Long> itemIds) {
        //修改商品表状态为删除状态
        boolean success = itemDao.batchUpdateStatusByShopIdAndIds(shopId, itemIds, DefaultItemStatus.ITEM_DELETE);
        if (!success) {
            log.error("failed to update items(ids={}) to status({})", itemIds, DefaultItemStatus.ITEM_DELETE);
            throw new ServiceException("item.status.update.fail");
        } else {
            skuExtDao.batchDeleteByItemIds(itemIds);
            //删除店铺商品
            shopCategoryItemDao.deleteByShopIdAndItemIds(shopId, itemIds);
        }
        return Boolean.TRUE;
    }

    @Transactional
    public Boolean batchDeleteItemsAndSkusByShopId(Long shopId, List<Long> itemIds, List<Long> skuIds) {
        //修改商品表状态为删除状态
        boolean success = itemDao.batchUpdateStatusByShopIdAndIds(shopId, itemIds, DefaultItemStatus.ITEM_DELETE);
        if (!success) {
            log.error("failed to update items(ids={}) to status({})", itemIds, DefaultItemStatus.ITEM_DELETE);
            throw new ServiceException("item.status.update.fail");
        } else {
            for (Long skuId : skuIds) {
                skuDao.updateStatusBySkuId(skuId, DefaultItemStatus.ITEM_DELETE);
            }
            //删除店铺商品
            shopCategoryItemDao.deleteByShopIdAndItemIds(shopId, itemIds);
        }
        return Boolean.TRUE;
    }

}
