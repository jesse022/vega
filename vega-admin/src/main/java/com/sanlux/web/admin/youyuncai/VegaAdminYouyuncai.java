package com.sanlux.web.admin.youyuncai;

import com.sanlux.web.front.core.youyuncai.request.VegaYouyuncaiComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 友云采对接control
 * Created by lujm on 2018/1/30.
 */
@RestController
@Slf4j
@RequestMapping("/api/vega/admin/youyuncai")
public class VegaAdminYouyuncai {
    @Autowired
    private VegaYouyuncaiComponent vegaYouyuncaiComponent;

    /**
     * 商品分类初始化接口
     * @return 是否成功
     */
    @RequestMapping(value = "/category-init", method = RequestMethod.GET)
    public Boolean categoryInit() {
        return vegaYouyuncaiComponent.categoryInit();
    }

    /**
     * 商品初始化接口
     * @return 是否成功
     */
    @RequestMapping(value = "/sku-init", method = RequestMethod.GET)
    public Boolean skuInit(){
        return vegaYouyuncaiComponent.skuInit();
    }

    /**
     * 单个商品更新(新增,修改,删除)接口(含多个SKU)[未使用,单个更新也可以使用下面的批量更新接口]
     * @param itemId 商品Id
     * @param apiType 接口类型
     * @return 是否成功
     */
    @Deprecated
    @RequestMapping(value = "/sku-sync-by-item", method = RequestMethod.GET)
    public Boolean skuSyncByItem(@RequestParam("itemId") Long itemId, @RequestParam("apiType") Integer apiType){
        return vegaYouyuncaiComponent.skuSyncByItem(itemId, apiType);
    }

    /**
     * 商品批量更新(自动拆分成新增,修改,删除接口)
     * @param itemIds 商品Ids
     * @return 是否成功
     */
    @RequestMapping(value = "/sku-sync-by-items", method = RequestMethod.GET)
    public Boolean skuSyncByItems(@RequestParam("itemIds") List<Long> itemIds){
        return vegaYouyuncaiComponent.skuSyncByItems(itemIds);
    }

}
