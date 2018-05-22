package com.sanlux.web.front.dto;

import io.terminus.parana.store.web.storage.dto.LeaveGodownDateDto;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2017/3/1.
 */
@Data
public class VegaLeaveGodownDateDto extends LeaveGodownDateDto implements Serializable {
    private static final long serialVersionUID = 5088066258646934501L;

    private Long total;
}
