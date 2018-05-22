package com.sanlux.web.front.core.trade;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.common.constants.DefaultId;
import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.dto.ServiceManagerOrderDto;
import com.sanlux.trade.enums.VegaRateType;
import com.sanlux.trade.model.VegaRateDefs;
import com.sanlux.trade.service.VegaOrderReadService;
import com.sanlux.trade.service.VegaRateDefsReadService;
import com.sanlux.user.model.ServiceManagerUser;
import com.sanlux.user.service.ServiceManagerUserReadService;
import com.sanlux.web.front.core.util.ArithUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 业务经理提成订单处理类
 *
 * Created by lujm on 2017/11/17.
 */
@Component
@Slf4j
public class VegaServiceManagerOrderComponent {
    @RpcConsumer
    private ServiceManagerUserReadService serviceManagerUserReadService;
    @RpcConsumer
    private VegaOrderReadService vegaOrderReadService;
    @RpcConsumer
    private VegaRateDefsReadService vegaRateDefsReadService;
    @RpcConsumer
    private VegaShopReadService vegaShopReadService;
    @RpcConsumer
    private UserReadService<User> userReadService;
    /**
     * 根据业务经理Ids获取业务经理提成
     * @param serviceManagerIds 业务经理Ids
     * @param startAt 下单开始时间
     * @param endAt 下单结束时间
     * @return 业务经理提成信息
     */
    public List<ServiceManagerOrderDto> getServiceManagerOrder(List<Long> serviceManagerIds, String startAt, String endAt) {
        try {
            Response<List<ServiceManagerUser>> listResponse = serviceManagerUserReadService.findByServiceManagerIds(serviceManagerIds);
            if (!listResponse.isSuccess()) {
                log.error("failed to find service manager user by serviceManagerIds = {}, cause : {}", serviceManagerIds, listResponse.getError());
                throw new JsonResponseException(500, listResponse.getError());
            }

            List<Long> buyerIds = Lists.transform(listResponse.getResult(), ServiceManagerUser::getUserId);
            Map<Long, ServiceManagerUser> serviceManagerMap = Maps.uniqueIndex(listResponse.getResult(), ServiceManagerUser::getUserId);

            List<ServiceManagerOrderDto> serviceManagerOrderDtos = Lists.newArrayList();
            if (!Arguments.isNullOrEmpty(buyerIds)) {
                Response<List<ShopOrder>> shopOrderList = vegaOrderReadService.findShopOrderByBuyerIds(startDate(startAt), endDate(endAt), buyerIds);
                if (!shopOrderList.isSuccess()) {
                    log.error("failed to find shop order, startAt={}, endAt={}, criteria={}, cause:{}",
                            startAt, endAt, buyerIds, shopOrderList.getError());
                    throw new JsonResponseException(500, shopOrderList.getError());
                }

                VegaRateDefs newMemberOrderCommission = findServiceManagerOrderCommissionRate(
                        VegaRateType.NEW_MEMBER_ORDER_COMMISSION.type(), VegaRateType.NEW_MEMBER_ORDER_COMMISSION.rateName());
                VegaRateDefs oldMemberOrderCommission = findServiceManagerOrderCommissionRate(
                        VegaRateType.OLD_MEMBER_ORDER_COMMISSION.type(), VegaRateType.OLD_MEMBER_ORDER_COMMISSION.rateName());

                shopOrderList.getResult().forEach(shopOrder -> {
                    Long oldValue = 0L, newValue = 0L;
                    if(checkShopIsOldMemberByUserId(shopOrder.getBuyerId())) {
                        // 老会员
                        oldValue = Math.round(ArithUtil.div(
                                ArithUtil.mul(shopOrder.getFee().doubleValue(), oldMemberOrderCommission.getRateKey().doubleValue()),
                                oldMemberOrderCommission.getRateBase().doubleValue()));
                    } else {
                        // 新会员
                        newValue = Math.round(ArithUtil.div(
                                ArithUtil.mul(shopOrder.getFee().doubleValue(), newMemberOrderCommission.getRateKey().doubleValue()),
                                newMemberOrderCommission.getRateBase().doubleValue()));
                    }

                    ServiceManagerOrderDto serviceManagerOrderDto = new ServiceManagerOrderDto();
                    serviceManagerOrderDto.setOrderId(shopOrder.getId());
                    serviceManagerOrderDto.setBuyerId(shopOrder.getBuyerId());
                    serviceManagerOrderDto.setFee(shopOrder.getFee());
                    serviceManagerOrderDto.setServiceManagerId(serviceManagerMap.get(shopOrder.getBuyerId()).getServiceManagerId());
                    serviceManagerOrderDto.setServiceManagerName(serviceManagerMap.get(shopOrder.getBuyerId()).getServiceManagerName());
                    serviceManagerOrderDto.setNewMemberCommission(newValue);
                    serviceManagerOrderDto.setOldMemberCommission(oldValue);
                    serviceManagerOrderDto.setTotalMemberCommission(oldValue + newValue);

                    serviceManagerOrderDtos.add(serviceManagerOrderDto);
                });
            }

            return getListByGroup(serviceManagerOrderDtos);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("find service manager order failed, serviceManagerIds:{}, startAt:{}, endAt:{}",
                    serviceManagerIds, startAt, endAt);
            throw new JsonResponseException("find.service.manager.order.fail");
        }
    }

