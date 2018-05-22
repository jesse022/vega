package com.sanlux.category.service;

import io.terminus.common.model.Response;
import io.terminus.parana.category.model.FrontCategory;

import java.util.List;

/**
 * Created by syf on 2017/6/28.
 */
public interface VegaFrontCategoryReaderService {

    Response<List<FrontCategory>> findAncestorsOf(Long id);



}
