package com.sanlux.web.front.controller.item;

import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.*;
import com.google.common.eventbus.EventBus;
import com.sanlux.category.service.VegaFrontCategoryReaderService;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.shop.enums.VegaShopStatus;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiEvent;
import io.terminus.common.utils.Joiners;
import io.terminus.parana.cache.ItemCacher;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.delivery.model.DeliveryFeeTemplate;
import io.terminus.parana.delivery.model.ItemDeliveryFee;
import io.terminus.parana.delivery.service.DeliveryFeeReadService;
import io.terminus.parana.rule.RuleEngine;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.web.core.events.item.ItemUpdateEvent;
import org.apache.commons.lang.StringUtils;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.common.enums.OrderUserType;
import com.sanlux.common.helper.UserTypeHelper;
import com.sanlux.common.utils.ListSum;
import com.sanlux.item.dto.RichShopItem;
import com.sanlux.item.dto.RichShopSku;
import com.sanlux.item.dto.ShopItemDeliveryFeeTemplate;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopItemDeliveryFee;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.*;
import com.sanlux.search.dto.VegaSearchedItem;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.web.front.component.item.ReceiveShopParser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.category.model.FrontCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import io.terminus.parana.category.service.CategoryBindingReadService;
import io.terminus.parana.category.service.FrontCategoryReadService;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.dto.FullItem;
import io.terminus.parana.item.dto.ViewedItem;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.ItemWriteService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.search.dto.SearchedItem;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.search.item.ItemSearchReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.text.Collator;
import java.util.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sanlux.common.helper.UserRoleHelper.getUserRoleName;

