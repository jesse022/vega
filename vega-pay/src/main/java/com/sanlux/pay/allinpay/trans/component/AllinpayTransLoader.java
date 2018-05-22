package com.sanlux.pay.allinpay.trans.component;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.pay.allinpay.request.AllinpayTransRequest;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import com.sanlux.pay.allinpay.trans.AllinpayTrans;
import io.terminus.common.model.Response;
import io.terminus.pay.api.TransLoader;
import io.terminus.pay.exception.PayException;
import io.terminus.pay.model.PayTransCriteria;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.List;

/**
 * DATE: 16/9/2 上午10:34 <br>
 * MAIL: zhanghecheng@terminus.io <br>
 * AUTHOR: zhanghecheng
 */
@Slf4j
public class AllinpayTransLoader<T extends AllinpayToken> implements TransLoader<AllinpayTrans, T> {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");


    @Override
    public List<AllinpayTrans> loadTrans(PayTransCriteria criteria, T token) {
        try {
            return getTrans(criteria,token);
        }catch (Exception e){
            log.error("loadTrans fail, criteria={}, token={}, cause={}", criteria, token, Throwables.getStackTraceAsString(e));
            throw new PayException("load.trans.fail");
        }
    }

    /**
     * 获取每个分页
     */
    public List<AllinpayTrans> getTrans(PayTransCriteria criteria, AllinpayToken alipayToken){

        String settleDate = DATE_TIME_FORMAT.print(new DateTime(criteria.getStart()));

        AllinpayTransRequest transRequest = AllinpayTransRequest.build(alipayToken);
        transRequest
                .settleDate(settleDate)
                .sign();

        Response<List<AllinpayTrans>> listRes = transRequest.trandQuery();
        if(!listRes.isSuccess()){
            log.error("query allinpay trans fail settle date:{},error:{}",settleDate,listRes.getError());
            return Lists.newArrayList();
        }

        return listRes.getResult();
    }

}
