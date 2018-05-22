package com.sanlux.youyuncai.model;

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
 * 集乘商品对外同步表Model
 * Created by lujm on 2018/1/31.
 */
@Data
public class VegaItemSync implements Serializable {

    private static final long serialVersionUID = 7711686450589432589L;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();


    private Long id;

    /**
     * 同步渠道
     */
    private Integer channel;

    /**
     * 同步类型
     */
    private Integer type;

    /**
     * 同步状态
     */
    private Integer status;

    /**
     * 同步信息主键ID，商品为:skuId,商品分类：主键
     */
    private Long syncId;

    /**
     * 同步信息上级ID，商品为:商品Id,商品分类：上级分类
     */
    private Long syncPid;

    /**
     * 同步信息名称，商品为:商品名称,商品分类：分类名称
     */
    private String syncName;

    /**
     * 店铺类型
     */
    private String syncLog;

    /**
     * 操作用户ID
     */
    private Long userId;

    /**
     * 操作用户名
     */
    private String userName;


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

    public enum Channel {
        /**
         * 友云采
         */
        YOU_YUN_CAI(1);

        private final int value;

        public final int value() {
            return value;
        }

        Channel(int value) {
            this.value = value;
        }
    }

    public enum Type {
        /**
         * 商品类目
         */
        CATEGORY(1),

        /**
         * 商品
         */
        ITEM(2);

        private final int value;

        public final int value() {
            return value;
        }

        Type(int value) {
            this.value = value;
        }
    }


    public enum Status {
        /**
         * 未同步
         */
        WAIT_SYNC(0),
        /**
         * 同步成功
         */
        SUCCESS(1),
        /**
         * 同步失败
         */
        FAIL(2),

        /**
         * 待更新
         */
        TO_UPDATE(3);

        private final int value;

        public final int value() {
            return value;
        }

        Status(int value) {
            this.value = value;
        }
    }

    public enum SyncType {

        /**
         * 新增
         */
        ADD,

        /**
         * 修改
         */
        UPDATE,

        /**
         * 删除
         */
        DELETE;
    }

}
