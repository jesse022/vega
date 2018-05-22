package com.sanlux.web.front.core.report;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.sanlux.common.helper.DateHelper;
import com.sanlux.trade.dto.*;
import com.sanlux.trade.enums.VegaOrderChannelEnum;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.util.List;

/**
 * 运营报表业务逻辑处理类
 *
 * Created by lujm on 2017/9/25.
 */
@Component
@Slf4j
public class VegaReportComponent {
    @RpcConsumer
    BackCategoryCacher backCategoryCacher;
    @RpcConsumer
    ItemReadService itemReadService;

    /**
     * 运营销售情况汇总报表
     * @param vegaOrderDetails 订单信息
     * @return 报表数据
     */
    public List<ReportSalesDto> getOperateReportByDate(List<VegaOrderDetail> vegaOrderDetails) {
        List<ReportSalesDto> reportSalesDtos = Lists.newArrayList();
        for (VegaOrderDetail vegaOrderDetail : vegaOrderDetails) {
            ReportSalesDto reportSalesDto = new ReportSalesDto();
            reportSalesDto.setSummaryDate(DateHelper.formatDate(vegaOrderDetail.getShopOrder().getCreatedAt())); // 下单日期
            reportSalesDto.setOrderSum(1); // 订单数量,固定值
            reportSalesDto.setMemberSum(1); // 会员数量,固定值
            reportSalesDto.setSalesVolumeSum(vegaOrderDetail.getShopOrder().getFee()); // 订单总金额,含运费 // TODO: 2017/9/25 是否包含运费? 和明细对不上

            List<ReportTerminalDto> reportTerminalDtos = Lists.newArrayList();
            for ( VegaOrderChannelEnum vegaOrderChannelEnum : VegaOrderChannelEnum.values() ) {
                ReportTerminalDto reportTerminalDto = new ReportTerminalDto();
                reportTerminalDto.setTerminalId(vegaOrderChannelEnum.value());
                reportTerminalDto.setTerminalType(vegaOrderChannelEnum.toString());
                if (Objects.equal(vegaOrderDetail.getShopOrder().getChannel(), vegaOrderChannelEnum.value())) {
                    reportTerminalDto.setSum(1); // 访客终端数量
                } else {
                    reportTerminalDto.setSum(0); // 访客终端数量,默认为0
                }
                reportTerminalDtos.add(reportTerminalDto);
            }
            reportSalesDto.setReportTerminalDtos(reportTerminalDtos);


            List<ReportCategoryDto> reportCategoryDtos = Lists.newArrayList();
            List<BackCategory> backCategories = backCategoryCacher.findChildrenOf(0L);
            if (!Arguments.isNullOrEmpty(backCategories)) {
                for (BackCategory backCategory : backCategories) {
                    ReportCategoryDto reportCategoryDto = new ReportCategoryDto();
                    reportCategoryDto.setCategoryId(backCategory.getId());
                    reportCategoryDto.setCategoryName(backCategory.getName());
                    Long skuOrderDefaultFee = 0L;
                    for (SkuOrder skuOrder : vegaOrderDetail.getSkuOrders()) {
                        if (Objects.equal(findBackCategoryIdItemId(skuOrder.getItemId()), backCategory.getId())) {
                            skuOrderDefaultFee = skuOrderDefaultFee + skuOrder.getFee();
                        }
                    }
                    reportCategoryDto.setCategoryFee(skuOrderDefaultFee);
                    reportCategoryDtos.add(reportCategoryDto);
                }
            }
            reportSalesDto.setReportCategoryDtos(reportCategoryDtos);

            reportSalesDtos.add(reportSalesDto);
        }

        return reportSalesDtos;
    }

    /**
     * 服务商每日销售报表汇总
     * @param vegaOrderDetails 订单信息
     * @return 报表数据
     */
    public List<ReportDealerSalesDto> getDealerSalesReportByDate(List<VegaOrderDetail> vegaOrderDetails) {
        List<ReportDealerSalesDto> reportDealerSalesDtoList = Lists.newArrayList();

        for (VegaOrderDetail vegaOrderDetail : vegaOrderDetails) {
            ReportDealerSalesDto reportDealerSalesDto = new ReportDealerSalesDto();

            reportDealerSalesDto.setOrderSum(1); // 订单数量,固定值
            reportDealerSalesDto.setSummaryDate(DateHelper.formatDate(vegaOrderDetail.getShopOrder().getCreatedAt())); // 下单日期
            reportDealerSalesDto.setSalesVolumeSum(vegaOrderDetail.getShopOrder().getFee()); // 订单总金额,含运费
            reportDealerSalesDto.setShopId(vegaOrderDetail.getShopOrder().getShopId());
            reportDealerSalesDto.setShopName(vegaOrderDetail.getShopOrder().getShopName());

            reportDealerSalesDtoList.add(reportDealerSalesDto);

        }
        return reportDealerSalesDtoList;
    }


    /**
     * 根据商品Id获取后台一级类目ID
     *
     * @param itemId 商品Id
     * @return 后台一级类目Id
     */
    private Long findBackCategoryIdItemId(Long itemId) {

        Response<Item> itemResponse = itemReadService.findById(itemId);
        if (!itemResponse.isSuccess()) {
            log.error("fail to find item detail by itemId:{},cause:{}", itemId, itemResponse.getError());
            return null;
        }
        Long backCategoryId = itemResponse.getResult().getCategoryId();
        List<BackCategory> backCategories = backCategoryCacher.findAncestorsOf(backCategoryId);
        if (Arguments.isNullOrEmpty(backCategories)) {
            return null;
        }

        for (BackCategory backCategory : backCategories) {
            if (Objects.equal(backCategory.getLevel(), 1)) {
                return backCategory.getId();
            }
        }
        return null;
    }


}
