package com.sanlux.web.admin.trade.job;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import com.sanlux.pay.direct.dto.QueryRequestBody;
import com.sanlux.pay.direct.dto.QueryRequestDto;
import com.sanlux.pay.direct.dto.QueryRequestSystemInfo;
import com.sanlux.pay.direct.dto.QueryResponseDto;
import com.sanlux.trade.settle.model.VegaSellerTradeDailySummary;
import com.sanlux.trade.settle.service.VegaSellerTradeDailySummaryReadService;
import com.sanlux.web.front.core.utils.DirectClient;
import com.sanlux.pay.direct.utils.DirectXmlHelper;
import com.sanlux.trade.enums.VegaDirectPayInfoStatus;
import com.sanlux.trade.model.VegaDirectPayInfo;
import com.sanlux.trade.service.VegaDirectPayInfoReadService;
import com.sanlux.trade.service.VegaDirectPayInfoWriteService;
import com.sanlux.web.front.core.utils.DirectQueryParams;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.settle.model.SellerTradeDailySummary;
import io.terminus.parana.settle.service.SellerTradeDailySummaryReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by liangfujie on 16/10/28
 */
@Slf4j
@Component
public class VegaDirectQueryJob {

    @RpcConsumer
    VegaDirectPayInfoReadService vegaDirectPayInfoReadService;

    @RpcConsumer
    VegaDirectPayInfoWriteService vegaDirectPayInfoWriteService;


    @RpcConsumer
    private VegaSellerTradeDailySummaryReadService VegaSellerTradeDailySummaryReadService;


    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private DirectClient directClient;

    @Autowired
    private DirectQueryParams directQueryParams;

