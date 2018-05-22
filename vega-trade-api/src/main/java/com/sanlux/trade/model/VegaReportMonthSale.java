package com.sanlux.trade.model;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sanlux.trade.dto.ReportCategoryDto;
import com.sanlux.trade.dto.ReportTerminalDto;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by lujm on 2018/1/4
 */
@Data
public class VegaReportMonthSale implements Serializable {

    private static final long serialVersionUID = 4712438905128163316L;
    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private Long id;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月度
     */
    private Integer month;

    /**
     * 状态
     */
    private Boolean status;

    /**
     * 订单总数
     */
    private Integer orderCount;

    /**
     * 订单金额
     */
    private Long orderFee;

    /**
     * 订单会员数
     */
    private Integer orderMember;

    /**
     * 访客分类
     */
    private String visitorJson;

    private List<ReportTerminalDto> visitors;

    /**
     * 类目分类
     */
    private String categoryJson;

    private List<ReportCategoryDto> categorys;

    /**
     * 扩展信息
     */
    private String extraJson;

    private Map<String, String> extra;

    private Date createdAt;

    private Date updatedAt;

    public void setVisitorJson(String visitorJson) throws Exception{
        this.visitorJson = visitorJson;
        if(Strings.isNullOrEmpty(visitorJson)) {
            this.visitors = Collections.emptyList();
        } else {
            this.visitors =
                    objectMapper.readValue(visitorJson, new TypeReference<List<ReportTerminalDto>>() {
                    });
        }

    }

    public void setVisitors(List<ReportTerminalDto> visitors) {
        this.visitors = visitors;
        if(visitors ==null ||visitors.isEmpty()){
            this.visitorJson = null;
        }else{
            try {
                this.visitorJson = objectMapper.writeValueAsString(visitors);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }

    public void setCategoryJson(String categoryJson) throws Exception{
        this.categoryJson = categoryJson;
        if(Strings.isNullOrEmpty(categoryJson)){
            this.categorys= Collections.emptyList();
        } else{
            this.categorys =
                    objectMapper.readValue(categoryJson, new TypeReference<List<ReportCategoryDto>>() {
            });
        }
    }

    public void setCategorys(List<ReportCategoryDto> categorys) {
        this.categorys = categorys;
        if(categorys ==null ||categorys.isEmpty()){
            this.categoryJson = null;
        }else{
            try {
                this.categoryJson = objectMapper.writeValueAsString(categorys);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }

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
