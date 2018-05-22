package com.sanlux.trade.service;

import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import io.terminus.common.model.Response;

/**
 * Created by liangfujie on 16/10/28
 */
public interface VegaDirectPayInfoWriteService {

    Response<Boolean> updateStatusByBusinessId(String businessId, Integer newStatus);

    Response<Boolean> create (VegaDirectPayInfo vegaDirectPayInfo, VegaSellerTradeDailySummary sellerTradeDailySummary);

    Response<Boolean> updateVegaDirectPayInfoAndSettleOrderDetail(VegaDirectPayInfo vegaDirectPayInfo,
                                                                  VegaSellerTradeDailySummary sellerTradeDailySummary);
}
