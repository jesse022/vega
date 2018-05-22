package com.sanlux.item.model;

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
 * Created by lujm on 2018/1/22.
 */
@Data
public class VegaBrandExtra implements Serializable {

    private static final long serialVersionUID = -7253412313000089084L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    private Long id;

    /**
     * 品牌ID
     */
    private Long brandId;

    /**
     * 品牌介绍详情
     */
    private String detail;

    /**
     * 扩展信息,不存数据库
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
