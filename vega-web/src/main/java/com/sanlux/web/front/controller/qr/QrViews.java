package com.sanlux.web.front.controller.qr;

import com.google.common.base.Throwables;
import com.google.common.io.ByteStreams;
import io.terminus.pay.web.util.QrHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 11/5/16
 * Time: 12:17 PM
 */
@RestController
@Slf4j
public class QrViews {


    @RequestMapping("/api/qr/view")
    public ResponseEntity<byte[]> qrView(@RequestParam("codeUrl") String codeUrl){

        File file;
        try {
            file = new QrHelper().getQrCode(codeUrl);
        }catch (Exception e){
            log.error("gen qr code fail, codeUrl={}, cause={}", codeUrl, Throwables.getStackTraceAsString(e));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        try(FileInputStream fileInputStream = new FileInputStream(file)){
            byte[] bytes = ByteStreams.toByteArray(fileInputStream);
            final HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            return new ResponseEntity<>(bytes, headers, HttpStatus.CREATED);
        }catch (Exception e){
            log.error("qrview fail, codeUrl={}, cause={}", codeUrl, Throwables.getStackTraceAsString(e));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
