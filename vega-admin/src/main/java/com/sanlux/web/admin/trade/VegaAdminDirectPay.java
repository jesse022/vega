package com.sanlux.web.admin.trade;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Strings;
import com.sanlux.common.enums.VegaShopExtraField;
import com.sanlux.pay.direct.dto.PayBusinessInfo;
import com.sanlux.pay.direct.dto.PayFunctionInfo;
import com.sanlux.pay.direct.dto.PayRequestDto;
import com.sanlux.pay.direct.dto.PayRequestSystemInfo;
import com.sanlux.pay.direct.dto.PayResponseDto;
import com.sanlux.pay.direct.utils.DirectXmlHelper;
import com.sanlux.shop.dto.VegaShop;
import com.sanlux.shop.model.VegaShopExtra;
import com.sanlux.shop.service.VegaShopReadService;
import com.sanlux.trade.enums.VegaDirectPayInfoStatus;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.service.VegaDirectPayInfoWriteService;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import com.sanlux.web.front.core.utils.DircetPayParams;
import com.sanlux.web.front.core.utils.DirectClient;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


/**
 * Created by liangfujie on 16/10/28
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/direct/bank")
public class VegaAdminDirectPay {

    @Autowired
    private DirectClient directClient;

    @Autowired
    private DircetPayParams dircetPayParams;


    @RpcConsumer
    private VegaDirectPayInfoWriteService vegaDirectPayInfoWriteService;


    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService sellerTradeDailySummaryReadService;

    @RpcConsumer
    private VegaShopReadService vegaShopReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");


    @RequestMapping(value = "/pay", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean directPay(Long id) {
        Response<VegaSellerTradeDailySummary> sellerTradeDailySummaryResponse =
                sellerTradeDailySummaryReadService.findSellerTradeDailySummaryById(id);
        if (!sellerTradeDailySummaryResponse.isSuccess()) {
            log.error("find sellerTradeDailySummary  detail failed , cause {}", sellerTradeDailySummaryResponse.getError());
            throw new JsonResponseException("find.seller.trade.detail.failed");
        }
        VegaSellerTradeDailySummary sellerTradeDailySummary = sellerTradeDailySummaryResponse.getResult();
        PayRequestDto payRequestDto = initPayRequestDto();
        PayFunctionInfo payFunctionInfo = payRequestDto.getFunctionInfo();
        double money = sellerTradeDailySummary.getSellerReceivableFee() / 100.0;
        Long shopId = sellerTradeDailySummary.getSellerId();
        Response<VegaShop> vegaShopResponse = vegaShopReadService.findByShopId(shopId);
        if (!vegaShopResponse.isSuccess()) {
            log.error("vega shop extra find failed,cause{}", vegaShopResponse.getError());
            throw new JsonResponseException("vega.shop.extra.find.fail");
        }
        VegaShop vegaShop = vegaShopResponse.getResult();
        VegaShopExtra vegaShopExtra = vegaShop.getShopExtra();
        Response<Shop> shopResponse = shopReadService.findById(shopId);
        if (!shopResponse.isSuccess()) {

            log.error(" shop  find failed,cause{}", vegaShopResponse.getError());
            throw new JsonResponseException("vega.shop.find.fail");
        }

        Shop shop = shopResponse.getResult();
        payFunctionInfo.setTRSAMT(String.valueOf(money));
        payFunctionInfo.setCRTACC(vegaShopExtra.getBankAccount());
        payFunctionInfo.setCRTNAM(shop.getExtra().get(VegaShopExtraField.RECEIVER_ACCOUNT_NAME.getName()));
        payFunctionInfo.setBRDNBR(shop.getExtra().get(VegaShopExtraField.RECEIVER_BANK_CODE.getName()));
        payFunctionInfo.setBNKFLG(shop.getExtra().get(VegaShopExtraField.IS_CMBC.getName()));
        payFunctionInfo.setCRTBNK(shop.getExtra().get(VegaShopExtraField.RECEIVER_BANK.getName()));
        payFunctionInfo.setCRTPVC(shop.getExtra().get(VegaShopExtraField.RECEIVER_BANK_PROVINCE.getName()));
        payFunctionInfo.setCRTCTY(shop.getExtra().get(VegaShopExtraField.RECEIVER_BANK_CITY.getName()));
        String expectDate = DATE_FORMAT.format(new Date()).substring(0, 8);
        payFunctionInfo.setEPTDAT(expectDate);
        String businessId = getBusinessId(id);
        payFunctionInfo.setYURREF(businessId);
        String responseStr = "";
        try {
            responseStr = directClient.postXmlToDirectSystem(DirectXmlHelper.BeanToXml(payRequestDto));

        } catch (Exception e) {
            log.error("direct pay response{}", responseStr);
            throw new JsonResponseException("connect.bank.server.failed");
        }
        PayResponseDto payResponseDto = DirectXmlHelper.xmlToBean(responseStr, PayResponseDto.class);
        VegaDirectPayInfo vegaDirectPayInfo = new VegaDirectPayInfo();
        if (payResponseDto != null) {
            int returnCode = Integer.valueOf(payResponseDto.getPayResponseSystemInfo().getRETCOD());
            if (returnCode == -9 || returnCode == -1) {
                log.error("direct pay maybe has error,please check ,payRequestDto{},payResponse{}", DirectXmlHelper.BeanToXml(payRequestDto), responseStr);
                throw new InvalidException(500, "direct.pay.maybe.has.error.{0}",
                        payResponseDto.getPayResponseSystemInfo().getERRMSG());
            } else if (returnCode == 0) {
                if (payResponseDto.getPayResponseBody().getREQSTS().trim().equals("FIN") &&
                        payResponseDto.getPayResponseBody().getRTNFLG().trim().equals("F")) {
                    log.error("direct pay  has error,please submit again ,payRequestDto{},payResponse{}", DirectXmlHelper.BeanToXml(payRequestDto), responseStr);
                    throw new InvalidException(500, "direct.pay.has.error.{0}",
                            payResponseDto.getPayResponseSystemInfo().getERRMSG());

                } else {
                    vegaDirectPayInfo.setBusinessId(payRequestDto.getFunctionInfo().getYURREF());
                    vegaDirectPayInfo.setStatus(VegaDirectPayInfoStatus.WAIT_APPROVE.value());
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("sellerReceivableFee", String.valueOf(money * 100));
                    try {
                        String extra = JsonMapper.nonEmptyMapper().getMapper().writeValueAsString(map);
                        vegaDirectPayInfo.setExtraJson(extra);
                    } catch (JsonProcessingException e) {
                        log.error("sellerReceivableFee write failed,cause{}", e.getMessage());
                        throw new JsonResponseException("seller.receivable.fee.write.failed");
                    }
                    vegaDirectPayInfo.setOrderId(id);

                    // 汇总信息更细打款状态
                    Integer transStatus = VegaDirectPayInfoStatus.WAIT_APPROVE.value();
                    sellerTradeDailySummary.getExtra().put("transStatus", String.valueOf(transStatus));
                    sellerTradeDailySummary.setExtra(sellerTradeDailySummary.getExtra());
                    sellerTradeDailySummary.setTransStatus(transStatus);

                    Response<Boolean> response = vegaDirectPayInfoWriteService.create(vegaDirectPayInfo, sellerTradeDailySummary);
                    if (!response.isSuccess()) {
                        log.error("create vega direct info failed , cause {}", response.getError());
                        throw new JsonResponseException("create.vega.direct.info.failed ");
                    }
                    return response.getResult();
                }

            } else {
                log.error(" direct pay failed,payRequestDto{},payResponse{}", DirectXmlHelper.BeanToXml(payRequestDto), responseStr);
                throw new InvalidException(500, "direct.pay.failed.{0}",
                        payResponseDto.getPayResponseSystemInfo().getERRMSG());
            }
        } else {
            log.error("post xml to direct system failed ");
            throw new JsonResponseException("post.direct.pay.failed");

        }

    }

    private PayRequestDto initPayRequestDto() {
        PayRequestDto payRequestDto = new PayRequestDto();
        PayRequestSystemInfo payRequestSystemInfo = new PayRequestSystemInfo();
        PayFunctionInfo payFunctionInfo = new PayFunctionInfo();
        PayBusinessInfo payBusinessInfo = new PayBusinessInfo();
        payRequestSystemInfo.setLGNNAM(dircetPayParams.getLGNNAM());
        payRequestSystemInfo.setDATTYP(dircetPayParams.getDATTYP());
        payRequestSystemInfo.setFUNNAM(dircetPayParams.getFUNNAM());
        payBusinessInfo.setBUSCOD(dircetPayParams.getBUSCOD());
        payBusinessInfo.setBUSMOD(dircetPayParams.getBUSMOD());
        payFunctionInfo.setCCYNBR(dircetPayParams.getCCYNBR());
        payFunctionInfo.setSTLCHN(dircetPayParams.getSTLCHN());
        payFunctionInfo.setNUSAGE(dircetPayParams.getNUSAGE());
        payFunctionInfo.setBUSNAR(dircetPayParams.getBUSNAR());
        payFunctionInfo.setDBTACC(dircetPayParams.getDBTACC());
        payFunctionInfo.setDBTBBK(dircetPayParams.getDBTBBK());
        payRequestDto.setSystemInfo(payRequestSystemInfo);
        payRequestDto.setFunctionInfo(payFunctionInfo);
        payRequestDto.setBusinessInfo(payBusinessInfo);
        return payRequestDto;
    }


    /**
     * 生成业务实例号
     *
     * @param orderId 订单ID
     * @return 业务实例号
     */

    public static String getBusinessId(Long orderId) {
        String prefix = DATE_FORMAT.format(new Date());
        String suffix = orderId.toString();
        Integer trackLength = suffix.length();
        String padding = Strings.repeat("0", 16 - trackLength);
        return prefix + padding + suffix;
    }

}
