package com.sanlux.item.service;

import com.sanlux.item.dto.excel.UploadRaw;
import io.terminus.common.model.Response;
import io.terminus.parana.item.model.Sku;

import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/10/28
 */
public interface VegaSkuWriteService {

    /**
     * 供应商批量导出库存
     * @param shopId 店铺Id
     * @param rawData 导入Excel数据
     * @return {参数1:status 是否成功;参数2:items 商品列表}
     */
    Response<Map<String, Object>> uploadToImportRaw(Long shopId, UploadRaw rawData);

    Response<Boolean> batchUpdateOuterSkuId (List<Sku> skuList);

    /**
     * 根据导入表格,修改店铺冻结状态商品的供货价格,并把商品状态设置成指定状态
     *
     * @param shopId 店铺Id
     * @param rawData 导入数据
     * @return 是否成功
     */
    Response<Boolean> batchUpdateSellerPriceByExcel(Long shopId, UploadRaw rawData);

}
