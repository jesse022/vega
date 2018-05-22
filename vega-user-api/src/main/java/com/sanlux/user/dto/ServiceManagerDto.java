package com.sanlux.user.dto;

import com.sanlux.user.model.ServiceManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 业务经理提成查询Dto
 *
 * Created by lujm on 2017/11/15.
 */
@Data
public class ServiceManagerDto implements Serializable {

    private static final long serialVersionUID = -639102115129395485L;

    private ServiceManager serviceManager;

    /**
     * 开始时间
     */
    private String startAt;

    /**
     * 结束时间
     */
    private String endAt;

    /**
     * 新会员提成
     */
    private Long newMemberCommission;

    /**
     * 老会员提成
     */
    private Long oldMemberCommission;

    /**
     * 总提成
     */
    private Long totalMemberCommission;

}
