package com.sanlux.web.front.controller.trade;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sanlux.item.model.IntegrationItem;
import com.sanlux.item.service.IntegrationItemReadService;
import com.sanlux.item.service.IntegrationItemWriteService;
import com.sanlux.trade.dto.IntegrationOrderCriteria;
import com.sanlux.trade.enums.IntegrationOrderStatus;
import com.sanlux.trade.model.IntegrationOrder;
import com.sanlux.trade.service.IntegrationOrderReadService;
import com.sanlux.trade.service.IntegrationOrderWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.spring.web.json.Json;

import java.util.Map;

/**
 * Created by cuiwentao
 * on 16/11/7
 */
@Slf4j
@RestController
@RequestMapping("/api/integration-order")
public class VegaIntegrationOrders {

    @RpcConsumer
    private IntegrationOrderReadService orderReadService;

    @RpcConsumer
    private IntegrationOrderWriteService orderWriteService;
    
    @RpcConsumer
    private IntegrationItemWriteService integrationItemWriteService;

    @RpcConsumer
    private IntegrationItemReadService integrationItemReadService;

    @RpcConsumer
    private UserReadService<User> userReadService;

    @RpcConsumer
    private UserWriteService<User> userWriteService;

    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<IntegrationOrder> orderPaging ( IntegrationOrderCriteria criteria) {
        ParanaUser user = UserUtil.getCurrentUser();
        criteria.setBuyerId(user.getId());
        Response<Paging<IntegrationOrder>> response = orderReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("paging integration order fail,criteria:{}, cause:{}",
                    criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public Map<String, Integer> createIntegrationOrder (@RequestBody IntegrationOrder order) {

        Response<IntegrationItem> itemResp = integrationItemReadService.findById(order.getItemId());
        if (!itemResp.isSuccess()) {
            log.error("find integration item by id:{} fail, cause:{}",
                    order.getItemId(), itemResp.getError());
            throw new JsonResponseException(itemResp.getError());
        }
        IntegrationItem item = itemResp.getResult();
        order.setItemName(item.getName());
        order.setItemImage(item.getImagesJson());
        order.setIntegrationPrice(item.getIntegrationPrice());
        order.setIntegrationFee(item.getIntegrationPrice() * order.getQuantity());

        Long buyerId = order.getBuyerId();
        if (buyerId == null) {
            log.error("create order :{} fail, cause: buyerId is null", order);
            throw new JsonResponseException("buyer.id.is.null");
        }

        order.setStatus(IntegrationOrderStatus.WAIT_DELIVERY.value());

        Response<Boolean> resp =
                integrationItemWriteService.checkAndReduceStock(order.getItemId(), order.getQuantity());
        if (!resp.isSuccess()) {
            log.error("reduce integration item stock quantity fail, id:{}, quantity:{}, cause:{}",
                    order.getItemId(), order.getQuantity(), resp.getError());
            throw new JsonResponseException(resp.getError());
        }

        if (!reduceUserIntegration(findUserById(buyerId), order.getIntegrationFee())) {
            log.error("reduce user integration fail maybe user integration not enough,integration:{}",
                    order);
            throw new JsonResponseException("reduce.user.integration.fail");
        }

        Response<Long> writeResp = orderWriteService.create(order);
        if (!writeResp.isSuccess()) {
            log.error("create integration order fail, order:{},cause:{}",
                    order, writeResp.getError());
            throw new JsonResponseException(writeResp.getError());
        }

        User newUser = findUserById(buyerId);
        Map<String, Integer> integrationMap = Maps.newHashMap();
        integrationMap.put("integrationCost", order.getIntegrationFee());
        integrationMap.put("integrationLeft", Integer.parseInt(newUser.getExtra().get("integration")));
        return integrationMap;
    }


    private Boolean reduceUserIntegration(User user, Integer cost) {
        if (user == null || CollectionUtils.isEmpty(user.getExtra())
                || Strings.isNullOrEmpty(user.getExtra().get("integration"))
                ) {
            return Boolean.FALSE;
        }
        if (Integer.parseInt(user.getExtra().get("integration")) < cost) {
            log.error("user integration not enough,integration {},cost {}", user.getExtra().get("integration"), cost);
            throw new JsonResponseException("user.integration.not.enough");
        }

        User toUpdate = new User();
        Map<String, String> extra = user.getExtra();
        extra.replace("integration", String.valueOf((Integer.parseInt(extra.get("integration")) - cost)));
        toUpdate.setId(user.getId());
        toUpdate.setExtra(extra);

        Response<Boolean> writeUserResp = userWriteService.update(toUpdate);
        if (!writeUserResp.getResult()) {
            log.error(" create integration oder reduce user integration fail,userId:{}, cost:{}, cause:{}",
                    user.getId(), cost, writeUserResp.getError());
            throw new JsonResponseException(writeUserResp.getError());
        }

        return writeUserResp.getResult();
    }

    private User findUserById(Long id) {
        Response<User> userResponse = userReadService.findById(id);
        if (!userResponse.isSuccess()) {
            log.error("find user by id:{} fail, cause:{}", id, userResponse.getError());
            throw new JsonResponseException(userResponse.getError());
        }
        return userResponse.getResult();
    }


}
