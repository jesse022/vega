/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.sanlux.pay.credit.request;

import com.google.common.base.Objects;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

/**
 * Mail: remindxiao@gmail.com <br>
 * Date: 2014-03-13 11:55 AM  <br>
 * Author: xiao
 */
@XStreamAlias("credit_pay")
public class CreditPaySyncResponse {


    @Setter
    @XStreamAlias("is_success")
    private String success;

    @Getter
    @Setter
    @XStreamAlias("error")
    private String error;


    public boolean isSuccess() {
        return Objects.equal(success, "T");
    }

}
