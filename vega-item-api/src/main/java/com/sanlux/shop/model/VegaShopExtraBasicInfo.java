package com.sanlux.shop.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.sanlux.shop.dto.VegaTouchScreenVideoDto;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.constants.JacksonType;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 经销商基础信息扩展表Model类
 * @author lujm on  2017-12-19
 */
@Data
public class VegaShopExtraBasicInfo implements Serializable {
    private static final long serialVersionUID = -2014147482868953727L;
    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    private Long id;
    
    /**
     * 店铺ID
     */
    private Long shopId;

    /**
     * 店铺名称
     */
    private String shopName;

    /**
     * 店铺状态
     */
    private Integer shopStatus;
    
    /**
     * 店铺类型
     */
    private Integer shopType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 客户端唯一地址
     */
    private String macAddress;

    /**
     * 背景图片
     */
    private String backgroundPicture;

    /**
     * 公司介绍(图文详情)
     */
    private String detail;

    /**
     * 推荐品牌Ids,不存数据库
     */
    private String recommendedBrands;

    /**
     * 视频信息路径
     */
    private String videosJson;

    /**
     * 视频信息路径JSON,不存数据库
     */
    private List<VegaTouchScreenVideoDto> videos;
    
    /**
     * 扩展信息, JSON
     */
    private String extraJson;

    /**
     * 扩展信息
     */
    private Map<String, String> extra;


    private Date createdAt;
    
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

    public void setVideosJson(String videosJson) throws Exception{
        this.videosJson = videosJson;
        if(Strings.isNullOrEmpty(videosJson)){
            this.videos= Collections.emptyList();
        } else{
            this.videos =
                    objectMapper.readValue(videosJson, new TypeReference<List<VegaTouchScreenVideoDto>>() {
                    });
        }
    }

    public void setVideos(List<VegaTouchScreenVideoDto> videos) {
        this.videos = videos;
        if(videos ==null ||videos.isEmpty()){
            this.videosJson = null;
        }else{
            try {
                this.videosJson = objectMapper.writeValueAsString(videos);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }

}
