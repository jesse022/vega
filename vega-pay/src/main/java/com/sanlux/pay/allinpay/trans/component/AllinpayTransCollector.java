package com.sanlux.pay.allinpay.trans.component;

import com.google.common.base.Throwables;
import com.sanlux.pay.allinpay.trans.AllinpayTrans;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pay.api.TransCollector;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.PayTrans;
import io.terminus.pay.util.NumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 支付宝的账单收集器, 负责收集账单信息
 *
 * DATE: 16/9/2 上午9:24 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Component
@Slf4j
public class AllinpayTransCollector implements TransCollector<AllinpayTrans> {

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonDefaultMapper();


    @Override
    public List<PayTrans> collectCommissionAndRate(List<AllinpayTrans> transList) throws PayException {

        try {

            List<PayTrans> result = new ArrayList<>();

            for (AllinpayTrans transData: transList) {
                if (transData.getTransCodeMsg().equals("ZF")) { //支付记录

                    PayTrans payTrans = new PayTrans();
                    result.add(payTrans);

                    payTrans.setFee(transData.totalFeeToFen());
                    payTrans.setTradeNo(transData.getTransOutOrderNo());
                    payTrans.setTradeAt(transData.getTradeAt());
                    if(transData.getServiceFeeRatio()!=null){
                        payTrans.setRate(NumberUtil.doubleMultipleByBase(transData.getServiceFeeRatio(), 10000));
                        payTrans.setCommission(transData.serviceFeeToFen());
                        payTrans.setDetails(JSON_MAPPER.toJson(Arrays.asList(payTrans)));
                    }else{
                        payTrans.setCommission(transData.serviceFeeToFen());
                        payTrans.setRate(0l);//通联费率为null,这里写0代替
                        payTrans.setDetails(JSON_MAPPER.toJson(Arrays.asList(payTrans)));
                    }
                    
                } else if (transData.getTransCodeMsg().equals("TH")) { //退款记录

                    PayTrans payTrans = new PayTrans();
                    result.add(payTrans);

                    payTrans.setRefundNo(transData.getTransOutOrderNo());
                    payTrans.setFee(transData.totalFeeToFen());
                    payTrans.setTradeAt(transData.getTradeAt());

                    payTrans.setCommission(transData.serviceFeeToFen());  //outcome
                    payTrans.setRate(0l);
                    payTrans.setDetails(JSON_MAPPER.toJson(Arrays.asList(payTrans)));
                    }
            }
            return result;
        }catch (Exception e){
            log.error("collectCommissionAndRate fail, transList={}, cause={}", transList, Throwables.getStackTraceAsString(e));
            throw new PayException("collect.commission.and.rate.fail");
        }
    }
}
