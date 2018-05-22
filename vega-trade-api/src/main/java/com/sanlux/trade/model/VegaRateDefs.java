package com.sanlux.trade.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * 三力士相关费率定义表model
 *
 * Created by lujm on 2017/11/16.
 */
@Data
public class VegaRateDefs implements Serializable {

    private static final long serialVersionUID = -3382533886148911436L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    private Long id;

    /**
     * 费率定义名称唯一标识
     */
    private String name;

    /**
     * 类型
     */
    private Integer type;

    /**
     * 费率定义中文描述
     */
    private String describe;

    /**
     * 费率值
     */
    private Long rateKey;

    /**
     * 费率基数
     */
    private Long rateBase;

    /**
     * 扩展信息字段,不存数据库
     */
    private Map<String, String> extra;

    /**
     * 扩展信息字段,存数据库
     */
    private String extraJson;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setExtraJson(String extraJson) throws Exception{
        this.extraJson = extraJson;
        if(Strings.isNullOrEmpty(extraJson)){
            this.extra= Collections.emptyMap();
        } else{
            this.extra = objectMapper.readValue(extraJson, JacksonType.MAP_OF_STRING);
        }
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if(extra ==null ||extra.isEmpty()){
            this.extraJson = null;
        }else{
            try {
                this.extraJson = objectMapper.writeValueAsString(extra);
            } catch (Exception e) {
                //ignore this exception
            }
        }
    }
}
