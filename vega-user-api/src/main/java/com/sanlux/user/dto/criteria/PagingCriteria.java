package com.sanlux.user.dto.criteria;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.terminus.common.model.PageInfo;
import io.terminus.parana.common.model.Criteria;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.Map;

/**
 * 分页的基类,用于包含所有都要用的分页条件
 * Created by zhanghecheng on 16/3/1.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PagingCriteria extends Criteria implements Serializable {

    private static final long serialVersionUID = 2598875146576926658L;

    /**
     * 分页号, 从1开始
     */
    @JsonIgnore
    @Getter
    @Setter
    private Integer pageNo=1;

    /**
     * 分页大小
     */
    @JsonIgnore
    @Getter
    @Setter
    private Integer pageSize=20;

    /**
     * 是否还有下一页
     */
    @JsonIgnore
    @Setter
    private Boolean hasNext=true;

    @JsonIgnore
    public Boolean  hasNext(){
        return this.hasNext;
    }

    /**
     * 跳转到下一页
     */
    public void nextPage(){
        if(pageNo==null){
            pageNo=1;
        }
        pageNo += 1;
    }

    /**
     * 分页大小, 默认20, 用于数据库
     * @return
     */
    public Integer getLimit(){
        PageInfo pageInfo=new PageInfo(pageNo,pageSize);
        return pageInfo.getLimit();
    }

    /**
     * 分页起始, 从0开始, 用于数据库
     * @return
     */
    public Integer getOffset(){
        PageInfo pageInfo=new PageInfo(pageNo,pageSize);
        return pageInfo.getOffset();
    }

    @Override
    public Map<String, Object> toMap() {
        formatDate();
        return super.toMap();
    }

    /**
     * 如果Start的时间和End的时间一致, 则End+1day
     */
    protected void formatDate(){

    }

}
