package com.sanlux.pay.direct.utils;


import com.sanlux.pay.direct.dto.PayBusinessInfo;
import com.sanlux.pay.direct.dto.PayFunctionInfo;
import com.sanlux.pay.direct.dto.PayRequestDto;
import com.sanlux.pay.direct.dto.PayRequestSystemInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.*;


/**
 * Created by liangfujie on 16/10/26
 */
@Slf4j
public class DirectXmlHelper {



    public static String BeanToXml(Object object) {
        StringBuffer stringBuffer = new StringBuffer("<?xml version=\"1.0\" encoding = \"GBK\"?>");
        try {
            JAXBContext context = JAXBContext.newInstance(object.getClass());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            StringWriter stringWriter = new StringWriter();
            marshaller.marshal(object, stringWriter);
            stringBuffer.append(stringWriter.toString());
            return stringBuffer.toString();

        } catch (JAXBException e) {
            log.error("bean to xml failed cause{}", e.getMessage());
        }
        return null;

    }


    public static <T> T xmlToBean(String xml, Class<T> c) {
        T object = null;
        try {
            JAXBContext context = JAXBContext.newInstance(c);
            Unmarshaller unMarshal = context.createUnmarshaller();
            object = (T) unMarshal.unmarshal(new StringReader(xml));
            return object;

        } catch (Exception e) {
            log.error("xml to bean failed,cause{}", e.getMessage());
        }
        return null;

    }


}
