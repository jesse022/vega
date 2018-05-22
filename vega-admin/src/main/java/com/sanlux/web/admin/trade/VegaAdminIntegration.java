package com.sanlux.web.admin.trade;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.trade.dto.IntegrationOrderCriteria;
import com.sanlux.trade.enums.IntegrationOrderStatus;
import com.sanlux.trade.model.IntegrationOrder;
import com.sanlux.trade.service.IntegrationOrderReadService;
import com.sanlux.trade.service.IntegrationOrderWriteService;
import com.sanlux.web.front.core.utils.ExportHelper;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;


/**
 * Created by cuiwentao
 * on 16/10/11
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/vega/integration/order")
public class VegaAdminIntegration {

    @RpcConsumer
    private IntegrationOrderReadService integrationOrderReadService;

    @RpcConsumer
    private IntegrationOrderWriteService integrationOrderWriteService;


    @RequestMapping(value = "/paging", method = RequestMethod.GET)
    public Paging<IntegrationOrder> orderPaging ( IntegrationOrderCriteria criteria) {
        Response<Paging<IntegrationOrder>> response = integrationOrderReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("paging integration order fail,criteria:{}, cause:{}",
                    criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/detail", method = RequestMethod.GET)
    public IntegrationOrder findIntegrationOrder (@RequestParam("id") Long id) {
        Response<IntegrationOrder> response = integrationOrderReadService.findById(id);
        if (!response.isSuccess()) {
            log.error("find integration order by id:{} fail, cause:{}",
                    id, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public Boolean delivery(@RequestParam(value = "deliveryCompany", required = true)String deliveryCompany,
                            @RequestParam(value = "deliveryNo", required = true) String deliveryNo,
                            @RequestParam(value = "id",required = true) Long id) {
        Response<Boolean> response = integrationOrderWriteService.delivery(id, deliveryCompany, deliveryNo);
        if (!response.isSuccess()) {
            log.error("delivery integration order fail, id:{}, deliveryCompany:{}, deliveryNo:{}, cause:{}",
                    id, deliveryCompany, deliveryNo, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @RequestMapping(value = "/export")
    public void exportOrderInfo( IntegrationOrderCriteria criteria,
                                HttpServletResponse httpServletResponse) {
        Response<Paging<IntegrationOrder>> pagingResponse = integrationOrderReadService.paging(criteria);

        if (!pagingResponse.isSuccess()) {
            log.error("integration order find  failed");
            throw new JsonResponseException("integration.order.find.fail");
        }
        List<IntegrationOrder> orderList = pagingResponse.getResult().getData();
        try {

            String xlsFileName = URLEncoder.encode("积分商品兑换记录表", "UTF-8") + ".xlsx";
            httpServletResponse.setContentType("application/x-download");
            String headerKey = "Content-Disposition";
            String headerValue = String.format("attachment; filename=\"%s\"", xlsFileName);
            httpServletResponse.setHeader(headerKey, headerValue);
            buildAdminOrderTemplateFile(orderList, httpServletResponse.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("download the Excel of order info failed ");

        }


    }

    //创建表单
    private void buildAdminOrderTemplateFile(List<IntegrationOrder> integrationOrderLists, OutputStream outputStream) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("订单号", 18 * 100);
            columnMaps.put("兑换人", 18 * 400);
            columnMaps.put("手机号", 18 * 400);
            columnMaps.put("兑换商品名称", 18 * 400);
            columnMaps.put("数量", 18 * 100);
            columnMaps.put("收货地址", 18 * 400);
            columnMaps.put("订单状态", 18 * 150);
            XSSFSheet xssfSheet = xssfWorkbook.createSheet("积分商品兑换记录表");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            if (integrationOrderLists != null && integrationOrderLists.size() > 0) {
                for (int i = 0; i < integrationOrderLists.size(); i++) {
                    Row row = xssfSheet.createRow(i + 1);
                    ExportHelper.setContent(row, getOrderContent(integrationOrderLists.get(i)));
                }
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("export order info fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }


    //转换订单信息
    private List<String> getOrderContent(IntegrationOrder integrationOrder) {
        List<String> contents = Lists.newArrayList();
        contents.add(Arguments.isNull(integrationOrder.getId()) ? "" : integrationOrder.getId().toString());
        contents.add(Strings.isNullOrEmpty(integrationOrder.getBuyerName()) ? "" : integrationOrder.getBuyerName());
        contents.add(Strings.isNullOrEmpty(integrationOrder.getBuyerPhone()) ? "" : integrationOrder.getBuyerPhone());
        contents.add(Strings.isNullOrEmpty(integrationOrder.getItemName()) ? "" : integrationOrder.getItemName());
        contents.add(Arguments.isNull(integrationOrder.getQuantity()) ? "" : integrationOrder.getQuantity().toString());
        contents.add(Strings.isNullOrEmpty(integrationOrder.getAddressInfoJson()) ? "" : integrationOrder.getAddressInfoJson());

        if (integrationOrder.getStatus() == IntegrationOrderStatus.DONE.value()) {
            contents.add(String.valueOf("已发货"));
        } else if (integrationOrder.getStatus() == IntegrationOrderStatus.WAIT_DELIVERY.value()) {
            contents.add(String.valueOf("待发货"));
        } else if (Arguments.isNull(integrationOrder.getStatus())) {
            contents.add("");
        }
        return contents;
    }

}
