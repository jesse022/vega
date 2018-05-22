package com.sanlux.trade.service;

import com.sanlux.trade.model.VegaDirectPayInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Created by liangfujie on 16/10/28
 */
public interface VegaDirectPayInfoReadService {

    public Response<List<VegaDirectPayInfo>> findManyByStatus(Integer status);

    public Response<Paging<VegaDirectPayInfo>> pagingByStatus(Integer pageNo, Integer pageSize, Integer status);

    public Response<VegaDirectPayInfo> findByBusinessId(String businessId);

    public Response<VegaDirectPayInfo> findByOrderId(Long orderId);


}
