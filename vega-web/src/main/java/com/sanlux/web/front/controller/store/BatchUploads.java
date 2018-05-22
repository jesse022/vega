package com.sanlux.web.front.controller.store;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.store.enums.StoreImportsStatus;
import com.sanlux.store.enums.StoreImportsType;
import com.sanlux.store.model.StoreImports;
import com.sanlux.store.service.StoreImportsReadService;
import com.sanlux.store.service.StoreImportsWriteService;
import com.sanlux.store.service.VegaLocationWriteService;
import com.sanlux.web.front.core.util.ObjectUtil;
import com.sanlux.web.front.dto.StoreImportDto;
import com.sanlux.web.front.queue.StoreQueueProvider;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.store.model.Location;
import io.terminus.parana.store.model.Repertory;
import io.terminus.parana.store.service.LocationReadService;
import io.terminus.parana.store.service.RepertoryReadService;
import io.terminus.parana.store.web.util.ExcelUtil;
import io.terminus.parana.store.web.util.ExportHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Objects;

/**
 * 进销存批量导入control类
 * Created by lujm on 2017/3/15.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/store")
public class BatchUploads {
    @RpcConsumer
    private ItemReadService itemReadService;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private VegaLocationWriteService vegaLocationWriteService;
    @RpcConsumer
    private StoreImportsReadService storeImportsReadService;
    @RpcConsumer
    private StoreImportsWriteService storeImportsWriteService;
    @RpcConsumer
    private RepertoryReadService repertoryReadService;
    @RpcConsumer
    private LocationReadService locationReadService;
    @Autowired
    private StoreQueueProvider storeQueueProvider;

    private DateTimeFormatter DATE = DateTimeFormat.forPattern("yyyyMMddHHmmss");


    /**
     * 改写进销存批量入库导入/批量创建库区接口
     * 原先接口:/api/storage/batch/upload/store
     * @param type: 1-上传excel批量创建库区等   2-批量创建入库单分配库位等
     * @param filePath 文件路径
     * @return 是否成功
     */
    @RequestMapping(value = "/batch/upload", method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> uploadItems(@RequestParam String filePath, @RequestParam Integer type) {
        try {
            ParanaUser user = UserUtil.getCurrentUser();
            if (user == null) {
                throw new JsonResponseException(401,"user.not.login");
            }
            //批量导入批次,根据批次查询成功/失败情况
            String importKey=getRandomNo();
            Response<Boolean> booleanResponse=batchCreate(filePath,user.getId(),type,importKey);
            if (!booleanResponse.isSuccess()) {
                log.error("first shop and second shop upload store excel fail, cause:{}", booleanResponse.getError());
                return Response.fail(booleanResponse.getError());
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("first shop and second shop upload store excel fail, cause:{}", e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * 改写下载批量入库单模板,增加按skuid排序功能
     * 原先接口:/api/storage/batch/download/store-pre-in/template
     */
    @RequestMapping(value = "/download/store-pre-in/template", method = RequestMethod.GET)
    public void downloadStorePreInTemplateFile(HttpServletRequest request, HttpServletResponse response) {
        try {
            final ParanaUser user = UserUtil.getCurrentUser();
            String fileName = "批量入库模板.xlsx";
            String agent = request.getHeader("USER-AGENT").toLowerCase();
            response.setCharacterEncoding("utf-8");
            response.setContentType("application/vnd.ms-excel");
            try{
                String encodefilename = java.net.URLEncoder.encode(fileName, "UTF-8");
                if (agent.contains("firefox")) {
                    response.setHeader("content-disposition", "attachment;filename=" + new String(fileName.getBytes("UTF-8"), "ISO8859-1"));
                } else if (agent.contains("safari")) {
                    encodefilename = encodefilename.replaceAll("\\+", "%20");
                    response.setHeader("content-disposition", "attachment;filename=" + encodefilename + ";filename*=utf-8''" + encodefilename);
                } else {
                    response.setHeader("Content-disposition", "attachment;filename=" + encodefilename);
                }
            } catch (UnsupportedEncodingException e) {
                throw new JsonResponseException("fileName encode fail!");
            }
            Response<List<Repertory>> repertoryResp = repertoryReadService.findByOwnerId(user.getId());
            if (!repertoryResp.isSuccess()) {
                log.error("find repertory by ownerId:{} fail, cause:{}",
                        user.getId(), repertoryResp.getError());
                throw new JsonResponseException(repertoryResp.getError());
            }
            List<Long> repertoryIds = Lists.transform(repertoryResp.getResult(), Repertory::getId);
            Response<List<Location>> locationResp = locationReadService.findByRepertoryIds(repertoryIds);
            if (!locationResp.isSuccess()) {
                log.error("find location by repertoryIds:{} fail, cause:{}",
                        repertoryIds, locationResp.getError());
                throw new JsonResponseException(locationResp.getError());
            }
            List<Location> locations = locationResp.getResult();
            locations.removeIf(location -> (location.getSkuId() == null || location.getSkuId() == 0));

            //按照商品SkuId从小到大排序
            Collections.sort(locations, new Comparator() {
                @Override
                public int compare(Object o1, Object o2) {
                    Location location0=(Location)o1;
                    Location location1=(Location)o2;
                    return location0.getSkuId().compareTo(location1.getSkuId());
                }
            });

            buildPreInTemplateFile(response.getOutputStream(), locations);
        } catch (Exception e) {
            log.error("download the Excel of store failed ");
            e.printStackTrace();
            throw new JsonResponseException("download.store.excel.fail");
        }
    }
    /**
     * 进销存批量导入日志查询接口
     *
     * @param pageNo   页码
     * @param pageSize 分页大小
     * @return StoreImports
     */
    @RequestMapping(value = "/import/log/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<StoreImports> creditAlterResumePaging(Integer pageNo, Integer pageSize,
                                                             @RequestParam(value = "status", required = false) Integer status,
                                                             @RequestParam(value = "type", required = false) Integer type,
                                                             @RequestParam(value = "startAt", required = false) String startAt,
                                                             @RequestParam(value = "endAt", required = false) String endAt) {
        ParanaUser user = UserUtil.getCurrentUser();
        if (user == null) {
            throw new JsonResponseException(401,"user.not.login");
        }
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("userId", user.getId());
        if (!Objects.isNull(status)) {
            criteria.put("status", status);
        }
        if (!Objects.isNull(type)) {
            criteria.put("type", type);
        }
        if (!Strings.isNullOrEmpty(startAt)) {
            criteria.put("startAt", startDate(startAt));
        }
        if (!Strings.isNullOrEmpty(endAt)) {
            criteria.put("endAt", endDate(endAt));
        }
        Response<Paging<StoreImports>> resp = storeImportsReadService.Paging(pageNo, pageSize,criteria);
        if (!resp.isSuccess()) {
            log.error("fail to find storeImports by criteria={},pageNo={},pageSize={},cause:{}",
                    criteria,pageNo, pageSize, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        return resp.getResult();
    }

    /**
     * 只对对上传数据表格数据分析,没有问题把文件路径放入队列,不进行具体业务操作
     * @param path 文件路径
     * @param userId 用户ID
     * @param type 上传类型 1:库位 2:入库单
     * @param key 导入批次号
     * @return 是否成功
     */
    public Response<Boolean> batchCreate (String path, Long userId, Integer type,String key) {
        try {
            // 下载下来excel
            try {
                URL url = new URL("http:" + path.replaceAll("http:", ""));
                if(log.isDebugEnabled()){
                    log.debug("start analyze excel from network ");
                }
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(30000);
                if (type == 1) {
                    ExcelUtil.analyzeStoreExcel(conn.getInputStream());
                } else {
                    ExcelUtil.analyzePreInExcel(conn.getInputStream());
                }
                conn.disconnect();

                ParanaUser user = UserUtil.getCurrentUser();
                if (user == null) {
                    throw new JsonResponseException(401,"user.not.login");
                }
                //表格分析完,开始导入之前,先把导入日志标记为未开始,然后把文件路径放入队列,不进行后续操作
                StoreImports storeImports = new StoreImports();
                Long id = 0L;
                try {
                    storeImports.setUserId(userId);
                    storeImports.setUserName(user.getName());
                    storeImports.setBatchNo(key);
                    storeImports.setStatus(StoreImportsStatus.NOT_STARTED.value());//未开始
                    if (type == 1) {
                        storeImports.setType(StoreImportsType.LOCATION.value());//库位导入
                    }else{
                        storeImports.setType(StoreImportsType.IN_STORE.value());//入库单导入
                    }
                    Response<Long> rsp = storeImportsWriteService.create(storeImports);
                    if (rsp.isSuccess()){
                        id=rsp.getResult();
                    }
                }catch (Exception e2) {
                    log.error("create storeImports failed, storeImports:{}, cause:{}", storeImports, Throwables.getStackTraceAsString(e2));
                }
                StoreImportDto storeImportDto=new StoreImportDto();
                storeImportDto.setUserID(userId);
                storeImportDto.setPath(path);
                if (type == 1) {
                    storeImportDto.setType(StoreImportsType.LOCATION.value());//库位导入
                }else{
                    storeImportDto.setType(StoreImportsType.IN_STORE.value());//入库单导入
                }
                storeImportDto.setKey(key);
                storeImportDto.setId(id);
                storeQueueProvider.push(ObjectUtil.object2Bytes(storeImportDto));//放入队列

                if(log.isDebugEnabled()){
                    log.debug("end analyze excel from network");
                }
            } catch (MalformedURLException e) {
                log.error("failed to parse excel, error : {}", e.getMessage());
                return Response.fail(e.getMessage());
            } catch (IOException e) {
                log.error("failed to parse excel, cause : {}", e.getMessage());
                return Response.fail(e.getMessage());
            } catch (Exception e){
                log.error("failed to parse excel, cause : {}", Throwables.getStackTraceAsString(e));
                return Response.fail(e.getMessage());
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("update store excel failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("upload.store.excel.failed");
        }
    }


    /**
     * 生成导入批次流水号
     * eg: 201703151650430008
     *
     * @return 流水号
     */
    private String getRandomNo() {
        String prefix = DATE.print(DateTime.now());
        String suffix = "000" + new Random().nextInt(10);
        suffix = suffix.substring(suffix.length() - 3, suffix.length());
        return prefix + "0" + suffix;
    }

    /**
     * list转换成string处理函数
     * @param list list
     * @param separator separator
     * @return string
     */
    public String listToString(List list, char separator) {
        StringBuilder sb = new StringBuilder();
        if(list.size()==1){
            sb.append(list.get(0)) ;
        }else {
            for (int i = 0; i < list.size(); i++) {
                sb.append(list.get(i)).append(separator);
            }
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

    /**
     * 截止时间
     *
     * @param endAt 截止时间
     * @return date
     */
    private Date endDate(String endAt) {
        return Strings.isNullOrEmpty(endAt) ? null : DateTime.parse(endAt).plusDays(1).toDate();
    }

    /**
     * 起始时间
     *
     * @param startAt 起始时间
     * @return date
     */
    private Date startDate(String startAt) {
        return Strings.isNullOrEmpty(startAt) ? null : DateTime.parse(startAt).toDate();
    }

    private void buildPreInTemplateFile (OutputStream outputStream, List<Location> locations) {
        XSSFWorkbook xssfWorkbook = new XSSFWorkbook();
        try {
            Map<String, Integer> columnMaps = Maps.newLinkedHashMap();
            columnMaps.put("仓库ID", 18 * 100);
            columnMaps.put("库区ID", 18 * 100);
            columnMaps.put("库区", 18 * 400);
            columnMaps.put("库组", 18 * 400);
            columnMaps.put("货架", 18 * 400);
            columnMaps.put("库位", 18 * 400);
            columnMaps.put("SKUID", 18 * 100);
            columnMaps.put("最大容量", 18 * 100);
            columnMaps.put("报警容量", 18 * 100);
            columnMaps.put("在库数量", 18 * 100);
            columnMaps.put("最大可分配数量", 18 * 100);
            columnMaps.put("入库数量", 18 * 100);
            columnMaps.put("价格(单位:分)", 18 * 100);



            XSSFSheet xssfSheet = xssfWorkbook.createSheet("批量入库模板");
            ExportHelper.setTitleAndColumnWidth(xssfSheet, columnMaps);

            Row row1 = xssfSheet.createRow(1);
            List<String> contents = Lists.newArrayList();
            contents.add("repertory_id");
            contents.add("location_id");
            contents.add("area_name");
            contents.add("group_name");
            contents.add("shelf_name");
            contents.add("location_name");
            contents.add("sku_id");
            contents.add("max_content");
            contents.add("warn_content");
            contents.add("now_content");
            contents.add("max_can_content");
            contents.add("quantity");
            contents.add("price");


            ExportHelper.setContent(row1, contents);
            if (locations != null && locations.size() > 0) {
                for (int i = 0; i < locations.size(); i++) {
                    Row row = xssfSheet.createRow(i + 2);
                    ExportHelper.setContent(row, getContent(locations.get(i)));
                }
            }
            xssfWorkbook.write(outputStream);
        } catch (Exception e) {
            log.error("build store excel fail,cause:{}", Throwables.getStackTraceAsString(e));

        }
    }

    private List<String> getContent(Location location) {
        List<String> contents = Lists.newArrayList();
        contents.add(Arguments.isNull(location.getRepertoryId()) ? "" : location.getRepertoryId().toString());
        contents.add(Arguments.isNull(location.getLocationId()) ? "" : location.getLocationId().toString());
        contents.add(Strings.isNullOrEmpty(location.getAreaName()) ? "" : location.getAreaName());
        contents.add(Strings.isNullOrEmpty(location.getGroupName()) ? "" : location.getGroupName());
        contents.add(Strings.isNullOrEmpty(location.getShelfName()) ? "" : location.getShelfName());
        contents.add(Strings.isNullOrEmpty(location.getLocationName()) ? "" : location.getLocationName());
        contents.add(Arguments.isNull(location.getSkuId()) ? "" : location.getSkuId().toString());
        contents.add(Arguments.isNull(location.getMaxContent()) ? "" : location.getMaxContent().toString());
        contents.add(Arguments.isNull(location.getWarnContent()) ? "" : location.getWarnContent().toString());
        contents.add(Arguments.isNull(location.getNowContent()) ? "" : location.getNowContent().toString());
        contents.add(String.valueOf(location.getMaxContent() - location.getNowContent() -location.getPreIn()));

        return contents;
    }
}
