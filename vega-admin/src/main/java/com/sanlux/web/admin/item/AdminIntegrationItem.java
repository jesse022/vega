package com.sanlux.web.admin.item;

import com.sanlux.item.dto.IntegrationItemCriteria;
import com.sanlux.item.model.IntegrationItem;
import com.sanlux.item.service.IntegrationItemReadService;
import com.sanlux.item.service.IntegrationItemWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.PagingCriteria;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/integration/item")
public class AdminIntegrationItem {

    @RpcConsumer
    private IntegrationItemReadService integrationItemReadService;

    @RpcConsumer
    private IntegrationItemWriteService integrationItemWriteService;

    @RequestMapping(value = "/create", method = RequestMethod.POST)
    public Long createIntegrationItem(@RequestBody IntegrationItem integrationItem) {
        Response<Long> response = integrationItemWriteService.create(integrationItem);
        if (!response.isSuccess()) {
            log.error("create integration item fail, integrationItem:{}, cause:{}",
                    integrationItem, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/update", method = RequestMethod.POST)
    public Boolean updateIntegrationOrder (@RequestBody IntegrationItem integrationItem) {
        Response<Boolean> response = integrationItemWriteService.update(integrationItem);
        if (!response.isSuccess()) {
            log.error("update integration item fail, integrationItem:{}, cause:{}",
                    integrationItem, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<IntegrationItem> integrationItemPaging ( IntegrationItemCriteria pagingCriteria) {
        Response<Paging<IntegrationItem>> response = integrationItemReadService.paging(pagingCriteria);
        if (!response.isSuccess()) {
            log.error("paging integration item fail, pagingCriteria:{}, cause:{}",
                    pagingCriteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/set/status", method = RequestMethod.POST)
    public Boolean updateStatus(@RequestParam(value = "id", required = true) Long id,
                                @RequestParam(value = "status", required = true) Integer status) {
        Response<Boolean> response = integrationItemWriteService.setStatus(id, status);
        if (!response.isSuccess()) {
            log.error("update integration item status fail, id:{}, status:{}, cause:{}",
                    id, status, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }
}
