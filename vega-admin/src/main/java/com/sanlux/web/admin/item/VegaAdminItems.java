package com.sanlux.web.admin.item;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.*;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.sanlux.common.constants.DefaultItemStatus;
import com.sanlux.item.dto.*;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.model.ShopItem;
import com.sanlux.item.model.ShopSku;
import com.sanlux.item.service.*;
import com.sanlux.search.dto.VegaSearchedItem;
import com.sanlux.web.front.core.events.youyuncai.VegaYouyuncaiEvent;
import com.sanlux.web.front.core.util.ItemUploadExcelAnalyzer;
import com.sanlux.web.front.core.utils.ExportHelper;
import com.sanlux.youyuncai.enums.YouyuncaiApiType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.dto.ItemWithSkus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.AdminItemWriteService;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.ItemWriteService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.search.dto.SearchedItem;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.search.item.ItemSearchReadService;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.Objects;

/**
 * Author:cp
 * Created on 8/11/16.
 */
@RestController
@Slf4j
@RequestMapping("/api")
public class VegaAdminItems {

    @RpcConsumer
    private ShopItemReadService shopItemReadService;

    @RpcConsumer
    private ItemReadService itemReadService;

    @RpcConsumer
    private SkuReadService skuReadService;

    @RpcConsumer
    private ShopItemWriteService shopItemWriteService;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @RpcConsumer
    private VegaSkuReadService vegaSkuReadService;

    @RpcConsumer
    private AdminItemWriteService adminItemWriteService;

    @RpcConsumer
    private ItemWriteService itemWriteService;

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private VegaItemReadService vegaItemReadService;

    @RpcConsumer
    private ShopSkuReadService shopSkuReadService;

    @RpcConsumer
    private ItemSearchReadService itemSearchReadService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventBus eventBus;

    private final static TypeReference SHOP_SKU_PRICE_TYPE = new TypeReference<List<ShopSkuPrice>>() {
    };

