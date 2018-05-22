package com.sanlux.user.dto;

import com.sanlux.user.model.Rank;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by liangfujie on 16/8/10
 */
@Data
public class UserRankDto  implements Serializable{
    private static final long serialVersionUID = -1086496734566129938L;
    private List<Rank> ranks;
    private UserRank userRank;
    //消费金额与积分比例
    private String integralScale;
    //消费金额与成长值比例
    private String growthValueScale;

}
