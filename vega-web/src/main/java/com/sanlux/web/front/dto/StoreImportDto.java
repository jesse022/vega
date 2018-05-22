package com.sanlux.web.front.dto;

import io.terminus.parana.store.dto.StorePreInDto;
import io.terminus.parana.store.dto.StoreUploadDto;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 进销存批量导入Dto类
 * Created by lujm on 2017/4/9.
 */
@Data
public class StoreImportDto implements Serializable {

    private static final long serialVersionUID = -3732546728182155742L;

    private Long userID;

    private List<StorePreInDto> storePreInDtos;

    private List<StoreUploadDto> storeToUploads;

    private String key;

    private Integer sonKey;

    private Long id;

    private String path;

    private Integer type;
}