    @RequestMapping(value = "/item/{id}/skus-with-shop-sku-price", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public RichSkus findSkusWithShopSkuPrice(@PathVariable("id") Long itemId) {
        Response<RichSkus> findResp = vegaSkuReadService.findSkusWithShopSkuPrice(0L, itemId);
        if (!findResp.isSuccess()) {
            log.error("fail to find skus with shop sku price by shopId=0,itemId={},cause:{}",
                    itemId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    @RequestMapping(value = "/shop-item/{itemId}/set-prices", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean setShopSkuPrices(@PathVariable("itemId") Long itemId,
                                    @RequestParam("skuPrices") String skuPrices) throws Exception {


        List<ShopSkuPrice> shopSkuPrices;
        try {
            shopSkuPrices = objectMapper.readValue(skuPrices, SHOP_SKU_PRICE_TYPE);
        } catch (IOException e) {
            log.error("can not deserialize to ShopSkuPrice List from json:{},cause:{}",
                    skuPrices, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(400, "sku.price.not.provided");
        }

        fillShopId(shopSkuPrices);

        Response<Optional<ShopItem>> findShopItem = shopItemReadService.findByShopIdAndItemId(0L, itemId);
        if (!findShopItem.isSuccess()) {
            log.error("fail to find shop item by shopId=0,itemId={},cause:{}",
                    itemId, findShopItem.getError());
            throw new JsonResponseException(findShopItem.getError());
        }
        Optional<ShopItem> shopItemOptional = findShopItem.getResult();

        if (shopItemOptional.isPresent()) {
            Response<Boolean> setResp = shopSkuWriteService.batchSetPrice(shopSkuPrices);
            if (!setResp.isSuccess()) {
                log.error("fail to batch set shop skus price:{},cause:{}",
                        shopSkuPrices, setResp.getError());
                throw new JsonResponseException(setResp.getError());
            }
        } else {
            ShopItem shopItem = makeShopItem(itemId);
            List<ShopSku> shopSkus = makeShopSkus(itemId, shopSkuPrices);
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

        // 运营审核商品成功,同步友云采
        eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(ImmutableList.of(itemId)));

        return true;
    }

    @RequestMapping(value = "/shop-item/batch/set-prices", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean batchSetShopSkuPrices(@RequestParam("itemIds") Long[] itemIds,
                                         @RequestParam("skuPrices") String skuPrices) throws Exception {
        List<ShopSkuPrice> shopSkuPrices;
        try {
            shopSkuPrices = objectMapper.readValue(skuPrices, SHOP_SKU_PRICE_TYPE);
        } catch (IOException e) {
            log.error("can not deserialize to ShopSkuPrice List from json:{},cause:{}",
                    skuPrices, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(400, "sku.price.not.provided");
        }

        fillShopId(shopSkuPrices);

        List<Long> itemIdList = Arrays.asList(itemIds);

        Response<Boolean> createResp =
                shopItemWriteService.batchCreateAndUpdate(makeShopItems(itemIdList), makeShopSkus(shopSkuPrices));
        if (!createResp.isSuccess()) {
            log.error("fail to batch create richShopItem , itemIds:{}, cause:{}",
                    itemIds, createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }

        batchSetItemStatus(itemIds, DefaultItemStatus.ITEM_ONSHELF);

        if (createResp.getResult()) {
            // 运营批量审核商品成功,同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIdList));
        }
        return createResp.getResult();
    }

    @RequestMapping(value = "/item/batch/status", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean batchSetItemStatus(@RequestParam("itemIds") Long[] itemIds, @RequestParam("status") Integer status) {
        List<Long> itemIdList = Arrays.asList(itemIds);

        Response<List<Item>> itemResp = itemReadService.findByIds(itemIdList);
        if (!itemResp.isSuccess()) {
            log.error("find items by ids:{} fail, cause:{}", itemIdList, itemResp.getError());
            throw new JsonResponseException(itemResp.getError());
        }
        List<Item> items = itemResp.getResult();

        if (!Objects.equals(status, DefaultItemStatus.ITEM_ONSHELF)) {
            Integer itemRightStatus =
                    Objects.equals(status, DefaultItemStatus.ITEM_FREEZE) ? DefaultItemStatus.ITEM_ONSHELF : DefaultItemStatus.ITEM_FREEZE;

            if (items.size() < itemIdList.size()
                    || !items.stream().allMatch(item -> Objects.equals(item.getStatus(), itemRightStatus))) {
                if (Objects.equals(status, DefaultItemStatus.ITEM_FREEZE)) {
                    throw new JsonResponseException("only.on.shelf.item.can.freeze");
                }
                throw new JsonResponseException("only.freeze.item.can.unfeeze");
            }
        }

        Response<Boolean> response = adminItemWriteService.batchUpdateStatus(itemIdList, status);
        if (!response.isSuccess()) {
            log.error("batch update items:{}, status:{} fail, cause:{}", itemIdList, status, response.getError());
            throw new JsonResponseException(response.getError());
        }

        if (response.getResult()) {
            // 运营冻结解冻商品成功,同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(itemIdList));
        }

        return response.getResult();
    }

    @RequestMapping(value = "/item/{id}/reject", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean rejectItem(@PathVariable Long id, @RequestParam String rejectReason) {

        Response<Item> itemResponse = itemReadService.findById(id);
        if (!itemResponse.isSuccess()) {
            log.error("find item by id:{} fail, cause:{}", id, itemResponse.getError());
            throw new JsonResponseException(itemResponse.getError());
        }
        Item toUpdate = new Item();
        Map<String, String> extra = itemResponse.getResult().getExtra();
        extra.put("rejectReason", rejectReason);
        toUpdate.setId(id);
        toUpdate.setStatus(DefaultItemStatus.ITEM_REFUSE);
        toUpdate.setExtra(extra);

        Response<Boolean> updateResp = itemWriteService.updateItem(toUpdate);
        if (!updateResp.isSuccess()) {
            log.error("update item fail, item:{},cause:{}", toUpdate, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        if (updateResp.getResult()) {
            // 审核不通过,同步友云采,触发删除接口(重新修改时不通过)
            //// TODO: 2018/2/6 友云采不存在时,友云采会不会报错?
            eventBus.post(VegaYouyuncaiEvent.formItemByitemId(id, YouyuncaiApiType.ITEM_DELETE.value()));
        }
        return updateResp.getResult();
    }

    @RequestMapping(value = "/paging/item-for-check", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ItemWithSkus> itemWithSkusPaging(@RequestParam(required = false) Integer pageNo,
                                                   @RequestParam(required = false) Integer pageSize) {

        Response<Paging<ItemWithSkus>> skusResp = vegaItemReadService.findItemWithSkusWaitCheck(pageNo, pageSize);
        if (!skusResp.isSuccess()) {
            log.error("find item with skus fail,  pageNo:{}, pageSize:{}, cause:{}",
                    pageNo, pageSize, skusResp.getError());
            throw new JsonResponseException(skusResp.getError());
        }
        return skusResp.getResult();
    }

    @RequestMapping(value = "/paging/shop-sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ShopSkuWithNameAndAttrs> pagingShopSku (ShopSkuCriteria criteria) {
        Response<Paging<ShopSku>> shopSkuResp = shopSkuReadService.shopSkuPaging(criteria);
        if (!shopSkuResp.isSuccess()) {
            log.error("paging shop sku fail, criteria:{}, cause:{}", criteria, shopSkuResp.getError());
            throw new JsonResponseException(shopSkuResp.getError());
        }
        Paging<ShopSku> shopSkuPaging = shopSkuResp.getResult();
        List<ShopSku> shopSkus = shopSkuPaging.getData();
        List<Long> skuIds = Lists.transform(shopSkus, ShopSku::getSkuId);
        List<Long> itemIds = Lists.transform(shopSkus, ShopSku::getItemId);
        Response<List<Sku>> skuResp = skuReadService.findSkusByIds(skuIds);
        if(!skuResp.isSuccess()) {
            log.error("find sku by ids:{} fail, cause:{}", skuIds, skuResp.getError());
            throw new JsonResponseException(skuResp.getError());
        }
        Response<List<Item>> itemResp = itemReadService.findByIds(itemIds);
        if (!itemResp.isSuccess()) {
            log.error("find item by ids:{} fail, cause:{}", itemIds, itemResp.getError());
            throw new JsonResponseException(itemResp.getError());
        }
        Map<Long, Sku> skuIndexById = Maps.uniqueIndex(skuResp.getResult(), Sku::getId);
        Map<Long, Item> itemIndexById = Maps.uniqueIndex(itemResp.getResult(), Item::getId);
        List<ShopSkuWithNameAndAttrs> shopSkuWithNameAndAttrses = Lists.newArrayListWithCapacity(shopSkus.size());
        shopSkus.forEach(shopSku -> {
            ShopSkuWithNameAndAttrs shopSkuWithNameAndAttrs = new ShopSkuWithNameAndAttrs();
            shopSkuWithNameAndAttrs.setShopSku(shopSku);
            shopSkuWithNameAndAttrs.setAttrs(skuIndexById.get(shopSku.getSkuId()).getAttrs());
            shopSkuWithNameAndAttrs.setName(itemIndexById.get(shopSku.getItemId()).getName());
            shopSkuWithNameAndAttrses.add(shopSkuWithNameAndAttrs);
        });
        return new Paging<>(shopSkuPaging.getTotal(), shopSkuWithNameAndAttrses);
    }

    @RequestMapping(value = "/find-all-freeze-item", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Item> findAllFreezeItem (@RequestParam Integer pageNo, @RequestParam Integer pageSize) {
        Map<String, String> map = new HashMap<>();
        map.put("statuses", String.valueOf(DefaultItemStatus.ITEM_FREEZE));

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
            log.error("find all freeze item through search fail, cause:{}");
            throw new JsonResponseException(searchResp.getError());
        }
        Paging<VegaSearchedItem> searchedItemPaging = searchResp.getResult().getEntities();
        return new Paging<>(searchedItemPaging.getTotal(), makeItemFromSearchedItem(searchedItemPaging.getData()));

    }

    /**
     * 下载运营批量导入销售价模板,导出所有待审核状态下的商品
     *
     * @param httpServletResponse
     */
    @RequestMapping(value = "/download-set-price-excel", method = RequestMethod.GET)
    public void downLoadSetPriceExcel (HttpServletResponse httpServletResponse) {
        try {
            String xlsFileName = URLEncoder.encode("运营批量设置销售价模板", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildSetPricTemplateFile(httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download.batch.update.seller.price.excel.fail");
            throw new JsonResponseException("download.batch.update.seller.price.excel.fail");
        }
    }

    /**
     * 运营批量导入商品销售价
     * @param file 文件
     * @return 是否成功
     * @throws IOException
     */
    @RequestMapping(value = "/upload-set-price-excel", method = RequestMethod.POST, produces = MediaType.TEXT_PLAIN_VALUE)
    public Response<Boolean> uploadSetPrice (MultipartFile file) throws IOException {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            UploadRaw rawData = ItemUploadExcelAnalyzer.analyzeSellerPriceExcel(file.getInputStream());
            Response<List<Long>> response = shopItemWriteService.batchUpdatePriceByExcel(rawData);

            if (!response.isSuccess()) {
                log.error("upload batch update seller price excel failed, shopId:{}, cause:{}",
                        user.getShopId(), response.getError());
                throw new JsonResponseException(response.getError());
            }
            //批量更新状态
            batchSetItemStatus(response.getResult().toArray(new Long[response.getResult().size()]), DefaultItemStatus.ITEM_ONSHELF);


            // 运营批量导入销售价,同步友云采
            eventBus.post(VegaYouyuncaiEvent.formItemByitemIds(response.getResult()));

            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("upload batch update seller price excel failed, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * 导出所有上架商品SKU含散客价/供货价信息,用于运营日常价格导出
     * @return
     */
    @RequestMapping(value = "/export-sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void exportDealerSalesReportWithDay(HttpServletResponse httpServletResponse) {
        try {
            Response<List<Sku>> skuResp = vegaSkuReadService.findAllSkuWithPrice();
            if(!skuResp.isSuccess()) {
                log.error("find all sku fail, cause:{}", skuResp.getError());
                throw new JsonResponseException(skuResp.getError());
            }


            String xlsFileName = URLEncoder.encode("集乘平台商品导出信息", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);

            buildExportAllSkuTemplateFile(httpServletResponse.getOutputStream(), skuResp.getResult());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("export.all.sku.to.excel.fail");
            throw new JsonResponseException("export.all.sku.to.excel.fail");
        }
    }


    private void fillShopId(List<ShopSkuPrice> shopSkuPrices) {
        for (ShopSkuPrice shopSkuPrice : shopSkuPrices) {
            shopSkuPrice.setShopId(0L);
        }
    }

    private ShopItem makeShopItem(Long itemId) {
        Response<Item> findItem = itemReadService.findById(itemId);
        if (!findItem.isSuccess()) {
            log.error("fail to find item by itemId={},cause:{}",
                    itemId, findItem.getError());
            throw new JsonResponseException(findItem.getError());
        }
        Item item = findItem.getResult();

        ShopItem shopItem = new ShopItem();
        shopItem.setShopId(0L);
        shopItem.setItemId(item.getId());
        shopItem.setItemName(item.getName());
        shopItem.setStatus(item.getStatus());
        return shopItem;
    }

    private List<ShopItem> makeShopItems(List<Long> itemIds) {
        Response<List<Item>> findItems = itemReadService.findByIds(itemIds);
        if (!findItems.isSuccess()) {
            log.error("fail to find item by itemIds={},cause:{}",
                    itemIds, findItems.getError());
            throw new JsonResponseException(findItems.getError());
        }

        List<ShopItem> shopItems = Lists.newArrayListWithCapacity(itemIds.size());
        for (Item item : findItems.getResult()) {
            ShopItem shopItem = new ShopItem();
            shopItem.setShopId(0L);
            shopItem.setItemId(item.getId());
            shopItem.setItemName(item.getName());
            shopItem.setStatus(item.getStatus());

            shopItems.add(shopItem);
        }
        return shopItems;
    }

    private List<ShopSku> makeShopSkus(List<ShopSkuPrice> shopSkuPrices) throws Exception {
        List<Long> itemIds = Lists.transform(shopSkuPrices, ShopSkuPrice::getItemId);
        Response<List<Sku>> findSkus = skuReadService.findSkusByItemIds(itemIds);
        if (!findSkus.isSuccess()) {
            log.error("fail to find skus by itemIds={},cause:{}",
                    itemIds, findSkus.getError());
            throw new JsonResponseException(findSkus.getError());
        }
        List<Sku> skus = findSkus.getResult();

        Map<Long, Sku> skuByIdIndex = Maps.uniqueIndex(skus, Sku::getId);

        Response<List<Item>> itemResp = itemReadService.findByIds(itemIds);
        if (!itemResp.isSuccess()) {
            log.error("find item by ids:{} fail, cause:{}", itemIds, itemResp.getError());
            throw new JsonResponseException(itemResp.getError());
        }
        Map<Long, Item> itemMap = Maps.uniqueIndex(itemResp.getResult(), Item::getId);

        List<ShopSku> shopSkus = Lists.newArrayListWithCapacity(shopSkuPrices.size());
        for (ShopSkuPrice shopSkuPrice : shopSkuPrices) {
            Sku sku = skuByIdIndex.get(shopSkuPrice.getSkuId());
            if (Arguments.isNull(sku)) {
                log.error("sku(id={}) not found from item(ids={})",
                        shopSkuPrice.getSkuId(), itemIds);
                throw new JsonResponseException("sku.not.found");
            }

            ShopSku shopSku = ShopSku.from(sku);
            shopSku.setShopId(shopSkuPrice.getShopId());
            shopSku.setPrice(shopSkuPrice.getPrice());
            shopSku.setCategoryId(itemMap.get(shopSku.getItemId()).getCategoryId());
            shopSkus.add(shopSku);
        }
        return shopSkus;
    }


    private List<ShopSku> makeShopSkus(Long itemId, List<ShopSkuPrice> shopSkuPrices) throws Exception {
        Response<List<Sku>> findSkus = skuReadService.findSkusByItemId(itemId);
        if (!findSkus.isSuccess()) {
            log.error("fail to find skus by itemId={},cause:{}",
                    itemId, findSkus.getError());
            throw new JsonResponseException(findSkus.getError());
        }
        List<Sku> skus = findSkus.getResult();

        Map<Long, Sku> skuByIdIndex = Maps.uniqueIndex(skus, Sku::getId);

        List<ShopSku> shopSkus = Lists.newArrayListWithCapacity(shopSkuPrices.size());
        for (ShopSkuPrice shopSkuPrice : shopSkuPrices) {
            Sku sku = skuByIdIndex.get(shopSkuPrice.getSkuId());
            if (Arguments.isNull(sku)) {
                log.error("sku(id={}) not found from item(id={})",
                        shopSkuPrice.getSkuId(), itemId);
                throw new JsonResponseException("sku.not.found");
            }

            ShopSku shopSku = ShopSku.from(sku);
            shopSku.setShopId(shopSkuPrice.getShopId());
            shopSku.setPrice(shopSkuPrice.getPrice());
            shopSkus.add(shopSku);
        }
        return shopSkus;
    }

    /**
     * 后台商品管理翻页查询Controller,增加按叶子类目ID查询条件
     * Created by lujm
     * on 16/12/6
     */
    @RequestMapping(value = {"/item/pagingitems-by"}, method = {RequestMethod.GET}, produces = {"application/json"})
    public Paging<Item> pagingByItems(@RequestParam(required = false) Long itemId,
                                      @RequestParam(required = false) Long userId,
                                      @RequestParam(required = false) Long shopId,
                                      @RequestParam(required = false) String itemName,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) Long categoryId,
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
        if (categoryId != null) {
            map.put("bcids", categoryId.toString());
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
            log.error("admin paging item fail by (userId:{},itemName:{},status:{},categoryId:{},pageNo:{},pageSize:{}),cause:{}",
                    userId,itemName,status,categoryId,pageNo,pageSize,searchResp.getError());
            throw new JsonResponseException(searchResp.getError());
        }
        Paging<VegaSearchedItem> searchedItemPaging = searchResp.getResult().getEntities();
        return new Paging<>(searchedItemPaging.getTotal(), makeItemFromSearchedItem(searchedItemPaging.getData()));
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
                    if (StringUtils.indexOf(result, "<em>" + testChar[i] + "</em>") > -1) {
                        //如果是高亮显示的字母
                        int j = result.indexOf(testChar[i], i + m - 5);//获取需要替换字符串的起始位置,减去固定长度</em>
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    } else {
                        int j = result.indexOf(testChar[i], i + m);//获取需要替换字符串的起始位置
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    }
                }
            }
        } catch (Exception e) {
            log.error("get highLight name fail itemName:{},highLightName:{},cause={}", itemName, result, e.getMessage());
        }
        return result;
    }

    /**
     * 批量修改模板数据封装
     *
     * @param outputStream
     */
    private void buildSetPricTemplateFile (OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("商品编号", 18 * 150);
            columnMaps.put("商品名称", 18 * 800);
            columnMaps.put("SKU编号", 18 * 150);
            columnMaps.put("商品规格", 18 * 800);
            columnMaps.put("供货价(分)", 18 * 180);
            columnMaps.put("销售价(精确到分)", 18 * 180);
            XSSFSheet xssfSheet = xssfWorkbook.createSheet("商品管理");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);
            Row row = xssfSheet.createRow(1);
            List<String> contents = Lists.newArrayList();
            contents.add("item_id");
            contents.add("item_name");
            contents.add("sku_id");
            contents.add("sku_name");
            contents.add("sku_price");
            contents.add("sku_new_price");
            ExportHelper.setContent(row, contents);

            Response<List<Sku>> skusResp = vegaSkuReadService.findSpeciallyStatusSkus(DefaultItemStatus.ITEM_WAIT_AUDIT);
            if (!skusResp.isSuccess()) {
                log.error("fail to find skus by status={},cause:{}", DefaultItemStatus.ITEM_WAIT_AUDIT, skusResp.getError());
                throw new JsonResponseException("sku.find.fail");
            }
            List<Long> itemIds = Lists.transform(skusResp.getResult(), Sku::getItemId);
            Set<Long> linkedHashSet = new LinkedHashSet<>(itemIds);
            itemIds = new ArrayList<>(linkedHashSet);
            Response<List<Item>> itemsResp = itemReadService.findByIds(itemIds);
            if (!itemsResp.isSuccess()) {
                log.error("find item by ids:{}, cause:{}", itemIds, itemsResp.getError());
                throw new JsonResponseException(itemsResp.getError());
            }
            Map<Long, Item> itemIndexById = Maps.uniqueIndex(itemsResp.getResult(), Item::getId);
            int index = 0;
            for (Sku sku : skusResp.getResult()) {
                Row rowData = xssfSheet.createRow(index + 2);
                String attrs = "";
                List<SkuAttribute> skuAttributes = sku.getAttrs();
                if (!Arguments.isNullOrEmpty(skuAttributes)) {
                    //规格
                    for (SkuAttribute skuAttribute : skuAttributes) {
                        String attr = skuAttribute.getAttrKey() + ":" + skuAttribute.getAttrVal() + "  ";
                        attrs += attr;
                    }
                }
                ExportHelper.setContent(rowData, ImmutableList.of(sku.getItemId().toString(),
                        Arguments.isNull(itemIndexById.get(sku.getItemId())) ? "" :itemIndexById.get(sku.getItemId()).getName(),
                        sku.getId().toString(),
                        attrs,
                        sku.getPrice().toString(),
                        ""));
                index++;
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("download.batch.update.seller.price.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
    }



    private void buildExportAllSkuTemplateFile(OutputStream outputStream, List<Sku> skus) {
        try {
            XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();

            columnMaps.put("商品Id", 18 * 250);
            columnMaps.put("商品名称", 18 * 500);
            columnMaps.put("规格", 18 * 300);
            columnMaps.put("SkuId", 18 * 200);
            columnMaps.put("供货价", 18 * 200);
            columnMaps.put("散客价", 18 * 200);
            columnMaps.put("单位", 18 * 200);


            XSSFSheet xssfSheet = xssfWorkbook.createSheet("集乘网商品导出信息表");
            XSSFCellStyle cellStyle = xssfWorkbook.createCellStyle();

            ExportHelper.setTitleAndColumnWidth(xssfSheet, ExportHelper.setCellStyle(cellStyle), 1, 25, columnMaps);

            if (!Arguments.isNull(skus)) {
                int index = 0;
                for (Sku sku : skus) {
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 1, Arguments.isNull(sku.getItemId()) ? "" : sku.getItemId().toString());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 2, Arguments.isNull(sku.getName()) ? "" : sku.getName());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 3, Arguments.isNull(sku.getSpecification()) ? "" : sku.getSpecification());

                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 4, Arguments.isNull(sku.getId()) ? "" : sku.getId().toString());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 5, Arguments.isNull(sku.getSkuCode()) ? "" : sku.getSkuCode());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 6, Arguments.isNull(sku.getPrice()) ? "" : sku.getPrice().toString());
                    ExportHelper.setContentByRowAndColumn(xssfSheet, cellStyle, 25, 2 + index, 7, Arguments.isNull(sku.getImage()) ? "" : sku.getImage());

                    index++;
                }
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export.all.sku.to.excel.fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

}