    private static final Integer BATCH_SIZE = 100;     // 批处理数量

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    private static DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    @Scheduled(cron = "0 */10 * * * ? ")
    public void handleOrderStatus() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("[CRON-JOB] [HANDLE-ORDER-STATUS-START] begin {}", DFT.print(DateTime.now()));
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<VegaDirectPayInfo> vegaDirectPayInfoList = Lists.newArrayList();
        int pageNo = 1;
        int pageSize = BATCH_SIZE;
        Boolean nextTag = handleWaitApprove(pageNo, pageSize, vegaDirectPayInfoList);
        while (nextTag) {
            pageNo++;
            nextTag = handleWaitApprove(pageNo, pageSize, vegaDirectPayInfoList);
        }
        nextTag = handleWaitBankPay(pageNo, pageSize, vegaDirectPayInfoList);
        while (nextTag) {
            pageNo++;
            nextTag = handleWaitBankPay(pageNo, pageSize, vegaDirectPayInfoList);
        }
        for (VegaDirectPayInfo vegaDirectPayInfo : vegaDirectPayInfoList) {
            QueryRequestDto queryRequestDto = new QueryRequestDto();
            QueryRequestBody queryRequestBody = new QueryRequestBody();
            QueryRequestSystemInfo queryRequestSystemInfo = new QueryRequestSystemInfo();
            queryRequestSystemInfo.setLGNNAM(directQueryParams.getLGNNAM());
            queryRequestSystemInfo.setDATTYP(directQueryParams.getDATTYP());
            queryRequestSystemInfo.setFUNNAM(directQueryParams.getFUNNAM());
            queryRequestBody.setYURREF(vegaDirectPayInfo.getBusinessId());
            queryRequestBody.setDATFLG(directQueryParams.getDATFLG());
            Date createdAt = vegaDirectPayInfo.getCreatedAt();
            Long orderId = vegaDirectPayInfo.getOrderId();
            Response<VegaSellerTradeDailySummary> vegaSellerTradeDailySummaryResponse = VegaSellerTradeDailySummaryReadService.findSellerTradeDailySummaryById(orderId);
            if (!vegaSellerTradeDailySummaryResponse.isSuccess()) {
                log.error("find sellerTradeDailySummary  detail failed , cause {}", vegaSellerTradeDailySummaryResponse.getError());
                return;
            }
            VegaSellerTradeDailySummary vegaSellerTradeDailySummary = vegaSellerTradeDailySummaryResponse.getResult();
            queryRequestBody.setBGNDAT(dateFormat.format(createdAt));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(createdAt);
            calendar.add(Calendar.DATE, 1);
            queryRequestBody.setENDDAT(dateFormat.format(calendar.getTime()));
            queryRequestDto.setQueryRequestBody(queryRequestBody);
            queryRequestDto.setQueryRequestSystemInfo(queryRequestSystemInfo);
            String questStr = DirectXmlHelper.BeanToXml(queryRequestDto);
            String responseStr = null;
            try {
                responseStr = directClient.postXmlToDirectSystem(questStr);
            } catch (Exception e) {
                log.error("connect direct bank server failed , cause {}", e.getMessage());
                return;
            }
            QueryResponseDto queryResponseDto = (QueryResponseDto) DirectXmlHelper.xmlToBean(responseStr, QueryResponseDto.class);
            if (vegaDirectPayInfo.getBusinessId().equals(queryResponseDto.getQueryResponseBody().getYURREF())) {
                if (queryResponseDto.getQueryResponseBody().getREQSTS().trim().equals("FIN")) {
                    if (queryResponseDto.getQueryResponseBody().getRTNFLG().trim().equals("S")) {
                        if (Arguments.isNull(vegaSellerTradeDailySummary.getExtra())) {
                            log.error("seller  order extra not exist !");
                        } else {
                            vegaSellerTradeDailySummary.getExtra().put("transStatus", String.valueOf(VegaDirectPayInfoStatus.PAY_SUCCESS.value()));
                            vegaSellerTradeDailySummary.setExtra(vegaSellerTradeDailySummary.getExtra());
                            vegaSellerTradeDailySummary.setTransStatus(VegaDirectPayInfoStatus.PAY_SUCCESS.value());
                            vegaDirectPayInfo.setStatus(VegaDirectPayInfoStatus.PAY_SUCCESS.value());
                            Response<Boolean> booleanResponse = vegaDirectPayInfoWriteService.updateVegaDirectPayInfoAndSettleOrderDetail(
                                    vegaDirectPayInfo, vegaSellerTradeDailySummary);

                            if (!booleanResponse.isSuccess()) {
                                log.error("update direct pay info  status failed ,businessId{},cause{}",
                                        vegaDirectPayInfo.getBusinessId(), booleanResponse.getError());
                            }
                        }
                    }
                    if (queryResponseDto.getQueryResponseBody().getRTNFLG().trim().equals("F")) {
                        if (Arguments.isNull(vegaSellerTradeDailySummary.getExtra())) {
                            log.error("seller  order extra not exist !");
                        } else {
                            vegaSellerTradeDailySummary.getExtra().put("transStatus", String.valueOf(VegaDirectPayInfoStatus.PAY_FAILED.value()));
                            vegaDirectPayInfo.setStatus(VegaDirectPayInfoStatus.PAY_FAILED.value());
                            vegaSellerTradeDailySummary.setExtra(vegaSellerTradeDailySummary.getExtra());
                            vegaSellerTradeDailySummary.setTransStatus(VegaDirectPayInfoStatus.PAY_FAILED.value());
                            Response<Boolean> booleanResponse = vegaDirectPayInfoWriteService.updateVegaDirectPayInfoAndSettleOrderDetail(
                                    vegaDirectPayInfo, vegaSellerTradeDailySummary);
                            if (!booleanResponse.isSuccess()) {
                                log.error("update direct pay info  status failed ,businessId{},cause{}",
                                        vegaDirectPayInfo.getBusinessId(), booleanResponse.getError());
                            }
                        }

                    }

                    if (queryResponseDto.getQueryResponseBody().getRTNFLG().trim().equals("R")) {

                        if (Arguments.isNull(vegaSellerTradeDailySummary.getExtra())) {
                            log.error("seller  order extra not exist !");
                        } else {
                            vegaSellerTradeDailySummary.getExtra().put("transStatus", String.valueOf(VegaDirectPayInfoStatus.PAY_REJECT.value()));
                            vegaDirectPayInfo.setStatus(VegaDirectPayInfoStatus.PAY_REJECT.value());
                            vegaSellerTradeDailySummary.setExtra(vegaSellerTradeDailySummary.getExtra());
                            vegaSellerTradeDailySummary.setTransStatus(VegaDirectPayInfoStatus.PAY_REJECT.value());

                            Response<Boolean> booleanResponse = vegaDirectPayInfoWriteService.updateVegaDirectPayInfoAndSettleOrderDetail(
                                    vegaDirectPayInfo, vegaSellerTradeDailySummary);
                            if (!booleanResponse.isSuccess()) {
                                log.error("update direct pay info  status failed ,businessId{},cause{}",
                                        vegaDirectPayInfo.getBusinessId(), booleanResponse.getError());
                            }
                        }
                    }
                } else if (!queryResponseDto.getQueryResponseBody().getREQSTS().trim().equals("FIN") && !queryResponseDto.getQueryResponseBody().getREQSTS().trim().equals("AUT")) {
                    if (Arguments.isNull(vegaSellerTradeDailySummary.getExtra())) {
                        log.error("seller  order extra not exist !");
                    } else {

                        vegaSellerTradeDailySummary.getExtra().put("transStatus", String.valueOf(VegaDirectPayInfoStatus.WAIT_BANK_PAY.value()));
                        vegaDirectPayInfo.setStatus(VegaDirectPayInfoStatus.WAIT_BANK_PAY.value());
                        vegaSellerTradeDailySummary.setExtra(vegaSellerTradeDailySummary.getExtra());
                        vegaSellerTradeDailySummary.setTransStatus(VegaDirectPayInfoStatus.WAIT_BANK_PAY.value());

                        Response<Boolean> booleanResponse = vegaDirectPayInfoWriteService.updateVegaDirectPayInfoAndSettleOrderDetail(
                                vegaDirectPayInfo, vegaSellerTradeDailySummary);
                        if (!booleanResponse.isSuccess()) {
                            log.error("update direct pay info  status failed ,businessId{},cause{}",
                                    vegaDirectPayInfo.getBusinessId(), booleanResponse.getError());
                        }
                    }
                }

            } else {
                log.error("business id not match ,businessId{},YURREF{}", vegaDirectPayInfo.getBusinessId(),
                        queryResponseDto.getQueryResponseBody().getYURREF());
            }

        }

