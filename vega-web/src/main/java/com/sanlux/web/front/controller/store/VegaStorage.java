package com.sanlux.web.front.controller.store;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.sanlux.store.service.InStorageReadService;
import com.sanlux.store.service.OutStorageReadService;
import com.sanlux.store.service.VegaLocationReadService;
import com.sanlux.store.service.VegaRepertoryToLocationLevelsReadService;
import com.sanlux.web.front.dto.VegaEntryGodownDetaDto;
import com.sanlux.web.front.dto.VegaLeaveGodownDateDto;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.item.service.ItemReadService;
import io.terminus.parana.item.service.SkuReadService;
import io.terminus.parana.store.dto.EntryJobDateDto;
import io.terminus.parana.store.dto.LeaveJobDateDto;
import io.terminus.parana.store.dto.LocationDateDto;
import io.terminus.parana.store.dto.RepertoryLevelUseDto;
import io.terminus.parana.store.enums.SonLeaveGodownStatus;
import io.terminus.parana.store.model.*;
import io.terminus.parana.store.service.*;
import io.terminus.parana.store.web.storage.dto.EntryGodownDetaDto;
import io.terminus.parana.store.web.storage.dto.LeaveGodownDateDto;
import io.terminus.parana.store.web.storage.dto.SonEntryGodownDetaDto;
import io.terminus.parana.store.web.storage.dto.SonLeaveGodownDateDto;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by lujm on 2017/3/1.
 */
@Slf4j
@RestController
@RequestMapping("/api/vega/store")
public class VegaStorage {
    @RpcConsumer
    private EntryGodownReadService entryGodownReadService;
    @RpcConsumer
    private SonEntryGodownReadService sonEntryGodownReadService;
    @RpcConsumer
    private SkuReadService skuReadService;
    @RpcConsumer
    private EntryJobReadService entryJobReadService;
    @RpcConsumer
    private LocationReadService locationReadService;
    @RpcConsumer
    private RepertoryReadService repertoryReadService;
    @RpcConsumer
    private InStorageReadService InStorageReadService;
    @RpcConsumer
    private OutStorageReadService outStorageReadService;
    @RpcConsumer
    private LeaveGodownReadService leaveGodownReadService;
    @RpcConsumer
    private LeaveJobReadService leaveJobReadService;
    @RpcConsumer
    private SonLeaveGodownReadService sonLeaveGodownReadService;
    @RpcConsumer
    private SonLeaveGodownWriteService sonLeaveGodownWriteService;
    @RpcConsumer
    private ItemReadService itemReadService;
    @RpcConsumer
    private RepertoryToLocationLevelsReadService repertoryToLocationLevelsReadService;
    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;
    @RpcConsumer
    private VegaLocationReadService vegaLocationReadService;
    @RpcConsumer
    private VegaRepertoryToLocationLevelsReadService vegaRepertoryToLocationLevelsReadService;