    /**
     * 根据主键进行分组求和
     * @param list 分组之前List
     * @return 分组之后List
     */
    private List<ServiceManagerOrderDto> getListByGroup(List<ServiceManagerOrderDto> list) {

        // 如果需要sum多个字段，可以定义 key value(object) Map<Long, object> map
        Map<Long, ServiceManagerOrderDto> map = Maps.newHashMap();
        for (ServiceManagerOrderDto bean : list) {
            // 如果需要group by 多个字段，对应key=字段a+字段b...
            Long key = bean.getServiceManagerId();
            ServiceManagerOrderDto mapValue = new ServiceManagerOrderDto();

            if (map.containsKey(key)) {
                mapValue.setServiceManagerId(key); //业务经理ID,主键
                mapValue.setOldMemberCommission(map.get(key).getOldMemberCommission() + bean.getOldMemberCommission()); // 老会员提成
                mapValue.setNewMemberCommission(map.get(key).getNewMemberCommission() + bean.getNewMemberCommission()); // 新会员提成
                mapValue.setTotalMemberCommission(map.get(key).getTotalMemberCommission() + bean.getTotalMemberCommission()); // 总提成
            } else {
                mapValue.setServiceManagerId(key); //业务经理ID,主键
                mapValue.setServiceManagerName(bean.getServiceManagerName()); // 业务经理姓名
                mapValue.setOldMemberCommission(bean.getOldMemberCommission()); // 老会员提成
                mapValue.setNewMemberCommission(bean.getNewMemberCommission()); // 新会员提成
                mapValue.setTotalMemberCommission(bean.getTotalMemberCommission());// 总提成
            }
            map.put(bean.getServiceManagerId(), mapValue);
        }

        // 放入list
        List<ServiceManagerOrderDto> result = Lists.newArrayList();
        for (Map.Entry<Long, ServiceManagerOrderDto> entry : map.entrySet()) {
            result.add(entry.getValue());
        }
        return result;
    }

    /**
     * 获取业务经理订单提成费率
     * @param type 类型
     * @param name 名称
     * @return 费率定义详情
     */
    private VegaRateDefs findServiceManagerOrderCommissionRate(Integer type, String name) {
        Response<VegaRateDefs> resp = vegaRateDefsReadService.findByTypeAndName(type, name);
        if (!resp.isSuccess()) {
            log.error("failed to find service manager order commission rate by type = {}, name = {}, " +
                    "cause : {}", type, name, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 根据用户ID判断是否经销商三力士老会员用户
     *
     * @param userId 用户Id
     * @return 是否
     */
    private Boolean checkShopIsOldMemberByUserId(Long userId) {
        Response<User> resp = userReadService.findById(userId);
        if (!resp.isSuccess()) {
            log.error("failed to find user by id = {}, cause : {}", userId, resp.getError());
            return Boolean.FALSE;
        }
        if(!resp.getResult().getRoles().contains(VegaUserRole.DEALER_FIRST.name()) &&
                !resp.getResult().getRoles().contains(VegaUserRole.DEALER_SECOND.name())){
            // 非经销商用户
            return Boolean.FALSE;
        }

        Response<VegaShop> shopRes = vegaShopReadService.findByUserId(userId);
        if (!shopRes.isSuccess() || Arguments.isNull(shopRes.getResult())) {
            log.error("find shop by user id:{} fail,error:{}", userId, shopRes.getError());
            return Boolean.FALSE;
        }
        if (Objects.equals(shopRes.getResult().getShopExtra().getIsOldMember(), DefaultId.DEFAULT_TRUE_ID)) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    /**
     * 截止时间
     *
     * @param endAt 截止时间
     * @return date
     */
    private Date endDate(String endAt) {
        return Strings.isNullOrEmpty(endAt) ? null : DateTime.parse(endAt).plusDays(1).toDate();
    }

    /**
     * 起始时间
     *
     * @param startAt 起始时间
     * @return date
     */
    private Date startDate(String startAt) {
        return Strings.isNullOrEmpty(startAt) ? null : DateTime.parse(startAt).toDate();
    }
}
