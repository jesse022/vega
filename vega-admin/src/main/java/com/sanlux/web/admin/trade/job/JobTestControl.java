package com.sanlux.web.admin.trade.job;

import com.sanlux.web.admin.credit.job.CreditFrozeJob;
import com.sanlux.web.admin.credit.job.CreditSmsNotifications;
import com.sanlux.web.admin.settle.job.VegaPayCheckJob;
import io.terminus.parana.web.admin.jobs.ItemDumps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * job测试controller类
 *
 * Created by lujm on 2017/3/28.
 */
@RestController
@Slf4j
@RequestMapping("/api/admin/job/test")
public class JobTestControl {
    @Autowired
    private  VegaOrderAutoConfirmJob vegaOrderAutoConfirmJob;

    @Autowired
    private VegaPayCheckJob vegaPayCheckJob;

    @Autowired
    private ItemDumps itemDumps;

    @Autowired
    private CreditSmsNotifications creditSmsNotifications;

    @Autowired
    private CreditFrozeJob creditFrozeJob;


    @RequestMapping(value = "/confirm", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void AutoConfirm() {
        vegaOrderAutoConfirmJob.handOrderExpire();
    }


    @RequestMapping(value = "/payCheck", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void AutoPayCheck() {
        vegaPayCheckJob.handPaycheck();
    }

    /**
     * 手工更新15分钟内修改过的商品索引
     */
    @RequestMapping(value = "/search/index", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void deltaDump() { itemDumps.deltaDump();}

    @RequestMapping(value = "/creditSms", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void notifyCreditRepayment() {
        creditSmsNotifications.notifyCreditRepayment();
    }

    @RequestMapping(value = "/frozeCredit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void frozeCredit() {
        creditFrozeJob.frozeCredit();
    }
}