    /**
     * 改写查看入库单详情接口,原先接口:/api/storage/entrygodown/detail,新接口增加分页查询功能
     * @param entryGodownId "入库单id
     * @param pageNo 页号
     * @param pageSize 每页多少条
     * @return
     */
    @ApiOperation(value = "查看入库单详情", notes = "", httpMethod = "GET")
    @RequestMapping(value = "/in/detail", method = RequestMethod.GET)
    public VegaEntryGodownDetaDto inDetail(@RequestParam("entryGodownId") @ApiParam("入库单id") Long entryGodownId,
                                         @RequestParam(value = "pageNo", required = false) @ApiParam("页号") Integer pageNo,
                                         @RequestParam(value = "pageSize", required = false) @ApiParam("每页多少条") Integer pageSize) {
        if (Objects.isNull(UserUtil.getUserId())) {
            throw new JsonResponseException("service.no.access");
        }
        EntryGodown entryGodown = RespHelper.or500(entryGodownReadService.findById(entryGodownId));
        if (Objects.isNull(entryGodown)) {
            log.error("this entryGodown not exist entryGodownId = {}", entryGodownId);
            throw new JsonResponseException("entryGodown.detail.find.fail");
        }
        if (!Objects.equals(entryGodown.getUserId(), UserUtil.getUserId())) {
            log.error("no access to the service userId = {}", UserUtil.getUserId());
            throw new JsonResponseException("service.no.access");
        }
        VegaEntryGodownDetaDto vegaEntryGodownDetaDto = new VegaEntryGodownDetaDto();
        vegaEntryGodownDetaDto.setId(entryGodown.getId());
        vegaEntryGodownDetaDto.setStatus(entryGodown.getStatus());
        vegaEntryGodownDetaDto.setType(entryGodown.getType());
        vegaEntryGodownDetaDto.setFinishedAt(entryGodown.getFinishedAt());
        vegaEntryGodownDetaDto.setCreatedAt(entryGodown.getCreatedAt());
        vegaEntryGodownDetaDto.setUpdatedAt(entryGodown.getUpdatedAt());

        Paging<SonEntryGodown> sonEntryGodownsPaging = RespHelper.or500(InStorageReadService.pagingByEntryGodownId(entryGodown.getId(), pageNo, pageSize));
        if (Objects.isNull(sonEntryGodownsPaging)) {
            log.error("paging sonEntryGodown failed ");
            throw new JsonResponseException("sonEntryGodown.find.failed");
        }
        List<SonEntryGodown> sonEntryGodowns = sonEntryGodownsPaging.getData();
        if (Objects.isNull(sonEntryGodowns)) {
            sonEntryGodowns = Lists.newArrayList();
        }

        Map<Long, SonEntryGodownDetaDto> sonEntryGodownDetaDtoMap = sonEntryGodowns
                .stream()
                .map(sonEntryGodown -> {
                    SonEntryGodownDetaDto sonEntryGodownDateDto = new SonEntryGodownDetaDto();
                    sonEntryGodownDateDto.setId(sonEntryGodown.getId());
                    sonEntryGodownDateDto.setSkuId(sonEntryGodown.getSkuId());
                    sonEntryGodownDateDto.setType(sonEntryGodown.getType());
                    sonEntryGodownDateDto.setItemQuantity(sonEntryGodown.getItemQuantity());
                    sonEntryGodownDateDto.setStoragedNumber(sonEntryGodown.getStoragedNumber());
                    sonEntryGodownDateDto.setStatus(sonEntryGodown.getStatus());
                    sonEntryGodownDateDto.setEntryGodownId(sonEntryGodown.getEntryGodownId());
                    sonEntryGodownDateDto.setItemName(sonEntryGodown.getItemName());
                    sonEntryGodownDateDto.setItemImage(sonEntryGodown.getItemImage());
                    Sku sku = RespHelper.or500(skuReadService.findSkuById(sonEntryGodown.getSkuId()));
                    sonEntryGodownDateDto.setSkuAttributes(sku.getAttrs());
                    List<EntryJobDateDto> entryJobDateDtos = Lists.newLinkedList();
                    sonEntryGodownDateDto.setEntryJobs(entryJobDateDtos);
                    return sonEntryGodownDateDto;
                })
                .collect(Collectors.toMap(SonEntryGodownDetaDto::getId, sonEntryGodownDetaDto -> sonEntryGodownDetaDto));
        List<Long> sonEntryGodownIds = sonEntryGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<EntryJob> entryJobs = RespHelper.or500(entryJobReadService.findBySonEntryGodownIds(sonEntryGodownIds));
        entryJobs.forEach(entryJob -> {
            SonEntryGodownDetaDto sonEntryGodownDetaDto = sonEntryGodownDetaDtoMap.get(entryJob.getSonEntryGodownId());
            EntryJobDateDto entryJobDateDto = new EntryJobDateDto();
            entryJobDateDto.setCreatedAt(entryJob.getCreatedAt());
            entryJobDateDto.setQuantity(entryJob.getQuantity());
            entryJobDateDto.setSonEntryGodownId(entryJob.getSonEntryGodownId());
            Location location = RespHelper.or500(locationReadService.findByLocationId(entryJob.getLocationId()));
            if (Objects.isNull(location)) {
                log.error("this location by id = {} not exist", entryJob.getLocationId());
                throw new JsonResponseException("entryGodown.detail.find.fail");
            }
            LocationDateDto locationDateDto = new LocationDateDto();
            entryJobDateDto.setLocationDateDto(locationDateDto);
            Repertory repertory = RespHelper.or500(repertoryReadService.findById(location.getRepertoryId()));
            if (Objects.isNull(repertory)) {
                log.error("this repertory by id = {} not exist", location.getRepertoryId());
                throw new JsonResponseException("entryGodown.detail.find.fail");
            }
            locationDateDto.setReporteryName(repertory.getName());
            locationDateDto.setAreaName(location.getAreaName());
            locationDateDto.setGroupName(location.getGroupName());
            locationDateDto.setGroupName(location.getGroupName());
            locationDateDto.setShelfName(location.getShelfName());
            locationDateDto.setLocationName(location.getLocationName());
            sonEntryGodownDetaDto.getEntryJobs().add(entryJobDateDto);
        });
        List<SonEntryGodownDetaDto> sonEntryGodownDetaDtoList = sonEntryGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .sorted((o1, o2) -> {
                    if (o1.getId() > o2.getId()) {
                        return -1;
                    } else if (Objects.equals(o1.getId(), o2.getId())) {
                        return 0;
                    }
                    return 1;
                })
                .collect(Collectors.toList());
        vegaEntryGodownDetaDto.setSonEntrys(sonEntryGodownDetaDtoList);
        vegaEntryGodownDetaDto.setTotal(sonEntryGodownsPaging.getTotal());
        return vegaEntryGodownDetaDto;
    }

