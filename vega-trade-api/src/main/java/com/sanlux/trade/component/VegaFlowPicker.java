/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.trade.component;

import com.sanlux.common.enums.VegaUserRole;
import com.sanlux.trade.dto.fsm.VegaFlowBook;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.order.api.FlowPicker;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

/**
 * 三力士订单流程
 *
 * Author:  songrenfei
 * Date: 2016-05-13
 */
@Slf4j
public class VegaFlowPicker implements FlowPicker, Serializable {

    private static final long serialVersionUID = -9124641500024566643L;

    /**
     * 根据(子)订单id和级别来决定选择哪个订单流程
     *
     * @param order   (子)订单, 可能是shopOrder, 也可能是skuOrder, 根据OrderLevel决定
     * @param orderLevel 订单级别
     * @return 对应的流程
     */
    @Override
    public Flow pick(OrderBase order, OrderLevel orderLevel) {

        //根据订单信息判断出走哪套流程 不用的买家身份走不通的流程
        Map<String, String> tagMap =  order.getTags();
        String roleName = tagMap.get("roleName");
        if(Objects.equals(roleName, UserRole.BUYER.name())){
            return VegaFlowBook.buyerOrder;
        }

        if(Objects.equals(roleName, VegaUserRole.DEALER_FIRST.name())){
            return VegaFlowBook.firstDealerOrder;
        }

        if(Objects.equals(roleName, VegaUserRole.DEALER_SECOND.name())){
            return VegaFlowBook.secondDealerOrder;
        }

        return null;//todo 空指针


    }
}
