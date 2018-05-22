package com.sanlux.web.front.core.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Created by liangfujie on 16/11/2
 */
@Slf4j
@ConfigurationProperties(prefix = "direct.pay.query")
@Component
public class DirectQueryParams {


    //函数名
    private String FUNNAM;
    //数据格式
    private Integer DATTYP;
    //登录用户名
    private String LGNNAM;

    //查询日期类型
    private String DATFLG;

    public String getDATFLG() {
        return DATFLG;
    }

    public void setDATFLG(String DATFLG) {
        this.DATFLG = DATFLG;
    }

    public String getFUNNAM() {
        return FUNNAM;
    }

    public void setFUNNAM(String FUNNAM) {
        this.FUNNAM = FUNNAM;
    }

    public Integer getDATTYP() {
        return DATTYP;
    }

    public void setDATTYP(Integer DATTYP) {
        this.DATTYP = DATTYP;
    }

    public String getLGNNAM() {
        return LGNNAM;
    }

    public void setLGNNAM(String LGNNAM) {
        this.LGNNAM = LGNNAM;
    }
}
