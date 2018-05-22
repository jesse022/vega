package com.sanlux.web.front.core.toolmall;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.sanlux.item.dto.toolmall.ToolMallCategoryReturn;
import com.sanlux.item.dto.toolmall.ToolMallItemAllReturn;
import com.sanlux.item.dto.toolmall.ToolMallItemOtherReturn;
import com.sanlux.item.enums.ToolMallItemImportType;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

/**
 * 土猫网开放接口
 * Created by lujm on 2018/4/13.
 */
@Slf4j
@Component
public class ToolMallAPIRequest {
    @Value("${toolMall.token.uid: 6}")
    private String uid ;
    @Value("${toolMall.token.appKey: ea0f1ac144e5}")
    private String appKey ;
    @Value("${toolMall.token.appSecret: d551e8cf779b4be29ae996589f8e1ea6}")
    private String appSecret ;
    @Value("${toolMall.token.appSecret: 27}")
    private String sid ;



    private static final String API_URL = "http://openapi.toolmall.com/b/pc1.0";
    // private static final String API_URL = "http://10.3.0.46:8080/api/v1.0/sys_time?uid=6&appKey=ea0f1ac144e5&timestamp=1523152715&sign=99cae88c43465819de891dc99d683589”

    // 系统时间获取
    private static final String SYS_TIME_URI = "/getTime";

    // 商品类目更新uri
    private static final String CATEGORY_LIST_URI = "/getCategory";

    // 商品更新uri
    private static final String SKU_LIST_URI = "/getProducts";


    private Map<String, String> getSignParams(){
        Map<String, String> params = Maps.newHashMap();
        params.put("uid", uid);
        params.put("appKey", appKey);
        params.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        return params;
    }


    /**
     * 获取系统时间
     */
    public Response<String> getSysTime(){
        try {
            Map<String, String>  params = getSignParams();
            String sign = getSign(params, appSecret);
            params.put("sign", sign);

            String url = API_URL + SYS_TIME_URI;
            log.info("url:" + url + ", sign: " + sign);

            String result = HttpRequest.post(API_URL + SYS_TIME_URI).connectTimeout(1000000).readTimeout(1000000).form(params).body();
            log.info("client result: " + result);
            return Response.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("tool mall get time api query fail cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("tool.mall.api.query.fail");
        }
    }

    /**
     * 获取商品类目更新列表
     * @param beginTime 开始时间
     * @param endTime   结束时间
     */
    public Response<ToolMallCategoryReturn> getCategoryList(String beginTime, String endTime){
        try {
            Map<String, String>  params = getSignParams();
            params.put("beginTime", beginTime);
            params.put("endTime", endTime);
            String sign = getSign(params, appSecret);
            params.put("sign", sign);
            log.info("client sign: " + sign);

            String result = HttpRequest.post(API_URL + CATEGORY_LIST_URI).connectTimeout(1000000).readTimeout(1000000).form(params).body();

            log.info("client result: " + result);
            if(result != null && !result.equals("")) {
               // // TODO: 2018/4/13
                return Response.ok(JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(result, ToolMallCategoryReturn.class));
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("tool mall get category api query fail cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("tool.mall.api.query.fail");
        }
    }


    /**
     * 获取商品列表
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   页码
     * @param pageSize  每页条数
     */
    public Response<Object> getSkuList(String beginTime, String endTime, Integer pageNum, Integer pageSize, Integer syncType){
        try {
            Map<String, String>  params = getSignParams();
            params.put("sid", sid);
            params.put("beginTime", beginTime);
            params.put("endTime", endTime);
            params.put("pageNum", pageNum.toString());
            params.put("pageSize", pageSize.toString());
            params.put("type", syncType.toString());

            String sign = getSign(params, appSecret);
            log.info("client sign: " + sign);
            params.put("sign", sign);


            String result = HttpRequest.post(API_URL + SKU_LIST_URI).connectTimeout(1000000).readTimeout(1000000).form(params).body();
            if(result != null && !result.equals("")) {
               // // TODO: 2018/4/13
                if(Objects.equals(syncType, ToolMallItemImportType.ItemSyncType.ALL.value())) {
                    return Response.ok(JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(result, ToolMallItemAllReturn.class));
                } else {
                    return Response.ok(JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(result, ToolMallItemOtherReturn.class));
                }
            }

            log.info("client result: " + result);

            return Response.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("tool mall get sku api query fail cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("tool.mall.api.query.fail");
        }
    }

    /**
     * 获取签名
     * @param params 参数
     * @param appSecret 认证密码
     * @return
     */
    private String getSign(Map<String, String> params, String appSecret){
        // 先将参数以其参数名的字典升序进行排序
        Map<String, String> sortedParams = new TreeMap<String, String>(params);
        Set<Map.Entry<String, String>> entrys = sortedParams.entrySet();

        // 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> param : entrys) {

            // 获取Key和value的字符串长度
            int keyLength = param.getKey().length();
            int valLength = param.getValue().length();

            // 获取位数,以keyLength-key:valueLength-value格式拼接
            sb.append(getKeyIntLength(keyLength)).append("-").append(param.getKey()).append(":")
                    .append(getValueIntLength(valLength)).append("-").append(param.getValue()).append(":");

        }
        // 移除最后一个:号
        sb.deleteCharAt(sb.length()-1);

        // 在排序后的字符串最后添加秘钥
        sb.append(appSecret);

        log.info("sign params: " + sb.toString());

        // 设置加密后的签名
        return MD5Util.getMD5String(sb.toString()).toLowerCase();
    }

    private String getKeyIntLength(int key){
        return String.valueOf(key).length() > 1 ? String.valueOf(key) : "0" + key;
    }

    private String getValueIntLength(int value){
        String str = String.valueOf(value);
        int length = 5 - str.length();
        String result = null;
        switch (length){
            case 0:
                result = str;
                break;
            case 1:
                result = "0" + str;
                break;
            case 2:
                result = "00" + str;
                break;
            case 3:
                result = "000" + str;
                break;
            case 4:
                result = "0000" + str;
                break;
        }
        return result;
    }
}
