package com.sanlux.web.front.core.utils;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;

/**
 * word生成工具类
 * Created by lujm on 2018/1/12.
 */
@Slf4j
public class ExportWordHelper {

    /**
     * word文件生成
     * @param dataMap      word中需要展示的动态数据，用map集合来保存
     * @param templateName word模板名称，例如：test.ftl
     */
    @SuppressWarnings("unchecked")
    public static void createWord(Map<String, Object> dataMap, String templateName, OutputStream outputStream){
        try {
            //创建配置实例
            Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
            //设置编码
            configuration.setDefaultEncoding("UTF-8");
            //ftl模板文件路径
            configuration.setClassLoaderForTemplateLoading(ExportWordHelper.class.getClassLoader(), "excel/");
            //获取模板
            Template template = configuration.getTemplate(templateName);
            //将模板和数据模型合并生成文件
            Writer out = new BufferedWriter(new OutputStreamWriter(outputStream));
            //生成文件
            template.process(dataMap, out);
            //关闭流
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
            log.info("create word failed, templateName:{} ", templateName);
            throw new JsonResponseException("create.word.fail");
        }
    }
}