    /**
     * 改写查看入库单分页查询接口,原先接口:/api/storage/entrygodown/paging
     * 解决由于子单信息多引起的查询性能慢问题
     * 子单查询改成固定翻页形式
     */
    @ApiOperation(value = "对入库单进行分页查看", notes = "", httpMethod = "GET")
    @RequestMapping(value = "/in/paging", method = RequestMethod.GET)
    public Paging<EntryGodownDetaDto> pagingEntryGodown(@RequestParam(value = "pageNo", required = false) @ApiParam("页号") Integer pageNo,
                                             @RequestParam(value = "pageSize", required = false) @ApiParam("每页多少条") Integer size,
                                             @RequestParam(value = "status", required = false) @ApiParam("入库单状态") Integer status,
                                             @RequestParam(value = "entryGodownId", required = false) @ApiParam("入库单id") Long entryGodownId,
                                             @RequestParam(value = "type", required = false) @ApiParam("入库单类型") Integer type,
                                             @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @ApiParam("开始时间") DateTime startTime,
                                             @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") @ApiParam("结束时间") DateTime endTime) {
        if (Objects.isNull(UserUtil.getUserId())) {
            throw new JsonResponseException("service.no.access");
        }
        EntryGodown entryGodown = new EntryGodown();
        entryGodown.setStatus(status);
        entryGodown.setId(entryGodownId);
        entryGodown.setType(type);
        entryGodown.setUserId(UserUtil.getUserId());
        Date startDate = null;
        if(Objects.nonNull(startTime)) {
            startDate = startTime.secondOfDay().withMinimumValue().toDate();
        }
        Date endDate = null;
        if(Objects.nonNull(endTime)) {
            endDate = endTime.plusDays(1).secondOfDay().withMinimumValue().toDate();
        }
        Paging<EntryGodown> entryGodownPaging = RespHelper.or500(entryGodownReadService.paging(entryGodown, startDate, endDate, pageNo, size));
        if (Objects.isNull(entryGodownPaging)) {
            log.error("paging entryGodown failed ");
            throw new JsonResponseException("entryGodown.find.failed");
        }
        List<EntryGodown> entryGodowns = entryGodownPaging.getData();
        if (Objects.isNull(entryGodowns)) {
            entryGodowns = Lists.newArrayList();
        }
        //填充EntryGodown的各种值,stream会按照key值进行自然排序
        Map<Long, EntryGodownDetaDto> entryGodownDetaDtoMap = entryGodowns
                .stream()
                .map(entryGodownIterator -> {
                    EntryGodownDetaDto entryGodownDateDto = new EntryGodownDetaDto();
                    entryGodownDateDto.setId(entryGodownIterator.getId());
                    entryGodownDateDto.setStatus(entryGodownIterator.getStatus());
                    entryGodownDateDto.setType(entryGodownIterator.getType());
                    entryGodownDateDto.setCreatedAt(entryGodownIterator.getCreatedAt());
                    entryGodownDateDto.setUpdatedAt(entryGodownIterator.getUpdatedAt());
                    entryGodownDateDto.setFinishedAt(entryGodownIterator.getFinishedAt());
                    List<SonEntryGodownDetaDto> sonEntryGodownDateDtos = Lists.newLinkedList();
                    entryGodownDateDto.setSonEntrys(sonEntryGodownDateDtos);
                    entryGodownDateDto.setSonEntrys(Lists.newArrayList());
                    return entryGodownDateDto;
                })
                .collect(Collectors
                        .toMap(EntryGodownDetaDto::getId, entryGodownDetaDto -> entryGodownDetaDto)
                );
        List<Long> entryGodownIds = entryGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<SonEntryGodown> sonEntryGodowns = Lists.newArrayList();
        for(Long Id : entryGodownIds){
            //只查询前5条记录
            Paging<SonEntryGodown> sonEntryGodownsPaging = RespHelper.or500(InStorageReadService.pagingByEntryGodownId(Id, 1, 5));
            if (Objects.isNull(sonEntryGodownsPaging)) {
                log.error("paging sonEntryGodown failed entryGodownId={}",Id);
            }else{
                if(!sonEntryGodownsPaging.isEmpty()) {
                    sonEntryGodowns.addAll(sonEntryGodownsPaging.getData());
                }
            }
        }
        sonEntryGodowns.stream().forEach(
                sonEntryGodownIterator -> {
                    EntryGodownDetaDto entryGodownDetaDto = entryGodownDetaDtoMap.get(sonEntryGodownIterator.getEntryGodownId());
                    SonEntryGodownDetaDto sonEntryGodownDateDto = new SonEntryGodownDetaDto();
                    sonEntryGodownDateDto.setId(sonEntryGodownIterator.getId());
                    sonEntryGodownDateDto.setSkuId(sonEntryGodownIterator.getSkuId());
                    sonEntryGodownDateDto.setEntryGodownId(sonEntryGodownIterator.getId());
                    sonEntryGodownDateDto.setType(sonEntryGodownIterator.getType());
                    sonEntryGodownDateDto.setItemQuantity(sonEntryGodownIterator.getItemQuantity());
                    sonEntryGodownDateDto.setStoragedNumber(sonEntryGodownIterator.getStoragedNumber());
                    sonEntryGodownDateDto.setStatus(sonEntryGodownIterator.getStatus());
                    sonEntryGodownDateDto.setItemName(sonEntryGodownIterator.getItemName());
                    sonEntryGodownDateDto.setItemImage(sonEntryGodownIterator.getItemImage());
                    Sku sku = RespHelper.or500(skuReadService.findSkuById(sonEntryGodownIterator.getSkuId()));
                    sonEntryGodownDateDto.setSkuAttributes(sku.getAttrs());
                    entryGodownDetaDto.getSonEntrys().add(sonEntryGodownDateDto);
                });
        List<EntryGodownDetaDto> entryGodownDetaDtoList = entryGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .collect(Collectors.toList())
                .stream()
                .sorted((o1, o2) -> {
                    if (o1.getId() > o2.getId()) {
                        return -1;
                    } else if (Objects.equals(o1.getId(), o2.getId())) {
                        return 0;
                    }
                    return 1;
                })
                .collect(Collectors.toList());
        return new Paging<>(entryGodownPaging.getTotal(), entryGodownDetaDtoList);
    }

