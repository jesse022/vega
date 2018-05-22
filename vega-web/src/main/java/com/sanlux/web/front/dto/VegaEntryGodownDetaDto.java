package com.sanlux.web.front.dto;

import io.terminus.parana.store.web.storage.dto.EntryGodownDetaDto;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by lujm on 2017/3/1.
 */
@Data
public class VegaEntryGodownDetaDto extends EntryGodownDetaDto implements Serializable {
    private static final long serialVersionUID = -4479347031596175486L;

    private Long total;
}
