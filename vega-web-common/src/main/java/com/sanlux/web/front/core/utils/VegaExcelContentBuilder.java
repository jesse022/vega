/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.sanlux.web.front.core.utils;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import com.sanlux.common.constants.SettleConstants;
import com.sanlux.trade.enums.VegaDirectPayInfoStatus;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.model.VegaSettleOrderDetail;
import com.sanlux.trade.settle.model.VegaSettleRefundOrderDetail;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.NumberUtils;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.settle.enums.CheckStatus;
import io.terminus.parana.settle.enums.TradeType;
import io.terminus.parana.settle.model.PayChannelDailySummary;
import io.terminus.parana.settle.model.PayChannelDetail;
import io.terminus.parana.settle.model.PlatformTradeDailySummary;
import io.terminus.parana.settle.model.SettleOrderDetail;
import io.terminus.parana.settle.model.SettleRefundOrderDetail;
import io.terminus.parana.settle.model.Settlement;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * @author : panxin
 */
public class VegaExcelContentBuilder { //extends ExcelContentBuildUtil {

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    public static List<String> buildPayChannelDetailContent(PayChannelDetail base) {
        List<String> list = Lists.newArrayList();
        list.add(DFT.print(new DateTime(base.getTradeFinishedAt())));
        list.add(base.getChannel());
        list.add(NumberUtils.formatPrice(base.getTradeFee()));
        list.add(NumberUtils.formatPrice(base.getGatewayCommission()));
        list.add(base.getGatewayRate().toString());
        list.add(NumberUtils.formatPrice(base.getActualIncomeFee()));
        list.add(getTradeType(base.getTradeType()));
        list.add(base.getTradeNo());
        list.add(base.getGatewayTradeNo());
        list.add(getCheckStatus(base.getCheckStatus()));
        return list;
    }

    private static String getTradeType(Integer tradeType) {
        TradeType type = TradeType.from(tradeType);
        return Arguments.isNull(type) ? "未知" : (type.equals(TradeType.Pay) ? "支付" : "退款");
    }

    private static String getOrderType(Integer orderType) {
        return OrderLevel.fromInt(orderType).equals(OrderLevel.SHOP) ? "店铺订单" : "SKU订单";
    }

    private static String getCheckStatus(Integer status) {
        CheckStatus status1 = CheckStatus.from(status);
        return Arguments.isNull(status1) ? "未知" : status1.toString();
    }

    public static List<String> buildSettlementContent(Settlement base) {
        List<String> list = Lists.newArrayList();
        list.add(DFT.print(new DateTime(base.getCreatedAt())));
        list.add(DFT.print(new DateTime(base.getTradeFinishedAt())));
        list.add(base.getChannel());
        list.add(NumberUtils.formatPrice(base.getOriginFee()));
        list.add(NumberUtils.formatPrice(base.getSellerDiscount()));
        list.add(NumberUtils.formatPrice(base.getPlatformDiscount()));
        list.add(NumberUtils.formatPrice(base.getShipFee()));
        list.add(NumberUtils.formatPrice(base.getSellerDiscount()));
        list.add(NumberUtils.formatPrice(base.getActualFee()));
        list.add(NumberUtils.formatPrice(base.getGatewayCommission()));
        list.add(NumberUtils.formatPrice(base.getPlatformCommission()));
        list.add(getTradeType(base.getTradeType()));
        list.add(base.getTradeNo());
        list.add(base.getGatewayTradeNo());
        list.add(base.getOrderIds());
        list.add(base.getPaymentOrRefundId().toString());
        list.add(getCheckStatus(base.getCheckStatus()));
        list.add(DFT.print(new DateTime(base.getCheckFinishedAt())));
        return list;
    }

    public static List<String> buildPayChannelDailySummary(PayChannelDailySummary base) {
        List<String> list = Lists.newArrayList();
        list.add(DFT.print(new DateTime(base.getCreatedAt())));
        list.add(base.getChannel());
        list.add(NumberUtils.formatPrice(base.getTradeFee()));
        list.add(NumberUtils.formatPrice(base.getGatewayCommission()));
        list.add(NumberUtils.formatPrice(base.getNetIncomeFee()));
        return list;
    }