    /**
     * 1.改写查看出库单详情接口,原先接口:/api/storage/leave/godown/findById,新接口增加分页查询功能
     * 2.查看批量出库完成子单信息接口
     * @param leaveGodownId 出库单id
     * @param status 出库状态,查看批量出库完成时需传入
     * @param pageNo 页号
     * @param pageSize 每页条数
     * @return
     */
    @ApiOperation(value = "查看出库单详情&&查看批量出库完成子单信息", notes = "", httpMethod = "GET")
    @RequestMapping(value = "/out/detail", method = RequestMethod.GET)
    public VegaLeaveGodownDateDto outDetail(@RequestParam("leaveGodownId") @ApiParam("出库单id") Long leaveGodownId,
                                            @RequestParam(value = "status", required = false) @ApiParam("出库状态,查看批量出库完成时需传入") Integer status,
                                            @RequestParam(value = "pageNo", required = false) @ApiParam("页号") Integer pageNo,
                                            @RequestParam(value = "pageSize", required = false) @ApiParam("每页多少条") Integer pageSize) {
        if (Objects.isNull(UserUtil.getUserId())) {
            throw new JsonResponseException("service.no.access");
        }
        LeaveGodown leaveGodown = RespHelper.or500(leaveGodownReadService.findById(leaveGodownId));
        if (Objects.isNull(leaveGodown)) {
            log.error("this leaveGodown not exist by id = {}", leaveGodownId);
            throw new JsonResponseException("leaveGodown.not.find");
        }
        if (!Objects.equals(leaveGodown.getUserId(), UserUtil.getUserId())) {
            log.error("no access to the service userId = {}", UserUtil.getUserId());
            throw new JsonResponseException("service.no.access");
        }
        VegaLeaveGodownDateDto vegaLeaveGodownDateDto = new VegaLeaveGodownDateDto();
        vegaLeaveGodownDateDto.setId(leaveGodown.getId());
        vegaLeaveGodownDateDto.setStatus(leaveGodown.getStatus());
        vegaLeaveGodownDateDto.setType(leaveGodown.getType());
        vegaLeaveGodownDateDto.setCreatedAt(leaveGodown.getCreatedAt());
        vegaLeaveGodownDateDto.setUpdatedAt(leaveGodown.getUpdatedAt());
        vegaLeaveGodownDateDto.setFinishedAt(leaveGodown.getFinishedAt());

        Paging<SonLeaveGodown> sonLeaveGodownPaging = null;
        if(!Objects.isNull(status)&&status==SonLeaveGodownStatus.LEFT.getValue()){
            //出库中,可以进行"出库完成操作" status=3
            SonLeaveGodown sonLeaveGodown=new SonLeaveGodown();
            sonLeaveGodown.setLeaveGodownId(leaveGodownId);
            sonLeaveGodown.setStatus(status);
            sonLeaveGodownPaging = RespHelper.or500(outStorageReadService.pagingBySonLeaveGodown(sonLeaveGodown, pageNo, pageSize));
        }else{
            sonLeaveGodownPaging = RespHelper.or500(outStorageReadService.pagingByLeaveGodownId(leaveGodownId, pageNo, pageSize));
        }
        if (Objects.isNull(sonLeaveGodownPaging)) {
            log.error("paging sonLeaveGodown failed ");
            throw new JsonResponseException("sonLeaveGodown.not.find");
        }
        List<SonLeaveGodown> sonLeaveGodowns = sonLeaveGodownPaging.getData();
        if (Objects.isNull(sonLeaveGodowns)) {
            sonLeaveGodowns = Lists.newArrayList();
        }
        Map<Long, SonLeaveGodownDateDto> sonLeaveGodownDetaDtoMap = sonLeaveGodowns
                .stream()
                .map(sonLeaveGodown -> {
                    SonLeaveGodownDateDto sonLeaveGodownDateDto = new SonLeaveGodownDateDto();
                    sonLeaveGodownDateDto.setId(sonLeaveGodown.getId());
                    sonLeaveGodownDateDto.setItemName(sonLeaveGodown.getItemName());
                    sonLeaveGodownDateDto.setItemImage(sonLeaveGodown.getItemImage());
                    sonLeaveGodownDateDto.setType(sonLeaveGodown.getType());
                    sonLeaveGodownDateDto.setItemQuantity(sonLeaveGodown.getItemQuantity());
                    sonLeaveGodownDateDto.setLeavedNumber(sonLeaveGodown.getLeavedNumber());
                    sonLeaveGodownDateDto.setStatus(sonLeaveGodown.getStatus());
                    Sku sku = RespHelper.or500(skuReadService.findSkuById(sonLeaveGodown.getSkuId()));
                    if (Objects.isNull(sku)) {
                        log.error("this sku not exist by id = {}", sonLeaveGodown.getSkuId());
                        throw new JsonResponseException("sku.not.find");
                    }
                    sonLeaveGodownDateDto.setSkuAttributes(sku.getAttrs());
                    LeaveJob leaveJobQuery = new LeaveJob();
                    leaveJobQuery.setSonLeaveGodownId(sonLeaveGodown.getId());
                    List<LeaveJob> leaveJobs = RespHelper.or500(leaveJobReadService.findList(leaveJobQuery));
                    if (Objects.isNull(leaveJobs)) {
                        leaveJobs = Lists.newLinkedList();
                    }
                    List<LeaveJobDateDto> leaveJobDateDtos = Lists.newLinkedList();
                    sonLeaveGodownDateDto.setLeaveJobDateDtos(leaveJobDateDtos);
                    return sonLeaveGodownDateDto;
                })
                .collect(Collectors.toMap(SonLeaveGodownDateDto::getId, sonLeaveGodownDetaDto -> sonLeaveGodownDetaDto));
        List<Long> sonLeaveGodownIds = sonLeaveGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        if(Objects.isNull(sonLeaveGodownIds)||sonLeaveGodownIds.size()==0){
            log.error("find sonLeaveGodown failed ");
            throw new JsonResponseException("outRepertory.sonLeaveGodown.not.find");
        }
        List<LeaveJob> leaveJobs = RespHelper.or500(leaveJobReadService.findBySonLeaveGodownIds(sonLeaveGodownIds));
        leaveJobs.forEach(leaveJob -> {
            LeaveJobDateDto leaveJobDateDto = new LeaveJobDateDto();
            leaveJobDateDto.setQuantity(leaveJob.getQuantity());
            leaveJobDateDto.setCreatedAt(leaveJob.getCreatedAt());
            Location location = RespHelper.or500(locationReadService.findByLocationId(leaveJob.getLocationId()));
            if (Objects.isNull(location)) {
                log.error("this location not exist by id = {}", leaveJob.getLocationId());
                throw new JsonResponseException("location.not.find");
            }
            LocationDateDto locationDateDto = new LocationDateDto();
            Repertory repertory = RespHelper.or500(repertoryReadService.findById(location.getRepertoryId()));
            if (Objects.isNull(repertory)) {
                log.error("this repertory not exist by id = {}", location.getRepertoryId());
                throw new JsonResponseException("repertory.not.find");
            }
            locationDateDto.setReporteryName(repertory.getName());
            locationDateDto.setAreaName(location.getAreaName());
            locationDateDto.setGroupName(location.getGroupName());
            locationDateDto.setShelfName(location.getShelfName());
            locationDateDto.setLocationName(location.getLocationName());
            leaveJobDateDto.setLocationDateDto(locationDateDto);
            leaveJobDateDto.setSonLeaveGodownId(leaveJob.getSonLeaveGodownId());
            SonLeaveGodownDateDto sonLeaveGodownDateDto = sonLeaveGodownDetaDtoMap.get(leaveJobDateDto.getSonLeaveGodownId());
            sonLeaveGodownDateDto.getLeaveJobDateDtos().add(leaveJobDateDto);
        });
        List<SonLeaveGodownDateDto> sonLeaveGodownDateDtos = sonLeaveGodownDetaDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .sorted((o1, o2) -> {
                    if (o1.getId() > o2.getId()) {
                        return -1;
                    } else if (Objects.equals(o1.getId(), o2.getId())) {
                        return 0;
                    }
                    return 1;
                })
                .collect(Collectors.toList());
        vegaLeaveGodownDateDto.setSonLeaveGodownDateDtos(sonLeaveGodownDateDtos);
        vegaLeaveGodownDateDto.setTotal(sonLeaveGodownPaging.getTotal());
        return vegaLeaveGodownDateDto;
    }