        stopwatch.stop();
        log.info("[CRON-JOB] [HANDLE-ORDER-STATUS-END] done at {} cost {} ms", DFT.print(DateTime.now()), stopwatch.elapsed(TimeUnit.MILLISECONDS));

    }


    public Boolean handleWaitApprove(int pageNo, int size, List<VegaDirectPayInfo> vegaDirectPayInfoList) {
        Response<Paging<VegaDirectPayInfo>> response = vegaDirectPayInfoReadService.pagingByStatus(pageNo, size, Integer.valueOf(VegaDirectPayInfoStatus.WAIT_APPROVE.value()));
        if (!response.isSuccess()) {
            log.error("paging vega direct pay info failed ,cause {}", response.getError());
            return Boolean.FALSE;
        }
        Paging<VegaDirectPayInfo> payInfoPaging = response.getResult();

        List<VegaDirectPayInfo> lists = payInfoPaging.getData();
        int listSize = lists.size();
        if (payInfoPaging.getTotal() == 0 || listSize == 0) {
            return Boolean.FALSE;
        }
        vegaDirectPayInfoList.addAll(lists);
        return listSize == size;

    }

    public Boolean handleWaitBankPay(int pageNo, int size, List<VegaDirectPayInfo> vegaDirectPayInfoList) {
        Response<Paging<VegaDirectPayInfo>> response = vegaDirectPayInfoReadService.pagingByStatus(pageNo, size, Integer.valueOf(VegaDirectPayInfoStatus.WAIT_BANK_PAY.value()));
        if (!response.isSuccess()) {
            log.error("paging vega direct pay info failed ,cause {}", response.getError());
            return Boolean.FALSE;
        }
        Paging<VegaDirectPayInfo> payInfoPaging = response.getResult();

        List<VegaDirectPayInfo> lists = payInfoPaging.getData();
        int listSize = lists.size();
        if (payInfoPaging.getTotal() == 0 || listSize == 0) {
            return Boolean.FALSE;
        }
        vegaDirectPayInfoList.addAll(lists);
        return listSize == size;

    }


}