    public static List<String> buildSettleOrderDetail(SettleOrderDetail base,
                                                      Boolean exportIsAdmin) {
        List<String> list = Lists.newArrayList();

        list.add(DFT.print(new DateTime(base.getPaidAt())));
        list.add(DFT.print(new DateTime(base.getCheckAt())));
        list.add(String.valueOf(base.getTradeNo()));
        if (exportIsAdmin) {
            list.add(((VegaSettleOrderDetail) base).getSellerType());
            list.add(base.getSellerName());
        }
        list.add(NumberUtils.formatPrice(base.getOriginFee()));
        list.add(NumberUtils.formatPrice(base.getShipFee()));
        list.add(NumberUtils.formatPrice(base.getActualPayFee()));
        list.add(ChannelEnums.from(base.getChannel()).toString());
        list.add(NumberUtils.formatPrice(-base.getGatewayCommission()));
        list.add(NumberUtils.formatPrice(-base.getCommission1()));
        list.add(NumberUtils.formatPrice(-base.getPlatformCommission()));
        list.add(NumberUtils.formatPrice(base.getSellerReceivableFee()));

        return list;
    }

    public static List<String> buildSettleRefundOrderDetail(SettleRefundOrderDetail base,
                                                            Boolean exporterIsAdmin) {
        List<String> list = Lists.newArrayList();

        list.add(DFT.print(new DateTime(base.getRefundAt())));
        if (exporterIsAdmin) {
            list.add(((VegaSettleRefundOrderDetail) base).getSellerType());
            list.add(base.getSellerName());
        }
        list.add(String.valueOf(base.getOrderId()));
        list.add(String.valueOf(base.getRefundId()));
        list.add(NumberUtils.formatPrice(-base.getOriginFee()));
        list.add(NumberUtils.formatPrice(base.getPlatformDiscount()));
        list.add(NumberUtils.formatPrice(-base.getShipFee()));
        list.add(NumberUtils.formatPrice(-base.getActualRefundFee()));
        list.add(NumberUtils.formatPrice(base.getCommission1()));
        list.add(NumberUtils.formatPrice(base.getPlatformCommission()));
        list.add(NumberUtils.formatPrice(base.getSellerDeductFee()));

        return list;
    }

    public static List<String> buildSellerTradeDailySummary(VegaSellerTradeDailySummary base,
                                                            Boolean isNeedTransStatus) {
        List<String> list = Lists.newArrayList();
        list.add(DFT.print(new DateTime(base.getSumAt())));
        if (isNeedTransStatus) {
            list.add(base.getSellerName());
        }
        list.add(String.valueOf(base.getOrderCount()));
        list.add(String.valueOf(base.getRefundOrderCount()));
        list.add(NumberUtils.formatPrice(base.getOriginFee()));
        list.add(NumberUtils.formatPrice(-base.getPlatformDiscount()));
        list.add(NumberUtils.formatPrice(-base.getRefundFee()));
        list.add(NumberUtils.formatPrice(base.getShipFee()));
        list.add(NumberUtils.formatPrice(base.getActualPayFee()));
        Long gatewayComm = MoreObjects.firstNonNull(base.getGatewayCommission(), 0L);
        list.add(NumberUtils.formatPrice(gatewayComm == 0 ? 0 : -gatewayComm));
        list.add(NumberUtils.formatPrice(-base.getCommission1()));
        list.add(NumberUtils.formatPrice(-base.getPlatformCommission()));
        list.add(NumberUtils.formatPrice(base.getSellerReceivableFee()));
        if (isNeedTransStatus) {
            list.add(VegaDirectPayInfoStatus.from(checkTransStatus(base)).toString());
        }
        return list;
    }

    public static List<String> buildPlatformTradeDailySummary(PlatformTradeDailySummary base) {
        List<String> list = Lists.newArrayList();

        list.add(DFT.print(new DateTime(base.getSumAt())));
        list.add(String.valueOf(base.getOrderCount()));
        list.add(String.valueOf(base.getRefundOrderCount()));
        list.add(NumberUtils.formatPrice(base.getOriginFee()));
        list.add(NumberUtils.formatPrice(-base.getRefundFee()));
        list.add(NumberUtils.formatPrice(base.getShipFee()));
        list.add(NumberUtils.formatPrice(base.getActualPayFee()));
        Long gatewayComm = MoreObjects.firstNonNull(base.getGatewayCommission(), 0L);
        list.add(NumberUtils.formatPrice(gatewayComm == 0 ? 0 : -gatewayComm));
        list.add(NumberUtils.formatPrice(-base.getCommission1()));
        list.add(NumberUtils.formatPrice(-base.getPlatformCommission()));
        list.add(NumberUtils.formatPrice(base.getSellerReceivableFee()));

        return list;
    }

    /**
     * 打款状态
     *
     * @param base 信息
     * @return 状态
     */
    private static int checkTransStatus(VegaSellerTradeDailySummary base) {
        return Integer.valueOf(base.getExtra().get(SettleConstants.TRANS_STATUS));
    }

}