    /**
     * 改写查看出库单分页查询接口,原先接口:/api/storage/leave/godown/paging
     * 解决由于子单信息多引起的查询性能慢问题
     * 子单查询改成固定翻页形式
     */
    @ApiOperation(value = "出库单分页展示", notes = "", httpMethod = "GET")
    @RequestMapping(value = "/out/paging", method = RequestMethod.GET)
    public Paging<LeaveGodownDateDto> pagingLeaveGodown(@RequestParam(value = "pageNo") @ApiParam("页号") Integer pageNo,
                                             @RequestParam(value = "pageSize") @ApiParam("每页多少条") Integer pageSize,
                                             @RequestParam(value = "status", required = false) @ApiParam("出库单状态") Integer status,
                                             @RequestParam(value = "leaveGodownId", required = false) @ApiParam("出库单id") Long leaveGodownId,
                                             @RequestParam(value = "type", required = false) @ApiParam("类型") Integer type,
                                             @RequestParam(value = "startTime", required = false) @ApiParam("开始时间") @DateTimeFormat(pattern = "yyyy-MM-dd") DateTime startTime,
                                             @RequestParam(value = "endTime", required = false) @ApiParam("结束时间") @DateTimeFormat(pattern = "yyyy-MM-dd") DateTime endTime) {
        if (Objects.isNull(UserUtil.getUserId())) {
            throw new JsonResponseException("service.no.access");
        }
        LeaveGodown leaveGodownQuery = new LeaveGodown();
        leaveGodownQuery.setId(leaveGodownId);
        leaveGodownQuery.setType(type);
        leaveGodownQuery.setStatus(status);
        leaveGodownQuery.setUserId(UserUtil.getUserId());
        Date startDate = null;
        if(Objects.nonNull(startTime)) {
            startDate = startTime.secondOfDay().withMinimumValue().toDate();
        }
        Date endDate = null;
        if(Objects.nonNull(endTime)) {
            endDate = endTime.plusDays(1).secondOfDay().withMinimumValue().toDate();
        }
        Paging<LeaveGodown> paging = RespHelper.or500(leaveGodownReadService.findByTime(leaveGodownQuery, startDate, endDate, pageNo, pageSize));
        List<LeaveGodown> leaveGodowns = paging.getData();
        if (Objects.isNull(leaveGodowns)) {
            leaveGodowns = Lists.newArrayList();
        }
        Map<Long, LeaveGodownDateDto> leaveGodownDateDtoMap = leaveGodowns
                .stream()
                .map(leaveGodownIterator -> {
                    LeaveGodownDateDto leaveGodownDateDto = new LeaveGodownDateDto();
                    leaveGodownDateDto.setId(leaveGodownIterator.getId());
                    leaveGodownDateDto.setCreatedAt(leaveGodownIterator.getCreatedAt());
                    leaveGodownDateDto.setUpdatedAt(leaveGodownIterator.getUpdatedAt());
                    leaveGodownDateDto.setFinishedAt(leaveGodownIterator.getFinishedAt());
                    leaveGodownDateDto.setStatus(leaveGodownIterator.getStatus());
                    leaveGodownDateDto.setType(leaveGodownIterator.getType());
                    List<SonLeaveGodownDateDto> sonLeaveGodownDateDtos = Lists.newLinkedList();
                    leaveGodownDateDto.setSonLeaveGodownDateDtos(sonLeaveGodownDateDtos);
                    return leaveGodownDateDto;
                })
                .collect(Collectors.toMap(LeaveGodownDateDto::getId, leaveGodownDateDto -> leaveGodownDateDto));
        List<Long> leaveGodownIds = leaveGodownDateDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<SonLeaveGodown> sonLeaveGodowns = Lists.newArrayList();
        for(Long Id : leaveGodownIds){
            //只查询前5条记录
            Paging<SonLeaveGodown> sonLeaveGodownsPaging = RespHelper.or500(outStorageReadService.pagingByLeaveGodownId(Id, 1, 5));
            if (Objects.isNull(sonLeaveGodownsPaging)) {
                log.error("paging sonLeaveGodowns failed leaveGodownId={}",Id);
            }else{
                if(!sonLeaveGodownsPaging.isEmpty()) {
                    sonLeaveGodowns.addAll(sonLeaveGodownsPaging.getData());
                }
            }
        }

        sonLeaveGodowns
                .stream()
                .forEach(sonLeaveGodownIterator -> {
                    SonLeaveGodownDateDto sonLeaveGodownDateDto = new SonLeaveGodownDateDto();
                    sonLeaveGodownDateDto.setId(sonLeaveGodownIterator.getId());
                    sonLeaveGodownDateDto.setSkuId(sonLeaveGodownIterator.getSkuId());
                    sonLeaveGodownDateDto.setType(sonLeaveGodownIterator.getType());
                    sonLeaveGodownDateDto.setStatus(sonLeaveGodownIterator.getStatus());
                    sonLeaveGodownDateDto.setItemName(sonLeaveGodownIterator.getItemName());
                    sonLeaveGodownDateDto.setItemImage(sonLeaveGodownIterator.getItemImage());
                    sonLeaveGodownDateDto.setCreatedAt(sonLeaveGodownIterator.getCreatedAt());
                    sonLeaveGodownDateDto.setUpdatedAt(sonLeaveGodownIterator.getUpdatedAt());
                    sonLeaveGodownDateDto.setLeavedNumber(sonLeaveGodownIterator.getLeavedNumber());
                    sonLeaveGodownDateDto.setItemQuantity(sonLeaveGodownIterator.getItemQuantity());
                    Sku sku = RespHelper.or500(skuReadService.findSkuById(sonLeaveGodownIterator.getSkuId()));
                    if (Objects.isNull(sku)) {
                        log.error("find sku failed by id = {}", sonLeaveGodownIterator.getSkuId());
                        throw new JsonResponseException("sku.not.find");
                    }
                    sonLeaveGodownDateDto.setSkuAttributes(sku.getAttrs());
                    LeaveGodownDateDto leaveGodownDateDto = leaveGodownDateDtoMap.get(sonLeaveGodownIterator.getLeaveGodownId());
                    leaveGodownDateDto.getSonLeaveGodownDateDtos().add(sonLeaveGodownDateDto);
                });
        List<LeaveGodownDateDto> leaveGodownDateDtos = leaveGodownDateDtoMap
                .entrySet()
                .stream()
                .map(Map.Entry::getValue)
                .sorted((o1, o2) -> {
                    if (o1.getId() > o2.getId()) {
                        return -1;
                    } else if (Objects.equals(o1.getId(), o2.getId())) {
                        return 0;
                    }
                    return 1;
                })
                .collect(Collectors.toList());
        return new Paging<>(paging.getTotal(), leaveGodownDateDtos);
    }

