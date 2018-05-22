package com.sanlux.pay.direct.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.Serializable;

/**
 * Created by liangfujie on 16/10/27
 */
@XmlRootElement(name = "CMBSDKPGK" )
public class QueryResponseDto implements Serializable {
    private static final long serialVersionUID = -2684239343355068474L;

    @XmlElement(name = "INFO")
    private QueryResponseSystemInfo queryResponseSystemInfo;

    @XmlElement(name = "NTQPAYQYZ")
    private QueryResponseBody queryResponseBody;

    @XmlTransient
    public QueryResponseSystemInfo getQueryResponseSystemInfo() {
        return queryResponseSystemInfo;
    }

    public void setQueryResponseSystemInfo(QueryResponseSystemInfo queryResponseSystemInfo) {
        this.queryResponseSystemInfo = queryResponseSystemInfo;
    }
    @XmlTransient
    public QueryResponseBody getQueryResponseBody() {
        return queryResponseBody;
    }

    public void setQueryResponseBody(QueryResponseBody queryResponseBody) {
        this.queryResponseBody = queryResponseBody;
    }
}
