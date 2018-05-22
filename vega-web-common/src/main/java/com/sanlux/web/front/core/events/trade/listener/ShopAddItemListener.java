package com.sanlux.web.front.core.events.trade.listener;

import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.sanlux.item.dto.excel.UploadRaw;
import com.sanlux.item.enums.VegaTaskJobStatus;
import com.sanlux.item.model.TaskJob;
import com.sanlux.item.service.ShopSkuWriteService;
import com.sanlux.item.service.VegaTaskJobReadService;
import com.sanlux.item.service.VegaTaskJobWriteService;
import com.sanlux.web.front.core.events.ShopAddItemEvent;
import com.sanlux.web.front.core.util.ItemUploadExcelAnalyzer;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Created by cuiwentao
 * on 16/12/14
 */
@Slf4j
@Component
public class ShopAddItemListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private VegaTaskJobWriteService vegaTaskJobWriteService;

    @RpcConsumer
    private VegaTaskJobReadService vegaTaskJobReadService;

    @RpcConsumer
    private ShopSkuWriteService shopSkuWriteService;

    @Autowired
    private MessageSource messageSource;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }


    @Subscribe
    public void onShopAddItemEvent(ShopAddItemEvent event) {
        final String eventUrl = event.getUrl();
        final Long userId = event.getUserId();
        String key = "cache:job:userId" + userId + ":path:" + eventUrl;
        Response<TaskJob> jobResp = vegaTaskJobReadService.findByKey(key);
        if (!jobResp.isSuccess()) {
            log.error("find task job by key:{} fail, cause:{}", key, jobResp.getError());
        }
        TaskJob job = jobResp.getResult();
        job.setStatus(VegaTaskJobStatus.HANDLEING.value());
        Response<String> updateJobResp = vegaTaskJobWriteService.create(job);
        if (!updateJobResp.isSuccess()) {
            log.error("update task job fail, key:{}, cause:{}", key, updateJobResp.getError());
        }

        // 下载下来excel
        UploadRaw rawData = new UploadRaw();
        try {
            URL url = new URL("http:" + job.getExtra());
            if (log.isDebugEnabled()) {
                log.debug("start analyze excel from network ");
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            //Thread.sleep(10000);
            rawData = ItemUploadExcelAnalyzer.analyzeItemExcel(conn.getInputStream());

            conn.disconnect();

            if (log.isDebugEnabled()) {
                log.debug("end analyze excel from network");
            }
        } catch (MalformedURLException e) {
            log.error("failed to parse excel, error : {}", e.getMessage());
            jobFail(job, key, e.getMessage());
            return;
        } catch (IOException e) {
            log.error("failed to parse excel, cause : {}", e.getMessage());
            jobFail(job, key, e.getMessage());
            return;
        } catch (Exception e) {
            log.error("failed to parse excel, cause : {}", Throwables.getStackTraceAsString(e));
            jobFail(job, key, e.getMessage());
            return;
        }

        Response<Boolean> response = shopSkuWriteService.uploadToImportRaw(event.getShopId(), rawData);
        if (!response.isSuccess()) {
            log.error("batch create location fail, data error, cause:{}", response.getError());
            jobFail(job, key, response.getError());
            return;
        }

        jobSuccess(job, key);

    }

    private void jobSuccess(TaskJob job, String key) {
        job.setStatus(VegaTaskJobStatus.HANDLED.value());
        Response<String> updateJobResp = vegaTaskJobWriteService.create(job);
        if (!updateJobResp.isSuccess()) {
            log.error("update task job fail, key:{}, cause:{}", key, updateJobResp.getError());
        }
    }

    private void jobFail(TaskJob job, String key, String error) {
        job.setError(messageSource.getMessage(error, null, error, Locale.CHINA));
        job.setStatus(VegaTaskJobStatus.FAIL.value());
        Response<String> updateJobResp = vegaTaskJobWriteService.create(job);
        if (!updateJobResp.isSuccess()) {
            log.error("update task job fail, key:{}, cause:{}", key, updateJobResp.getError());
        }

    }
}