    /**
     * 批量进行出库完成接口
     * @param sonLeaveGodownIds 选中带出库的出库子单ID
     * @return Boolean
     */
    @ApiOperation(value = "批量完成出库", notes = "", httpMethod = "POST")
    @RequestMapping(value = "/out/batchFinish",method = RequestMethod.POST)
    public Boolean batchFinish(@RequestBody @ApiParam("出库子单ID") List<Long> sonLeaveGodownIds){
        if(Objects.isNull(UserUtil.getUserId())){
            throw new JsonResponseException("service.no.access");
        }
        if(Objects.isNull(sonLeaveGodownIds)){
            log.error("find sonLeaveGodownIds fail");
            throw new JsonResponseException("find.sonLeaveGodownIds.fail");
        }
        Boolean returns;
        for(Long sonLeaveGodownId : sonLeaveGodownIds){
            SonLeaveGodown sonLeaveGodown = RespHelper.or500(sonLeaveGodownReadService.findById(sonLeaveGodownId));
            if(Objects.isNull(sonLeaveGodown)){
                log.error("this sonLeaveGodown not exist by id = {}", sonLeaveGodownId);
                throw new JsonResponseException("sonLeaveGodown.not.find");
            }
            if(!Objects.equals(sonLeaveGodown.getUserId(), UserUtil.getUserId())){
                log.error("no access to the service userId = {}", UserUtil.getUserId());
                throw new JsonResponseException("service.no.access");
            }
            returns=RespHelper.or500(sonLeaveGodownWriteService.finish(sonLeaveGodownId, UserUtil.getUserId()));
            if(!returns){
                return Boolean.FALSE;
            }
        }
        return Boolean.TRUE;
    }



