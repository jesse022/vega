package com.sanlux.shop.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 触摸屏视频信息处理Dto
 * Created by lujm on 2017/12/21.
 */
@Data
public class VegaTouchScreenVideoDto implements Serializable {
    private static final long serialVersionUID = 3730537524200385883L;

    private String videoName;

    private String videoPath;
}
