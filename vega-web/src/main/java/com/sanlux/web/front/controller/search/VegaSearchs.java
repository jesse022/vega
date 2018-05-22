package com.sanlux.web.front.controller.search;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.sanlux.search.dto.VegaSearchedItem;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.search.item.ItemSearchReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Created by cuiwentao
 * on 2/16/17
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/search")
public class VegaSearchs {

    @RpcConsumer
    private ItemSearchReadService itemSearchReadService;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<? extends SearchedItemWithAggs<VegaSearchedItem>> searchItemWithAggs(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam Map<String, String> params) {

        try {
            if (!Strings.isNullOrEmpty(params.get("q"))) {
                params.put("q", params.get("q").toLowerCase());
            }
            String templateName = "search.mustache";
            Response<? extends SearchedItemWithAggs<VegaSearchedItem>> resp =
                    itemSearchReadService.searchWithAggs(pageNo, pageSize, templateName, params, VegaSearchedItem.class);
            if (resp.isSuccess() && resp.getResult() != null) {
                List<VegaSearchedItem> itemList = resp.getResult().getEntities().getData();
                itemList.forEach(vegaSearchedItem ->
                    vegaSearchedItem.setName(fixHighLight(vegaSearchedItem.getDisplayName(), vegaSearchedItem.getName()))
                );
                resp.getResult().getEntities().setData(itemList);
            }
            return resp;
        } catch (Exception e) {
            log.error("search item fail, params:{}, pageno:{}, pageSize:{}, cause:{}",
                    params, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(e.getMessage());
        }

    }

    private String fixHighLight(String itemName, String highLightName) {
        String result = highLightName;
        highLightName = highLightName.replaceAll("<em>", "");
        highLightName = highLightName.replaceAll("</em>", "");
        if (!StringUtils.equals(itemName.toLowerCase(), highLightName.toLowerCase())) {
            return result;
        }
        char[] testChar = highLightName.toCharArray();
        char[] demoChar = itemName.toCharArray();
        int m = 0;
        try {
            for (int i = 0; i < testChar.length; i++) {
                if (StringUtils.indexOf(result, "<em>" + testChar[i] + "</em>") > -1) {
                    m = m + 9;//<em></em> 高亮字符计数
                }
                if (Character.isLowerCase(testChar[i]) || Character.isUpperCase(testChar[i])) {
                    if (StringUtils.indexOf(result, "<em>" + testChar[i] + "</em>") > -1) {
                        //如果是高亮显示的字母
                        int j = result.indexOf(testChar[i], i + m - 5);//获取需要替换字符串的起始位置,减去固定长度</em>
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    } else {
                        int j = result.indexOf(testChar[i], i + m);//获取需要替换字符串的起始位置
                        result = result.substring(0, j) + (result.substring(j, result.length())).replaceFirst(String.valueOf(testChar[i]), String.valueOf(demoChar[i]));
                    }
                }
            }
        } catch (Exception e) {
            log.error("get highLight name fail itemName:{},highLightName:{},cause={}", itemName, result, e.getMessage());
        }
        return result;
    }
}