    /**
     * 老接口:api/locationLevel/findLevelsWithCategory
     * 分配入库/出库功能查询满足类目限制的子级
     * @param pid pid
     * @param repertoryId 仓储ID
     * @param inOrderId 入库单ID
     * @param outOrderId 出库单ID
     * @param filter 过滤条件
     * @return
     */
    @ApiOperation(value = "返回满足类目条件的子级并加标志", notes = "通过类目以及出入库要求进行过滤", httpMethod = "GET")
    @RequestMapping("/findLevelsWithCategory")
    public List<RepertoryLevelUseDto> findChildrenWithCategory(@RequestParam @ApiParam("pid")Long pid,
                                                               @RequestParam @ApiParam("repertoryId")Long repertoryId,
                                                               @RequestParam(required = false)@ApiParam("inOrderId") Long inOrderId,
                                                               @RequestParam(required = false)@ApiParam("outOrderId") Long outOrderId,
                                                               @RequestParam(required = false)@ApiParam("filter") Boolean filter){
        //权限校验
        checkRights(repertoryId);
        Long skuId = 0L;
        List<Location> locations = Lists.newArrayList();
        List<RepertoryToLocationLevel> levels = Lists.newArrayList();
        if (!Objects.isNull(outOrderId)){
            //出库分配
            SonLeaveGodown sonLeaveGodown=RespHelper.or500(sonLeaveGodownReadService.findById(outOrderId));
            if (Objects.isNull(sonLeaveGodown)) {
                log.error("this sonLeaveGodown not exist by id = {}", outOrderId);
                throw new JsonResponseException("sonLeaveGodown.not.find");
            }
            skuId = sonLeaveGodown.getSkuId();

            //根据repertoryId,skuId获取满足分配出库的库位信息
            locations = RespHelper.or500(vegaLocationReadService.findByRepertoryAndSku(repertoryId, skuId));
            if (Arguments.isNullOrEmpty(locations)) {
                log.error("find Location fail by repertoryId = {} skuId = {}", repertoryId,skuId);
                throw new JsonResponseException("Location.not.find");
            }
        }else {
            //入库分配
            SonEntryGodown sonEntryGodown=RespHelper.or500(sonEntryGodownReadService.findById(inOrderId));
            if (Objects.isNull(sonEntryGodown)) {
                log.error("this sonEntryGodown not exist by id = {}", inOrderId);
                throw new JsonResponseException("sonEntryGodown.not.find");
            }
            skuId = sonEntryGodown.getSkuId();
            //根据出/入库单id查找skuId查找itemId查找categoryId
            Long categoryId = findCategoryIdByOrderId(inOrderId,null);
            List<BackCategory> categoryList = RespHelper.or500(backCategoryReadService.findAncestorsOf(categoryId));
            List<Long> categoryIdList = Lists.transform(categoryList, BackCategory::getId);
            List<Long> categoryIds=Lists.newArrayList();
            categoryIds.addAll(categoryIdList);
            if(!categoryIds.contains(0L)) categoryIds.add(0L);
            List<Long> skuIds=Lists.newArrayList();
            skuIds.add(skuId);
            if(!skuIds.contains(0L)) skuIds.add(0L);
            //根据categoryIds,skuIds获取满足分配入库的库位信息
            locations = RespHelper.or500(vegaLocationReadService.findByCategoryAndSku(categoryIds, skuIds,repertoryId));
            if (Arguments.isNullOrEmpty(locations)) {
                log.warn("find Location fail by categoryId = {} skuId = {}", categoryId,skuId);
                //获取未绑定商品的库位信息
                locations = RespHelper.or500(vegaLocationReadService.findByCategoryAndSkuIsNull(repertoryId));
                if(Arguments.isNullOrEmpty(locations)) {
                    log.warn("find Location fail by repertoryId = {}, categoryId and skuId is null", repertoryId);
                    throw new JsonResponseException("Location.not.find");
                }
            }
        }

        List<Long> Location_ids = Lists.transform(locations, Location::getLocationId);//库位
        List<Long> Shelf_ids = Lists.transform(locations, Location::getShelfId);//货架
        List<Long> Group_ids = Lists.transform(locations, Location::getGroupId);//库组
        List<Long> Area_ids = Lists.transform(locations, Location::getAreaId);//库区
        List<Long> Ids=Lists.newArrayList();
        Ids.addAll(Location_ids);
        Ids.addAll(Shelf_ids);
        Ids.addAll(Group_ids);
        Ids.addAll(Area_ids);
        if(Ids.size()==0){
            if(!Objects.isNull(outOrderId)){
                log.info("out repertory fail cause not find location");
                throw new JsonResponseException("out.repertory.fail");
            }else {
                log.info("in repertory fail cause not find location");
                throw new JsonResponseException("in.repertory.fail");
            }
        }
        //查找当前仓库下的子级
        levels = RespHelper.or500(vegaRepertoryToLocationLevelsReadService.findChildrenByIds(pid,repertoryId,Ids));
        if (Objects.isNull(levels)) {
            log.error("find RepertoryToLocationLevel fail by pid = {} repertoryId = {} Ids = {}", pid,repertoryId,Ids);
            throw new JsonResponseException("RepertoryToLocationLevel.not.find");
        }

        List<RepertoryLevelUseDto> repertoryLevelUseDtoList;
        repertoryLevelUseDtoList = Lists.transform(levels, new Function<RepertoryToLocationLevel, RepertoryLevelUseDto>() {
            @Nullable
            @Override
            public RepertoryLevelUseDto apply(@Nullable RepertoryToLocationLevel repertoryToLocationLevel) {
                RepertoryLevelUseDto repertoryLevelUseDto = BeanMapper.map(repertoryToLocationLevel, RepertoryLevelUseDto.class);
                repertoryLevelUseDto.setCanUse(true);
                return repertoryLevelUseDto;
            }
        });

        //根据前端出入过滤需求决定是否进行过滤
        List<RepertoryLevelUseDto> repFilter;
        repFilter = FluentIterable.from(repertoryLevelUseDtoList).filter(repertoryLevelUseDto -> {
            if (!Objects.equals(filter,null) && Objects.equals(filter, true)) {
                return repertoryLevelUseDto.getCanUse();
            } else {
                return true;
            }
        }).toList();
        return repFilter;
    }