/**
 * Author:cp
 * Created on 8/10/16.
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class VegaItems {

    @RpcConsumer
    private ShopItemReadService shopItemReadService;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private ShopItemWriteService shopItemWriteService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private ItemWriteService itemWriteService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopItemDeliveryFeeWriteService shopItemDeliveryFeeWriteService;

    @RpcConsumer
    private VegaDeliveryFeeTemplateReadService vegaDeliveryFeeTemplateReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private VegaItemReadService vegaItemReadService;

    @RpcConsumer
    private VegaItemWriteService vegaItemWriteService;

    @Autowired
    private ReceiveShopParser receiveShopParser;

    @RpcConsumer
    private ItemSearchReadService itemSearchReadService;

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private CategoryBindingReadService categoryBindingReadService;

    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;

    @RpcConsumer
    private FrontCategoryReadService frontCategoryReadService;

    @RpcConsumer
    private VegaFrontCategoryReaderService vegaFrontCategoryReaderService;

    @RpcConsumer
    private VegaFrontCategoriesCacherService vegaFrontCategoriesCacherService;

    @RpcConsumer
    private ItemCacher itemCacher;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private DeliveryFeeReadService deliveryFeeReadService;

    @RpcConsumer
    private RuleEngine ruleEngine;


    public VegaItems() {
    }


    @RequestMapping(value = "/shop-item", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean createShopItems(@RequestParam("itemIds") Long[] itemIds) throws Exception {
        List<Long> itemIdList = Arrays.asList(itemIds);
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();

        List<Long> existed = findShopItemsIfExisted(itemIdList, shopId);

        List<Long> notExistedItemIds = Lists.newArrayList();
        for (Long itemId : itemIdList) {
            if (!existed.contains(itemId)) {
                notExistedItemIds.add(itemId);
            }
        }
        if (CollectionUtils.isEmpty(notExistedItemIds)) {
            log.error("all items have already add to stock manager, itemIds:{}", itemIdList);
            throw new JsonResponseException("all.items.have.already.add.to.stock.manager");
        }

        List<Item> items = findItems(notExistedItemIds);
        Multimap<Long, Sku> skusByItemIdIndex = findSkusByItems(items);

        for (Item item : items) {
            ShopItem shopItem = makeShopItem(shopId, item);
            List<ShopSku> shopSkus = makeShopSkus(shopId, skusByItemIdIndex.get(item.getId()));

            RichShopItem richShopItem = new RichShopItem();
            richShopItem.setShopItem(shopItem);
            richShopItem.setShopSkus(shopSkus);
            Response<Long> createResp = shopItemWriteService.create(richShopItem);
            if (!createResp.isSuccess()) {
                log.error("fail to create richShopItem:{},cause:{}",
                        richShopItem, createResp.getError());
                throw new JsonResponseException(createResp.getError());
            }
        }

        return true;
    }

    /**
     * 通过itemId获取前台类目
     * @param itemId
     * @return 前台类目树
     */
    @RequestMapping(value = "/findFrontCategoriesByItem/{itemId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<FrontCategory> findCategoriesByItem(@PathVariable("itemId") Long itemId) {

        Response<Item> itemResponse = itemReadService.findById(itemId);
        if (!itemResponse.isSuccess()) {
            log.error("fail to find item detail by itemId:{},cause:{}", itemId, itemResponse.getError());
            throw new JsonResponseException(itemResponse.getError());
        }
        Long backCategoryId = itemResponse.getResult().getCategoryId();
        Response<List<FrontCategory>> findResp = categoryBindingReadService.findByBackCategoryId(backCategoryId);
        if (!findResp.isSuccess()) {
            log.error("fail to find frontCategory by backCategory={}", backCategoryId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        Long frontCategoryId=findResp.getResult().get(0).getId();
        Response<List<FrontCategory>> findRespList=vegaFrontCategoriesCacherService.findAncestorsFromCatch(frontCategoryId);
        if (!findRespList.isSuccess()) {
            log.error("fail to find frontCategories by frontCategoryId={}", frontCategoryId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findRespList.getResult();

    }



    @RequestMapping(value = "/shop-sku/{skuId}/stock", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateStockQuantity(@PathVariable("skuId") Long skuId,
                                       @RequestParam("delta") Integer delta) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        Response<Boolean> updateResp = shopSkuWriteService.updateStockQuantity(paranaUser.getShopId(), skuId, delta);
        if (!updateResp.isSuccess()) {
            log.error("fail to update stock quantity,shopId={},skuId={},delta={},cause:{}",
                    paranaUser.getShopId(), skuId, delta, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }

    @RequestMapping(value = "/shop-item/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ShopItem> findByShopId(@RequestParam(value = "itemId", required = false) Long itemId,
                                         @RequestParam(value = "itemName", required = false) String itemName,
                                         @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                         @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        Response<Paging<ShopItem>> findResp = shopItemReadService.findBy(shopId, itemId, itemName, pageNo, pageSize);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop items by shopId={},itemId={},itemName={},pageNo={},pageSize={},cause:{}",
                    shopId, itemId, itemName, pageNo, pageSize, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    @RequestMapping(value = "/shop-item/{itemId}/skus", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<RichShopSku> findShopSkuDetail(@PathVariable("itemId") Long itemId) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        Response<List<RichShopSku>> findResp = shopSkuReadService.findShopSkuDetail(shopId, itemId);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop sku detail by shopId={},itemId={},cause:{}",
                    shopId, itemId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    @RequestMapping(value = "/shop-item/{itemId}/delivery-fee", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOrUpdateShopItemDeliveryFee(@PathVariable("itemId") Long itemId,
                                                     @RequestBody ShopItemDeliveryFee shopItemDeliveryFee) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        shopItemDeliveryFee.setShopId(shopId);
        shopItemDeliveryFee.setItemId(itemId);

        Response<Boolean> createOrUpdateResp = shopItemDeliveryFeeWriteService.createOrUpdateShopItemDeliveryFee(shopItemDeliveryFee);
        if (!createOrUpdateResp.isSuccess()) {
            log.error("fail to create or update item shop delivery fee:{},cause:{}",
                    shopItemDeliveryFee, createOrUpdateResp.getError());
            throw new JsonResponseException(createOrUpdateResp.getError());
        }
        return createOrUpdateResp.getResult();
    }

    @RequestMapping(value = "/shop-item/{itemId}/delivery-fee-template", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShopItemDeliveryFeeTemplate findShopItemDeliveryFeeTemplate(@PathVariable("itemId") Long itemId) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();

        Response<ShopItemDeliveryFeeTemplate> findResp = vegaDeliveryFeeTemplateReadService.findShopItemDeliveryFeeTemplate(shopId, itemId);
        if (!findResp.isSuccess()) {
            log.error("fail to find shop item delivery fee template where shopId={},itemId={},cause:{}",
                    shopId, itemId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    @RequestMapping(value = "/shop-sku", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean deleteShopSku(@RequestParam(value = "shopSkuId") Long shopSkuId) {
        Response<Boolean> response = shopSkuWriteService.deleteById(shopSkuId);
        if (!response.isSuccess()) {
            log.error("delete shop sku by id fail,shopSkuId:{},cause:{}", shopSkuId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/shop-sku", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean addShopSkus(@RequestParam("skuIds") Long[] skuIds) throws Exception {
        List<Long> skuIdList = Arrays.asList(skuIds);
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        List<Long> existSkuIds = findExistSkuIds(shopId, skuIdList);

        List<Long> notExistedSkuIdList = Lists.newArrayList();
        for (Long skuId : skuIdList) {
            if (!existSkuIds.contains(skuId)) {
                notExistedSkuIdList.add(skuId);
            }
        }

        Response<List<Sku>> skusResp = skuReadService.findSkusByIds(notExistedSkuIdList);
        if (!skusResp.isSuccess()) {
            log.error("find skus by ids fail, shopId:{}, skuIdList:{}, cause:{}",
                    shopId, notExistedSkuIdList, skusResp.getError());
            throw new JsonResponseException(skusResp.getError());
        }
        List<Sku> skus = skusResp.getResult();
        List<ShopSku> shopSkuList = makeShopSkus(shopId, skus);

        for (ShopSku shopSku : shopSkuList) {
            Response<Long> response = shopSkuWriteService.create(shopSku);
            if (!response.isSuccess()) {
                log.error("create shop sku fail, shopId:{}, skuId:{}, cause:{}",
                        shopId, shopSku.getSkuId(), response.getError());
                throw new JsonResponseException(response.getError());
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 供应商批量修改供货价
     * 1.只能修改已冻结状态的商品价格
     * 2.修改完后商品状态变成待审核
     *
     * @param itemIds   商品Ids
     * @param times 价格倍率,控制到小数点6位
     * @return 是否成功
     */
    @RequestMapping(value = "/shop-item/batch/set-seller-prices", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> batchSetItemSellerSkuPrices(@RequestParam("itemIds") Long[] itemIds,
                                         @RequestParam("times") String times) {
        final Long shopId = UserUtil.<ParanaUser>getCurrentUser().getShopId();
        List<Long> itemIdList = Arrays.asList(itemIds);
        List<FullItem> fullItems = Lists.newArrayList();
        List<Long> notSuccesItemIds = Lists.newArrayList();
        int num = 0;
        for (Long itemId : itemIdList) {
            Response<List<Sku>> skusResp = skuReadService.findSkusByItemId(itemId);
            if (!skusResp.isSuccess()) {
                log.error("find skus by itemId:{} fail, cause:{}", itemId, skusResp.getError());
                num++;
                notSuccesItemIds.add(itemId);
                continue;
            }
            List<Sku> skuList = skusResp.getResult();
            List<Sku> skus = Lists.newArrayList();
            if (!Arguments.isNullOrEmpty(skuList)) {
                //过滤当前用户店铺和冻结状态的商品
                List<Sku> skuListFilter = FluentIterable.from(skuList).filter(sku -> Objects.equals(sku.getShopId(), shopId) &&
                        Objects.equals(sku.getStatus(), DefaultItemStatus.ITEM_FREEZE)).toList();
                if (Arguments.isNullOrEmpty(skuListFilter)) {
                    num ++;
                    notSuccesItemIds.add(itemId);
                    continue;
                }
                skuListFilter.stream().forEach(sku -> {
                    Sku skuNew = new Sku();
                    skuNew.setId(sku.getId());
                    skuNew.setPrice((int) (sku.getPrice() * Float.parseFloat(times)));
                    skuNew.setShopId(shopId);
                    skuNew.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);
                    skus.add(skuNew);
                });

                FullItem fullItem = new FullItem();
                Item item = new Item();
                item.setId(itemId);
                item.setShopId(shopId);
                item.setStatus(DefaultItemStatus.ITEM_WAIT_AUDIT);
                setItemPriceBySkus(item, skus);
                fullItem.setItem(item);
                fullItem.setSkus(skus);
                fullItems.add(fullItem);
            }
        }
        if(Arguments.isNullOrEmpty(fullItems)){
            log.error("fail to batch update item seller price (itemIds:{},times:{}), cause:{the item is not belong to this shop or item status is not freeze}",
                    itemIds, times);
            return Response.fail("only.freeze.and.me.item.can.batch.update.seller.price");
        }
        Response<Boolean> res = vegaItemWriteService.batchUpdateSellerPrice(fullItems);
        if(!res.isSuccess()){
            log.error("fail to batch update item seller price (itemIds:{},times:{}), cause:{}",
                    itemIds, times, res.getError());
            return Response.fail("batch.update.item.seller.price.fail");
        }
        if(num > 0){
            //部分成功
            throw new InvalidException(500, "{0}.batch.update.item.seller.price.partial.success", Joiners.COMMA.join(notSuccesItemIds));
        }
        return Response.ok(Boolean.TRUE);
    }


    private List<Long> findExistSkuIds(Long shopId, List<Long> skuIdList) {
        Response<List<ShopSku>> shopSkusResp = shopSkuReadService.findByShopIdAndSkuIds(shopId, skuIdList);
        if (!shopSkusResp.isSuccess()) {
            log.error("fail to find shop skus by shopId:{}, skuIdList:{},cause:{}",
                    shopId, skuIdList, shopSkusResp.getError());
            throw new JsonResponseException(shopSkusResp.getError());
        }
        List<ShopSku> shopSkus = shopSkusResp.getResult();
        if (!CollectionUtils.isEmpty(shopSkus)) {
            return Lists.transform(shopSkus, ShopSku::getSkuId);
        }
        return Collections.<Long>emptyList();
    }

    private List<Long> findShopItemsIfExisted(List<Long> itemIds, Long shopId) {
        Response<List<ShopItem>> findShopItems = shopItemReadService.findByShopIdAndItemIds(shopId, itemIds);
        if (!findShopItems.isSuccess()) {
            log.error("fail to find shop items by shopId={},itemIds={},cause:{}",
                    shopId, itemIds, findShopItems.getError());
            throw new JsonResponseException(findShopItems.getError());
        }
        List<ShopItem> shopItems = findShopItems.getResult();

        if (!CollectionUtils.isEmpty(shopItems)) {
            return Lists.transform(shopItems, ShopItem::getItemId);
        }
        return Collections.<Long>emptyList();
    }

    private List<Item> findItems(List<Long> itemIds) {
        Response<List<Item>> findItems = itemReadService.findByIds(itemIds);
        if (!findItems.isSuccess()) {
            log.error("fail to find items by itemIds={},cause:{}",
                    itemIds, findItems.getError());
            throw new JsonResponseException(findItems.getError());
        }
        List<Item> items = findItems.getResult();
        if (CollectionUtils.isEmpty(items)) {
            log.error("items not found for itemIds:{}", itemIds);
            throw new JsonResponseException("item.not.found");
        }
        return items;
    }

    private ImmutableListMultimap<Long, Sku> findSkusByItems(List<Item> items) {
        List<Long> itemIds = Lists.newArrayListWithCapacity(items.size());
        for (Item item : items) {
            itemIds.add(item.getId());
        }

        Response<List<Sku>> findSkus = skuReadService.findSkusByItemIds(itemIds);
        if (!findSkus.isSuccess()) {
            log.error("fail to find skus by itemIds:{},cause:{}",
                    itemIds, findSkus.getError());
            throw new JsonResponseException(findSkus.getError());
        }

        return Multimaps.index(findSkus.getResult(), Sku::getItemId);
    }

    private ShopItem makeShopItem(Long shopId, Item item) {
        ShopItem shopItem = new ShopItem();
        shopItem.setShopId(shopId);
        shopItem.setItemId(item.getId());
        shopItem.setItemName(item.getName());
        shopItem.setStatus(item.getStatus());
        return shopItem;
    }

    private List<ShopSku> makeShopSkus(Long shopId, Collection<Sku> skus) throws Exception {
        List<ShopSku> shopSkus = Lists.newArrayListWithCapacity(skus.size());
        for (Sku sku : skus) {

            ShopSku shopSku = ShopSku.from(sku);
            shopSku.setShopId(shopId);
            shopSku.setStockQuantity(0);
            shopSkus.add(shopSku);
        }
        return shopSkus;
    }

    /**
     * 根据商品ID随机获取相同类目下的推荐商品
     *
     * @param itemId 商品Id
     * @param limit  数量
     * @return
     */
    @RequestMapping(value = "/commend/item/{itemId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<Item>> getCommendItem(@PathVariable("itemId") Long itemId,
                                               @RequestParam(value = "limit", required = false) Integer limit){
        Item item = itemCacher.findItemById(itemId);
        if (Arguments.isNull(item)) {
            log.error("fail to find item detail by itemId:{},cause:{}", itemId);
            return Response.fail("item.find.fail");
        }
        Long backCategoryId = item.getCategoryId();

        Response<List<Item>> listResponse = vegaItemReadService.randFindItemsByCategoryId(backCategoryId, Arguments.isNull(limit) ? 5 : limit);
        if (! listResponse.isSuccess()) {
            log.error("fail to find commend item by itemId:{}, cause:{}", itemId, listResponse.getError());
            throw new JsonResponseException(listResponse.getError());
        }
        return Response.ok(listResponse.getResult());
    }


    /**
     * 商品详情页返回信息,需根据商品ID,和收货地址区ID获取
     * @param itemId 商品ID
     * @return 商品详情页信息
     */
    @RequestMapping(value = "/item-detail/{itemId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ViewedItem rebuildItemDetail(@PathVariable("itemId") Long itemId,
                                        @RequestParam(value = "regionId", required = false) Integer regionId) {

        ParanaUser user = UserUtil.getCurrentUser();

        //parana商品详情页服务
        ViewedItem viewedItem = findForView(itemId);

        //重新组装一把价格库存
        regionId = regionId == null ? DefaultId.DEFAULT_REGION_ID : regionId;
        Long itemOwnerShopId = user == null ? DefaultId.PLATFROM_SHOP_ID : findItemOwnerShopId(itemId, regionId, user);
        OrderUserType orderUserType = user == null ? OrderUserType.NORMAL_USER : UserTypeHelper.getOrderUserTypeByUser(user);
        Long userId = user == null ?  DefaultId.NOT_LOG_IN_USER_ID : user.getId();

        List<Sku> skuList = viewedItem.getSkus();
        if (Arguments.isNull(skuList)) {
            skuList = Lists.newArrayList();
        }
        List<Long> deleteSkuId = Lists.newArrayList();
        skuList.forEach(sku -> {
            ShopSku shopSku = findShopSkuByShopIdAndSkuId(DefaultId.PLATFROM_SHOP_ID, sku.getId());
            if (shopSku == null) {
                log.error("platform shop sku find empty, skuId:{} ", sku.getId());
                if (Objects.equals(viewedItem.getItem().getStatus(), DefaultItemStatus.ITEM_WAIT_AUDIT)) {
                    // 商品状态为待审核时不抛异常
                    deleteSkuId.add(sku.getId());
                } else {
                    throw new JsonResponseException("platform.shop.sku.find.empty");
                }
            }
            if (!Arguments.isNull(shopSku)) {
                Map<String, String> extra = new HashMap<>();
                extra.put("originPrice", shopSku.getPrice().toString());
                sku.setExtra(extra);
                sku.setPrice(findSkuPrice(sku.getId(), itemOwnerShopId, userId, orderUserType));
                sku.setStockQuantity(findStockQuantity(itemOwnerShopId, sku.getId()));
            }
        });

        skuList.removeIf(sku -> deleteSkuId.contains(sku.getId()));


        List<Integer> skuPrices = Lists.transform(skuList, Sku::getPrice);
        if (!CollectionUtils.isEmpty(skuPrices)) {
            List<Integer> originPrices = findShopSkuPrices(DefaultId.PLATFROM_SHOP_ID, itemId, skuList);
            Map<String, String> extra = new HashMap<>();
            extra.put("originLowPrice", Collections.min(originPrices).toString());
            extra.put("originHighPrice", Collections.max(originPrices).toString());
            viewedItem.getItem().setExtra(extra);
            viewedItem.getItem().setLowPrice(Collections.min(skuPrices));
            viewedItem.getItem().setHighPrice(Collections.max(skuPrices));
        }
        List<Integer> stockQuantities = Lists.transform(skuList, Sku::getStockQuantity);
        if (!CollectionUtils.isEmpty(stockQuantities)) {
            viewedItem.getItem().setStockQuantity(ListSum.listSum(stockQuantities));
        }

        log.warn("item detail operate log, itemId:{}, regionId:{}, sellerShopId:{}", itemId, regionId, itemOwnerShopId);

        return viewedItem;
    }


    private Integer findStockQuantity(Long shopId, Long skuId) {

        if (Objects.equals(shopId, DefaultId.PLATFROM_SHOP_ID)) {
            Response<Sku> skuResp = skuReadService.findSkuById(skuId);
            if (!skuResp.isSuccess()) {
                log.error("find sku stock quantity fail, shopId:{}, skuId:{}, cause:{}",
                        shopId, skuId, skuResp.getError());
                throw new JsonResponseException(skuResp.getError());
            }
            return skuResp.getResult().getStockQuantity();
        }

        ShopSku shopSku = findShopSkuByShopIdAndSkuId(shopId, skuId);
        if (Arguments.isNull(shopSku)) {
            return 0;
        }
        return shopSku.getStockQuantity();
    }

    private ShopSku findShopSkuByShopIdAndSkuId(Long shopId, Long skuId) {

        Response<Optional<ShopSku>> shopSkuResponse = shopSkuReadService.findByShopIdAndSkuId(shopId, skuId);
        if (!shopSkuResponse.isSuccess()) {
            log.error("find shop sku fail, shopId:{}, skuId:{}, cause:{}",
                    shopId, skuId, shopSkuResponse.getError());
            throw new JsonResponseException(shopSkuResponse.getError());
        }
        if (!shopSkuResponse.getResult().isPresent()) {
            return null;
        }
        return shopSkuResponse.getResult().get();
    }

    private Integer findSkuPrice(Long skuId, Long sellerShopId, Long userId, OrderUserType orderUserType) {
        Response<Integer> priceResp = receiveShopParser.findSkuPrice(skuId, sellerShopId, userId, orderUserType);
        if (!priceResp.isSuccess()) {
            log.error("find sku price fail, skuId:{}, sellerShopId:{}, userId:{}, orderUserType:{},cause:{}",
                    skuId, sellerShopId, userId, orderUserType, priceResp.getError());
            throw new JsonResponseException(priceResp.getError());
        }
        return priceResp.getResult();
    }

    private Long findItemOwnerShopId(Long itemId, Integer regionId, ParanaUser user) {
        Response<Long> itemOwnerShopIdResp =
                receiveShopParser.findShopIdForItemDetail(itemId, regionId, user);
        if (!itemOwnerShopIdResp.isSuccess()) {
            log.error("find shopId for item detail fail, itemId:{}, regionId:{}, user:{}, cause:{}",
                    itemId ,regionId, user, itemOwnerShopIdResp.getError());
            throw new JsonResponseException(itemOwnerShopIdResp.getError());
        }
        return itemOwnerShopIdResp.getResult();
    }

    private ViewedItem findForView(Long itemId) {
        Response<ViewedItem> viewedItemResp = itemReadService.findForView(itemId);
        if (!viewedItemResp.isSuccess()) {
            log.error("fail to find item detail by itemId:{},cause:{}", itemId, viewedItemResp.getError());
            throw new JsonResponseException(viewedItemResp.getError());
        }
        return viewedItemResp.getResult();
    }

    @RequestMapping(value = "/item-to-add-purchase-order", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<FullItem> finItemsToAddPurchaseOrder(@RequestParam Boolean isItemName, @RequestParam String itemNameOrCode,
                                                       @RequestParam Integer pageNo, @RequestParam Integer pageSize) {
      String itemCode = null;
        List<Item> items = Lists.newArrayList();
        Long itemSize = 0L;

        if (isItemName) {
            Map<String, String> map = new HashMap<>();
            map.put("q", itemNameOrCode);
            map.put("status", String.valueOf(DefaultItemStatus.ITEM_ONSHELF));
            Response<? extends SearchedItemWithAggs<SearchedItem>> searchResp =
                    itemSearchReadService.searchWithAggs(pageNo, pageSize, "search.mustache", map, SearchedItem.class);
            if (!searchResp.isSuccess()) {
                log.error("find item through search fail, q:{}, cause:{}", map.get("q"), searchResp.getError());
                throw new JsonResponseException(searchResp.getError());
            }
            Paging<SearchedItem> searchedItemPaging = searchResp.getResult().getEntities();
            List<Long> itemIds = Lists.transform(searchedItemPaging.getData(), SearchedItem::getId);
            Response<List<Item>> itemResp = itemReadService.findByIds(itemIds);
            if (!itemResp.isSuccess()) {
                log.error("find item by ids:{} fail, cause:{}", itemIds, itemResp.getError());
                throw new JsonResponseException(itemResp.getError());
            }
            items = itemResp.getResult();
            itemSize = searchedItemPaging.getTotal();

        } else {
            itemCode = itemNameOrCode.trim();
            Integer status = DefaultItemStatus.ITEM_ONSHELF;
            Response<Paging<Item>> itemsResp =
                    vegaItemReadService.findBy(itemCode, null, status, pageNo, pageSize);
            if (!itemsResp.isSuccess()) {
                log.error("find items to add purchase order by itemCode:{}, cause:{}",
                        itemCode, itemsResp.getError());
                throw new JsonResponseException(itemsResp.getError());
            }
            items = itemsResp.getResult().getData();
            itemSize = itemsResp.getResult().getTotal();

        }

        return new Paging<>(itemSize, formRichShopItem(items));
    }

    /**
     * 采购单添加商品接口修改,增加类目参数
     * Created by lujm on 2016/12/30
     * @param isItemName  查询类型 1:商品名称 2:商品编码 3:商品类目
     * @param FirstCategoryId  一级类目
     * @param SecondCategoryId 二级类目
     * @param ThirdCategoryId  三级类目
     * @param sort  排序字段,格式"0_0_0_0_0",新增第四位按照商品名称搜索引擎整体排序条件
     */
    @RequestMapping(value = "/item-to-add-purchase-orderNew", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<FullItem> finItemsToAddPurchaseOrder(@RequestParam String isItemName,
                                                       @RequestParam String itemNameOrCode,
                                                       @RequestParam(required = false) String FirstCategoryId,
                                                       @RequestParam(required = false) String SecondCategoryId,
                                                       @RequestParam(required = false) String ThirdCategoryId,
                                                       @RequestParam(required = false) String sort,
                                                       @RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        List<Item> items = Lists.newArrayList();
        Long itemSize = 0L;
        if (!Strings.isNullOrEmpty(isItemName)&&isItemName.equals("2")){
            //商品编码方式
            String itemCode = itemNameOrCode.trim();
            Integer status = DefaultItemStatus.ITEM_ONSHELF;
            Response<Paging<Item>> itemsResp =
                    vegaItemReadService.findBy(itemCode, null, status, pageNo, pageSize);
            if (!itemsResp.isSuccess()) {
                log.error("find items to add purchase order by itemCode:{}, cause:{}",
                        itemCode, itemsResp.getError());
                throw new JsonResponseException(itemsResp.getError());
            }
            items = itemsResp.getResult().getData();
            itemSize = itemsResp.getResult().getTotal();
        }else {
            Map<String, String> map = new HashMap<>();
            if (!Strings.isNullOrEmpty(itemNameOrCode)){
                map.put("q", itemNameOrCode.toLowerCase());
            }
            map.put("statuses", String.valueOf(DefaultItemStatus.ITEM_ONSHELF));
            if (!Strings.isNullOrEmpty(FirstCategoryId)) {
                map.put("fcid", FirstCategoryId);//一级类目
            }
            if(!Strings.isNullOrEmpty(SecondCategoryId)){
                map.put("fcid", SecondCategoryId);//二级类目
            }
            if(!Strings.isNullOrEmpty(ThirdCategoryId)){
                map.put("fcid", ThirdCategoryId);//三级类目
            }
            if(!Strings.isNullOrEmpty(sort)){
                map.put("sort", sort);//商品名称排序字段
            }
            Response<? extends SearchedItemWithAggs<VegaSearchedItem>> searchResp =
                    itemSearchReadService.searchWithAggs(pageNo, pageSize, "search.mustache", map, VegaSearchedItem.class);
            if (!searchResp.isSuccess()) {
                log.error("find item through search fail, q:{},fcid:{}, cause:{}", map.get("q"),map.get("fcid"),searchResp.getError());
                throw new JsonResponseException(searchResp.getError());
            }
            if (searchResp.isSuccess() && searchResp.getResult() != null) {
                List<VegaSearchedItem> itemList = searchResp.getResult().getEntities().getData();
                itemList.forEach(vegaSearchedItem ->
                        vegaSearchedItem.setName(fixHighLight(vegaSearchedItem.getDisplayName(), vegaSearchedItem.getName()))
                );
                searchResp.getResult().getEntities().setData(itemList);
            }
            Paging<VegaSearchedItem> searchedItemPaging = searchResp.getResult().getEntities();
            Map<Long, VegaSearchedItem> itemMap = Maps.uniqueIndex(searchedItemPaging.getData(), VegaSearchedItem::getId);
            List<Long> itemIds = Lists.transform(searchedItemPaging.getData(), VegaSearchedItem::getId);
            Response<List<Item>> itemResp = itemReadService.findByIds(itemIds);
            if (!itemResp.isSuccess()) {
                log.error("find item by ids:{} fail, cause:{}", itemIds, itemResp.getError());
                throw new JsonResponseException(itemResp.getError());
            }
            items = itemResp.getResult();
            itemSize = searchedItemPaging.getTotal();
            items.forEach(item -> item.setName(itemMap.get(item.getId()).getName()));
        }
        return new Paging<>(itemSize, formRichShopItem(items));
    }

    @RequestMapping(value = "/{itemId}/skus", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Sku> findSkusByItemId(@PathVariable Long itemId){
        Response<List<Sku>> skusResp = skuReadService.findSkusByItemId(itemId);
        if (!skusResp.isSuccess()) {
            log.error("find skus by itemId:{} fail, cause:{}", itemId, skusResp.getError());
            throw new JsonResponseException(skusResp.getError());
        }
        return skusResp.getResult();
    }

    @RequestMapping(value = "/show/items", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Item> findItemsByIds(@RequestParam Long[] ids) {
        if (ids == null || ids.length == 0) {
            return Collections.emptyList();
        }

        List<Item> items = findItems(Lists.newArrayList(ids));

        for (Item item : items) {
            List<Integer> shopSkuPrices = findShopSkuPrices(DefaultId.PLATFROM_SHOP_ID, item.getId(), null);
            if (!CollectionUtils.isEmpty(shopSkuPrices)) {
                item.setLowPrice(Collections.min(shopSkuPrices));
            }
        }
        return items;
    }

    /**
     * 批量修改一个叶子类目下所有自己商品的状态
     * @param categoryId categoryId
     * @param status status
     * @return Boolean
     */
    @RequestMapping(value = "/category/item/batch/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchSetItemStatus(@RequestParam("categoryId") Long categoryId, @RequestParam("status") Integer status) {
        ParanaUser user = UserUtil.getCurrentUser();
        List<Integer> statuses = Lists.newArrayList(DefaultItemStatus.ITEM_ONSHELF);
        Response<Optional<List<Long>>> itemIdsResp =
                vegaItemReadService.findItemIdsByCategoryIdAndShopId(categoryId, user.getShopId(), statuses);
        if(!itemIdsResp.isSuccess()) {
            log.error("find itemIds by categoryId:{} fail, cause:{}", categoryId, itemIdsResp.getError());
            throw new JsonResponseException(itemIdsResp.getError());
        }
        if (!itemIdsResp.getResult().isPresent()) {
            return Boolean.TRUE;
        }
        List<Long> itemIdList = itemIdsResp.getResult().get();

        Response<Boolean> response = itemWriteService.batchUpdateStatusByShopIdAndItemIds(user.getShopId(),itemIdList, status);
        if (!response.isSuccess()) {
            log.error("batch update items:{}, status:{} fail, cause:{}", itemIdList, status, response.getError());
            throw new JsonResponseException(response.getError());
        }

        if (Objects.equals(status, DefaultItemStatus.ITEM_WAIT_AUDIT) && response.getResult() && !Arguments.isNullOrEmpty(itemIdList)) {
            // 供应商批量设置商品状态为未审核状态(商品下架),同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIdList));
        }

        return response.getResult();
    }


    /**
     * 前台主搜当前页按名称排序接口
     * modify by lujm on 2017/1/9
     * @param sortType 排序类型 1代表倒序,其他代表升序
     */
    @RequestMapping(value = "/sort/item", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SearchedItem> sortItemHelper (@RequestBody Paging<SearchedItem> searchedItemPaging,
                                                @RequestParam String sortType)  {
        try {
            List<SearchedItem> searchedItems = searchedItemPaging.getData();
            Map<String, SearchedItem> itemMap = Maps.uniqueIndex(searchedItems, SearchedItem::getName);
            List<String> itemName = Lists.transform(searchedItems, SearchedItem::getName);
            List<SearchedItem> toReturn =  Lists.newArrayList();
            Map<String, String> map = Maps.newHashMap();
            List<String> itemNameTemp = Lists.newArrayList();
            for (String name : itemName) {
                String tempName = name;
                name = name.replaceAll("<em>", "");
                name = name.replaceAll("</em>", "");
                map.put(name, tempName);
                itemNameTemp.add(name);
            }
            //默认升序:是根据的汉字的拼音的字母排序的，而不是根据汉字一般的排序方法
            Collections.sort(itemNameTemp, Collator.getInstance(java.util.Locale.CHINA));
            if(sortType!=null&&sortType.trim().equals("1")){
                Collections.reverse(itemNameTemp);//倒序
            }
            for (String name : itemNameTemp) {
                toReturn.add(itemMap.get(map.get(name)));
            }
            return new Paging<>(searchedItemPaging.getTotal(), toReturn);
        } catch (Exception e) {
            log.error("sort item by name fail, item:{}, cause:{}", searchedItemPaging.getData(), e.getMessage() );
            throw new JsonResponseException("sort.item.fail");
        }
    }

    /**
     * 采购单当前页按名称排序接口
     * Created by lujm on 2016/12/29
     * @param sortType 排序类型 1代表倒序,其他代表升序
     * @param sortMethod 排序方法 1代表按数字排序,其他代表按商品名称排序
     * 按商品名称数字排序规则(add by lujm on 2017/1/9):
     * 1.商品名称中有数字的,获取名称中所有数字(如"普通带C889"获取的数字为"889","普通97带C889"获取的数字为"97889"),然后按数字本身大小进行升序或倒序进行排序
     * 2.商品名称中没有数字的,都转化为数字"0"进行处理
     * 3.商品名称中没有数字的部分商品(即转化之后都为0的商品),按照商品名称的拼音字母顺序进行升序或倒序
     * 4.按商品名称数字排序也只能对当前页数据进行排序
     */
    @RequestMapping(value = "/sort/item/PurchaseOrder", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<FullItem> sortItemPurchaseOrder (@RequestBody Paging<FullItem> fullItemPaging,
                                                   @RequestParam String sortType,
                                                   @RequestParam(required = false,defaultValue="1") String sortMethod) {
        try {

            List<FullItem> fullItems = fullItemPaging.getData();
            Map<String, FullItem> fullItemMap = Maps.uniqueIndex(fullItems, new Function<FullItem, String>() {
                @Override
                public String apply(FullItem input) {
                    return input.getItem().getName();
                }
            });

            List<String> itemName = Lists.transform(fullItems, new Function<FullItem, String>() {
                @Override
                public String apply(FullItem input) {
                    return input.getItem().getName();
                }
            });

            List<FullItem> toReturn = Lists.newArrayList();

            if (sortMethod != null && sortMethod.trim().equals("1")) {
                /**按数字进行排序**/
                Map<Long, String> mapNumber = Maps.newHashMap();
                Map<String, String> mapName = Maps.newHashMap();
                List<Long> itemNumberTemp = Lists.newArrayList();
                List<String> itemNameTemp = Lists.newArrayList();

                for (String name : itemName) {
                    String tempName = name;
                    Long itemNameToNumber=getNumbers(name);//获取商品名称中的所有数字,没有数字返回0l;
                    if(itemNameToNumber!=0){
                        //商品名称包含数字
                        mapNumber.put(itemNameToNumber, tempName);
                        itemNumberTemp.add(itemNameToNumber);
                    }else{
                        mapName.put(name, tempName);
                        itemNameTemp.add(name);
                    }
                }
                //默认升序:商品名称有数字的按照数字大小进行排序
                Collections.sort(itemNumberTemp);
                //默认升序:商品名称没有数字的按照商品名称汉字的拼音的字母排序的
                Collections.sort(itemNameTemp, Collator.getInstance(java.util.Locale.CHINA));
                if (sortType != null && sortType.trim().equals("1")) {
                    //倒序
                    Collections.reverse(itemNumberTemp);
                    Collections.reverse(itemNameTemp);

                    for (Long name : itemNumberTemp) {
                        toReturn.add(fullItemMap.get(mapNumber.get(name)));
                    }
                    for (String name : itemNameTemp) {
                        toReturn.add(fullItemMap.get(mapName.get(name)));
                    }
                }else {
                    //升序,先add商品名称没有数字(转化之后都为0)的商品
                    for (String name : itemNameTemp) {
                        toReturn.add(fullItemMap.get(mapName.get(name)));
                    }
                    for (Long name : itemNumberTemp) {
                        toReturn.add(fullItemMap.get(mapNumber.get(name)));
                    }
                }
            }else{
                /**按商品名称进行排序**/
                Map<String, String> map = Maps.newHashMap();
                List<String> itemNameTemp = Lists.newArrayList();
                for (String name : itemName) {
                    String tempName = name;
                    map.put(name, tempName);
                    itemNameTemp.add(name);
                }
                //默认升序:是根据的汉字的拼音的字母排序的，而不是根据汉字一般的排序方法
                Collections.sort(itemNameTemp, Collator.getInstance(java.util.Locale.CHINA));
                if (sortType != null && sortType.trim().equals("1")) {
                    Collections.reverse(itemNameTemp);//倒序
                }

                for (String name : itemNameTemp) {
                    toReturn.add(fullItemMap.get(map.get(name)));
                }
            }
            return new Paging<>(fullItemPaging.getTotal(), toReturn);

            }catch(Exception e){
                log.error("sort item by name fail, FullItem:{}, cause:{}", fullItemPaging.getData(), e.getMessage());
                throw new JsonResponseException("sort.item.fail");
            }
        }

    /**
     * 供应商中心-商品管理-上架商品(改用搜索引擎接口)
     * Created by lujm on 2016/12/28
     * 原先接口:/api/seller/items/paging
     */
    @RequestMapping(value = {"/seller/items/pagingitems-by"}, method = {RequestMethod.GET}, produces = {"application/json"})
    public Paging<Item> pagingByItems(@RequestParam(required = false) Long itemId,
                                      @RequestParam(required = false) Long shopId,
                                      @RequestParam(required = false) String itemName,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String statuses,
                                      @RequestParam(required = false) String itemCode,
                                      @RequestParam(required = false) Integer pageNo,
                                      @RequestParam(required = false) Integer pageSize) {
        Map<String, String> map = new HashMap<>();
        if (itemId != null) {
            map.put("ids", itemId.toString());
        }
        if (!Strings.isNullOrEmpty(itemName)) {
            map.put("q", itemName.toLowerCase());
        }
        if (shopId != null) {
            map.put("shopId", shopId.toString());
        }
        if (!Strings.isNullOrEmpty(statuses)) {
            //非上架商品
            map.put("statuses", statuses);
        }
        if (status!=null) {
            //上架商品
            map.put("statuses", status.toString());
        }
        if (!Strings.isNullOrEmpty(itemCode)) {
            map.put("itemCode", itemCode);
        }

        Response<? extends SearchedItemWithAggs<VegaSearchedItem>> searchResp =
                itemSearchReadService.searchWithAggs(pageNo, pageSize, "search.mustache", map, VegaSearchedItem.class);
        if (searchResp.isSuccess() && searchResp.getResult() != null) {
            List<VegaSearchedItem> itemList = searchResp.getResult().getEntities().getData();
            itemList.forEach(vegaSearchedItem ->
                    vegaSearchedItem.setName(fixHighLight(vegaSearchedItem.getDisplayName(), vegaSearchedItem.getName()))
            );
            searchResp.getResult().getEntities().setData(itemList);
        }

        if (!searchResp.isSuccess()) {
            log.error("admin paging item fail by (itemName:{},status:{},itemCode:{},pageNo:{},pageSize:{}),cause:{}",
                    itemName,status,itemCode,pageNo,pageSize,searchResp.getError());
            throw new JsonResponseException(searchResp.getError());
        }
        Paging<VegaSearchedItem> searchedItemPaging = searchResp.getResult().getEntities();
        return new Paging<>(searchedItemPaging.getTotal(), makeItemFromSearchedItem(searchedItemPaging.getData()));
    }

    /**
     * 改写供应商修改商品状态接口,这里只更改删除接口,其他操作还是用原先的接口
     * 原接口地址:/api/seller/items/status
     * add by lujm on 2017/03/21
     * @param ids 商品IDs
     * @return 是否成功
     */

    @RequestMapping(value = {"/seller/delete/item"}, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchUpdateStatus(@RequestParam("ids[]") Long[] ids) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        List<Long> itemIds = Arrays.asList(ids);
        Response<Boolean> rsp = vegaItemWriteService.batchDeleteItemsByShopId(paranaUser.getShopId(), itemIds);

        if(!rsp.isSuccess()) {
            log.error("failed to update status to {} for item(id={}), error code:{}",DefaultItemStatus.ITEM_DELETE,itemIds,rsp.getError());
            throw new JsonResponseException(rsp.getError());
        } else {
            for(int i=0;i<itemIds.size();i++) {
                Long id = itemIds.get(i);
                eventBus.post(new ItemUpdateEvent(id));
            }
            return rsp.getResult();
        }
    }

    /**
     * 供应商新增商品SKU接口,扩展原先供应商商品详情修改接口
     *
     * @param fullItem 商品详情
     * @return 是否成功
     */
    @RequestMapping(value = {"/seller/add/sku"}, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> update(@RequestBody FullItem fullItem) {
        ParanaUser paranaUser = UserUtil.getCurrentUser();
        Long shopId = paranaUser.getShopId();
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            log.error("failed to find shop(id={}), error code:{}", shopId, rShop.getError());
            throw new JsonResponseException(rShop.getError());
        }
        Shop shop = rShop.getResult();
        if (!Objects.equals(shop.getStatus(), VegaShopStatus.NORMAL.value())) {
            log.error("shop(id={})\'s status is {}, create item failed", shopId);
            throw new JsonResponseException("shop.status.abnormal");
        }
        checkDeliveryFeeTemplate(fullItem.getItemDeliveryFee(), shopId);
        Item item = fullItem.getItem();
        item.setShopId(shopId);
        item.setShopName(shop.getName());
        item.setTags(null);
        Long itemId = item.getId();
        Response<Item> rItem = this.itemReadService.findById(itemId);
        if (!rItem.isSuccess()) {
            log.error("failed to find item(id={}), error code:{}", itemId, rItem.getError());
            throw new JsonResponseException(rItem.getError());
        }
        if (!Objects.equals((rItem.getResult()).getShopId(), shopId)) {
            log.error("the item(id={}) is not belong to seller(shop id={})", itemId, shopId);
            throw new JsonResponseException("item.not.belong.to.seller");
        }
        item.setStockType(rItem.getResult().getStockType());
        extractInfoFromSkus(item, fullItem.getSkus());
        if (!validate(fullItem)) {
            return Response.fail("item.update.fail");
        }
        Response<Boolean> rUpdate = itemWriteService.update(fullItem);
        if (!rUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", fullItem, rUpdate.getError());
            throw new JsonResponseException(rUpdate.getError());
        }
        this.eventBus.post(new ItemUpdateEvent(itemId));
        return Response.ok(rUpdate.getResult());
    }

    /**
     * 店铺验证运费模板
     *
     * @param itemDeliveryFee
     */
    private void checkDeliveryFeeTemplate(ItemDeliveryFee itemDeliveryFee, Long shopId) {
        if (!Objects.isNull(itemDeliveryFee)) {
            Long deliveryFeeTemplateId = itemDeliveryFee.getDeliveryFeeTemplateId();
            if (!Objects.isNull(deliveryFeeTemplateId)) {
                Response<DeliveryFeeTemplate> findResp = deliveryFeeReadService.findDeliveryFeeTemplateById(deliveryFeeTemplateId);
                if (!findResp.isSuccess()) {
                    log.error("fail to find delivery fee template by id:{},cause:{}", deliveryFeeTemplateId, findResp.getError());
                    throw new JsonResponseException(findResp.getError());
                }
                DeliveryFeeTemplate deliveryFeeTemplate = findResp.getResult();
                if (!Objects.equals(deliveryFeeTemplate.getShopId(), shopId)) {
                    log.error("the delivery fee template(id={}) not belong to seller(shop id={})", deliveryFeeTemplateId, shopId);
                    throw new JsonResponseException("delivery.fee.template.not.belong.to.seller");
                }
            }
        }
    }

    /**
     * 商品库存及价格区间设置
     *
     * @param item
     * @param skus
     */
    private void extractInfoFromSkus(Item item, List<Sku> skus) {
        int highPrice;
        if(Objects.equals(item.getStockType(), 0)) {
            highPrice = 0;
            for (Sku sku : skus) {
                if (Objects.isNull(sku.getStockQuantity())) {
                    throw new JsonResponseException("stock.empty");
                }
                if (sku.getStockQuantity() < 0) {
                    throw new IllegalArgumentException("sku.stock.negative");
                }
                highPrice +=sku.getStockQuantity();
            }
            item.setStockQuantity(highPrice);
        }
        setItemPriceBySkus(item, skus);
    }

    /**
     * 商品详情校验
     *
     * @param fullItem
     * @return
     * @throws InvalidException
     */
    private Boolean validate(FullItem fullItem) throws InvalidException {
        try {
            ruleEngine.handleInboundData(fullItem, null);
            return Boolean.TRUE;
        } catch (Exception e) {
            log.error("failed to validate fullItem({}), cause:{}", fullItem.getItem(), Throwables.getStackTraceAsString(e));
            Throwables.propagateIfInstanceOf(e, InvalidException.class);
            return Boolean.FALSE;
        }
    }

    private List<Integer> findShopSkuPrices(Long shopId, Long itemId, List<Sku> skus) {
        Response<Optional<List<ShopSku>>> shopSkuResp =
                shopSkuReadService.findByShopIdAndItemId(shopId, itemId);
        if (!shopSkuResp.isSuccess()) {
            log.error("find shopSkus by itemId:{} and shopId:{} fail, cause:{}",
                    itemId, shopId, shopSkuResp.getError());
            throw new JsonResponseException(shopSkuResp.getError());
        }
        if (shopSkuResp.getResult().isPresent()) {
            List<ShopSku> shopSkus = shopSkuResp.getResult().get();
            if (!Arguments.isNullOrEmpty(skus)) {
                // 删除已经删除状态的SKU
                List<Long> skuIds = Lists.transform(skus, Sku::getId);
                shopSkus.removeIf(shopSku -> !skuIds.contains(shopSku.getSkuId()));
            }
            return Lists.transform(shopSkus, ShopSku::getPrice);
        }
        return Collections.<Integer>emptyList();
    }

    private List<FullItem> formRichShopItem (List<Item> items) {
        List<FullItem> fullItems = Lists.newArrayList();
        for (Item item : items) {
            FullItem fullItem = new FullItem();
            fullItem.setItem(item);
            fullItem.setSkus(getSkusByItemId(item.getId()));
            fullItems.add(fullItem);
        }
        return fullItems;
    }

    private List<Item> makeItemFromSearchedItem (List<VegaSearchedItem> searchedItems) {
        List<Long> itemIds = Lists.transform(searchedItems, SearchedItem::getId);
        Response<List<Item>> itemResponse = itemReadService.findByIds(itemIds);
        if (!itemResponse.isSuccess()) {
            log.error("find item by ids:{} fail, cause:{}", itemIds, itemResponse.getError());
            throw new JsonResponseException(itemResponse.getError());
        }
        return itemResponse.getResult();
    }


    /**
     * 截取商品名称中数字方法
     * Created by lujm on 2017/1/9
     * 如:
     * "普通带C889"获取的数字为"889"
     * "普通97带C889"获取的数字为"97889"
     * "普通带C"获取的数字为"0"
     */
    private Long getNumbers(String content) {
        String regEx="[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(content);
        String toReturn=m.replaceAll("").trim();
        if(toReturn.equals("")){
            return 0l;
        }else {
            return Long.parseLong(toReturn);
        }
    }

    /**
     * 处理搜索引擎返回结果中高亮字符串字母大小写问题
     * add by lujm on 2017/2/17
     * 如:商品名称为"普通带A",搜索引擎返回"普通带<em>a</em>",需要返回"普通带<em>A</em>"
     *
     * @param itemName      商品名称字符串
     * @param highLightName 商品名称高亮显示字符串
     * @return String
     */
    private String fixHighLight(String itemName, String highLightName) {
        String result = highLightName;
        highLightName = highLightName.replaceAll("<em>", "");
        highLightName = highLightName.replaceAll("</em>", "");
        if (!StringUtils.equals(itemName.toLowerCase(), highLightName.toLowerCase())) {
            return result;
        }
        char[] testChar = highLightName.toCharArray();
        char[] demoChar = itemName.toCharArray();
        int m = 0;
        try {
            for (int i = 0; i < testChar.length; i++) {
                if (StringUtils.indexOf(result, "<em>" + testChar[i] + "</em>") >= 0) {
                    m = m + 9;//<em></em> 高亮字符计数
                }
                if (Character.isLowerCase(testChar[i]) || Character.isUpperCase(testChar[i])) {
                    if (StringUtils.indexOf(result, "<em>" + testChar[i] + "</em>") >= 0) {
                        //如果是高亮显示的字母
                        int j = result.indexOf(testChar[i], i + m - 5 );//获取需要替换字符串的起始位置,减去固定长度</em>
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    } else {
                        int j = result.indexOf(testChar[i], i + m);//获取需要替换字符串的起始位置
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    }
                }
            }
        }catch(Exception e){
            log.error("get highLight name fail itemName:{},highLightName:{},cause={}",itemName,result,e.getMessage());
        }
        return result;
    }

    /**
     * 根据skus获取item表price信息
     *
     * @param item item
     * @param skus skus
     */
    private void setItemPriceBySkus(Item item, List<Sku> skus) {
        if(Arguments.isNullOrEmpty(skus) || skus.size()< 1){
            return;
        }
        Integer highPrice =skus.get(0).getPrice();
        Integer lowPrice = skus.get(0).getPrice();
        for(Sku sku : skus){
            if (sku.getPrice() <= 0) {
                throw new IllegalArgumentException("sku.price.need.positive");
            }
            if(lowPrice > sku.getPrice()) lowPrice = sku.getPrice();
            if(highPrice < sku.getPrice()) highPrice = sku.getPrice();
        }
        if (lowPrice > 0) {
            item.setLowPrice(lowPrice);
        }
        if (highPrice > 0) {
            item.setHighPrice(highPrice);
        }
    }

    /**
     * 根据商品ID获取SKU信息
     * @param itemId 商品Id
     * @return SKU信息
     */
    private List<Sku> getSkusByItemId(Long itemId) {
        Response<List<Sku>> skusResp = skuReadService.findSkusByItemId(itemId);
        if (!skusResp.isSuccess()) {
            log.error("find skus by itemId:{} fail, cause:{}", itemId, skusResp.getError());
            throw new JsonResponseException(skusResp.getError());
        }
        List<Sku> skuList = skusResp.getResult();

        ParanaUser buyer = UserUtil.getCurrentUser();
        OrderUserType orderUserType = UserTypeHelper.getOrderUserTypeByUser(buyer);
        String roleName = getUserRoleName(buyer);
        skuList.forEach(sku -> sku.setPrice(getRealSkuPrice(sku.getId(), getReceiveShopId(roleName, buyer), buyer.getId(), orderUserType)));

        return skuList;
    }

    /**
     * 获取SKU真实价格
     * @param skuId skuId
     * @param receiveShopId 接单店铺
     * @param userId 买家用户Id
     * @param orderUserType 买家类型
     * @return 价格
     */
    private Integer getRealSkuPrice(Long skuId, Long receiveShopId, Long userId, OrderUserType orderUserType) {
        Response<Integer> skuPriceResp =
                receiveShopParser.findSkuPrice(skuId, receiveShopId, userId, orderUserType);
        if (!skuPriceResp.isSuccess()) {
            log.error("find sku price fail, skuId:{}, shopId:{}, userId:{}, cause:{}",
                    skuId, receiveShopId, userId, skuPriceResp.getError());
            throw new JsonResponseException(skuPriceResp.getError());
        }

        return skuPriceResp.getResult();
    }

    /**
     * 采购单增加商品列表查询获取用户接单店铺
     * @param roleName 买家角色
     * @param buyer 买家信息
     * @return 接单店铺Id
     */
    private Long getReceiveShopId(String roleName, ParanaUser buyer) {
        //一级下单
        if (StringUtils.equals(roleName, VegaUserRole.DEALER_FIRST.name())) {
            return DefaultId.PLATFROM_SHOP_ID;
        }
        //二级下单
        if (StringUtils.equals(roleName, VegaUserRole.DEALER_SECOND.name())) {
            Response<Long> shopPidRes = receiveShopParser.getShopPidForDealerSecond(buyer.getId());
            if (!shopPidRes.isSuccess()) {
                log.error("get shop pid by dealer second user id:{}", buyer.getId());
                throw new JsonResponseException(shopPidRes.getError());
            }
            return shopPidRes.getResult();
        }

        //普通用户
        if (StringUtils.equals(roleName, UserRole.BUYER.name())) {
            Response<Long> shoIdRes = receiveShopParser.getShopIdByrBuyer(buyer);
            if (shoIdRes.isSuccess()) {
                return shoIdRes.getResult();
            }
            log.error("get shop id for buyer where user info id:{} error:{}", buyer.getId(), shoIdRes.getError());
            throw new JsonResponseException(shoIdRes.getError());
        }

        throw new JsonResponseException("not.matching.receive.shop");
    }



}