    /**
     * 用户权限校验
     */
    private void checkRights(Long repertoryId){
        Repertory repertory = RespHelper.or500(repertoryReadService.findById(repertoryId));
        if(Objects.isNull(repertory)){
            log.error("find repertory failed,repertoryId={}",repertoryId);
            throw new JsonResponseException(500, "repertory.find.failed");
        }
        if(!Objects.equals(repertory.getOwnerId(),UserUtil.getUserId())){
            log.error("no access to the service userId = {}", UserUtil.getUserId());
            throw new JsonResponseException("service.no.access");
        }
    }

    /**
     * 通过出库入库单ID查找绑定的类目id
     * @param inOrderId
     * @param outOrderId
     * @return
     */
    private Long findCategoryIdByOrderId(Long inOrderId,Long outOrderId){

        Long skuId = 0L;
        if(inOrderId !=null){
            SonEntryGodown sonEntryGodown = RespHelper.or500(sonEntryGodownReadService.findById(inOrderId));
            if (Objects.isNull(sonEntryGodown)) {
                log.error("sonEntryGodown find failed = {}", inOrderId);
                throw new JsonResponseException("sonEntryGodown.find.failed");
            }
            skuId = sonEntryGodown.getSkuId();
        }
        else if(outOrderId != null){
            SonLeaveGodown sonLeaveGodown = RespHelper.or500(sonLeaveGodownReadService.findById(outOrderId));
            if (Objects.isNull(sonLeaveGodown)) {
                log.error("sonLeaveGodown find failed = {}", outOrderId);
                throw new JsonResponseException("sonLeaveGodown.find.failed");
            }
            skuId = sonLeaveGodown.getSkuId();
        }
        else {
            return 0L;
        }
        Sku sku = RespHelper.or500(skuReadService.findSkuById(skuId));
        if(Objects.isNull(sku)){
            log.error("sku find failed = {}", inOrderId);
            throw new JsonResponseException("sku.find.failed");
        }
        Item item = RespHelper.or500(itemReadService.findById(sku.getItemId()));
        if(Objects.isNull(item)){
            log.error("item find failed = {}", inOrderId);
            throw new JsonResponseException("item.find.failed");
        }
        return item.getCategoryId();
    }
}
