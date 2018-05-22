package com.sanlux.web.front.core.utils;

import com.sanlux.pay.direct.dto.*;
import com.sanlux.pay.direct.utils.DirectXmlHelper;
import io.terminus.common.utils.Arguments;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Created by liangfujie on 16/10/27
 */
@Slf4j
@ConfigurationProperties(prefix = "direct.pay")
@Component
public class DirectClient implements Serializable {



    private static final long serialVersionUID = 5955222368288843719L;
    //请求地址
    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {

        this.url = url;
    }

    public String postXmlToDirectSystem(String xml) throws Exception {
        HttpURLConnection urlConnection = null;
        OutputStream outputStream = null;
        BufferedReader bufferedReader = null;

        try {

            URL httpUrl = new URL(url);
            urlConnection = (HttpURLConnection) httpUrl.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            //urlConnection.setRequestProperty("Pragma:", "no-cache");
            urlConnection.setRequestProperty("Cache-Control", "no-cache");
            urlConnection.setRequestProperty("Content-Type", "text/xml");
            urlConnection.setConnectTimeout(6000);
            urlConnection.connect();
            outputStream = urlConnection.getOutputStream();
            byte[] bytes = xml.getBytes("GBK");
            outputStream.write(bytes);
            StringBuffer temp = new StringBuffer();
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            Reader rd = new InputStreamReader(in,"GBK");
            int c = 0;
            while ((c = rd.read()) != -1) {
                temp.append((char) c);
            }
            in.close();
            return temp.toString();

        } catch (MalformedURLException e) {
            log.error("connect server failed,cause{}", e.getMessage());

        } catch (IOException e) {
            log.error("io execute failed,cause{}", e.getMessage());
            throw e;

        } finally {
            try {

                if (!Arguments.isNull(outputStream)) {
                    outputStream.close();
                }
                if (!Arguments.isNull(bufferedReader)) {
                    bufferedReader.close();
                }

            } catch (IOException e) {
                log.error("io close failed , cause{}", e.getMessage());
            }

        }

        return null;

    }

    public String postXmlToDirectSystem2(String xml) {
        HttpURLConnection httpConn = null;
        try {
            // 建立连接
            URL httpUrl = new URL(url);
            httpConn = (HttpURLConnection) httpUrl.openConnection();

            //设置连接属性
            httpConn.setConnectTimeout(6000);// 设置连接超时时间，单位毫秒
            httpConn.setReadTimeout(6000);// 设置读取数据超时时间，单位毫秒
            httpConn.setDoOutput(true);// 使用 URL 连接进行输出
            httpConn.setDoInput(true);// 使用 URL 连接进行输入
            httpConn.setUseCaches(false);// 忽略缓存
            httpConn.setRequestMethod("POST");// 设置URL请求方法

            // 设置请求属性
            // 获得数据字节数据，请求数据流的编码，必须和下面服务器端处理请求流的编码一致
            byte[] requestStringBytes = xml.getBytes("GBK");
            httpConn.setRequestProperty("Content-length", "" + requestStringBytes.length);
            httpConn.setRequestProperty("Content-Type", "application/octet-stream");
            httpConn.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
            httpConn.setRequestProperty("Charset", "GBK");

            // 建立输出流，并写入数据
            OutputStream outputStream = httpConn.getOutputStream();
            outputStream.write(requestStringBytes);
            outputStream.close();

            // 获得响应状态
            int responseCode = httpConn.getResponseCode();


            if (HttpURLConnection.HTTP_OK == responseCode) {
                // 连接成功
                // 当正确响应时处理数据
                StringBuffer sb = new StringBuffer();
                String readLine;
                BufferedReader responseReader;
                // 处理响应流，必须与服务器响应流输出的编码一致
                responseReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream(), "GBK"));
                while ((readLine = responseReader.readLine()) != null) {
                    sb.append(readLine).append("\n");
                }
                responseReader.close();
                return sb.toString();
            }
        } catch (Exception e) {
            log.error("connect server failed,cause{}", e.getMessage());
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();// 关闭连接
            }
        }
        return null;
    }



    public static void main(String[] args) {
        //test1();
        test2();

    }


    public static void test1(){
        DirectClient directClient =new DirectClient();
        directClient.setUrl("http://122.224.57.20:8080");
        PayRequestDto payRequestDto = new PayRequestDto();
        PayRequestSystemInfo payRequestSystemInfo = new PayRequestSystemInfo();
        PayFunctionInfo payFunctionInfo = new PayFunctionInfo();
        PayBusinessInfo payBusinessInfo = new PayBusinessInfo();
        payRequestSystemInfo.setLGNNAM("银企直连专用集团1");
        payRequestSystemInfo.setDATTYP(2);
        payRequestSystemInfo.setFUNNAM("DCPAYREQ");
        payBusinessInfo.setBUSCOD("N02030");
        payBusinessInfo.setBUSMOD("00001");
        payFunctionInfo.setYURREF("201611040847110000000000001143");
        payFunctionInfo.setDBTACC("591902896710201");
        payFunctionInfo.setDBTBBK("59");///
        payFunctionInfo.setCCYNBR("10");
        payFunctionInfo.setSTLCHN("N");
        payFunctionInfo.setNUSAGE("转账");
        payFunctionInfo.setBUSNAR("转账");
        payFunctionInfo.setCRTACC("591902896710704");
        payFunctionInfo.setCRTNAM("银企直连专用账户9");
        payFunctionInfo.setBRDNBR("308391026010");
        payFunctionInfo.setTRSAMT("2.0");
        payFunctionInfo.setBNKFLG("Y");
        payFunctionInfo.setCRTBNK("招商银行福州分行");
        payFunctionInfo.setCRTPVC("福建省");
        payFunctionInfo.setCRTCTY("福州市");

        payRequestDto.setSystemInfo(payRequestSystemInfo);
        payRequestDto.setFunctionInfo(payFunctionInfo);
        payRequestDto.setBusinessInfo(payBusinessInfo);
        String xml = DirectXmlHelper.BeanToXml(payRequestDto);

        String response = null;
        try {
            response = directClient.postXmlToDirectSystem(xml);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //PayResponseDto payResponseDto = DirectXmlHelper.xmlToBean(response,PayResponseDto.class);
        System.out.println(response);

    }



    public static void test2()  {
        DirectClient directClient =new DirectClient();
        directClient.setUrl("http://122.224.57.20:8080");
       QueryRequestDto queryRequestDto =new QueryRequestDto();
        QueryRequestSystemInfo queryRequestSystemInfo =new QueryRequestSystemInfo();
        queryRequestSystemInfo.setFUNNAM("GetPaymentInfo");
        queryRequestSystemInfo.setLGNNAM("金敏2");
        queryRequestSystemInfo.setDATTYP(2);
        QueryRequestBody queryRequestBody =new QueryRequestBody();
        queryRequestBody.setDATFLG("B");
        queryRequestBody.setYURREF("201707241631530000000000000213");
        //DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String date ="20170722";
        String date1 ="20170725";
        queryRequestBody.setENDDAT(date1);
        queryRequestBody.setBGNDAT(date);
        queryRequestDto.setQueryRequestSystemInfo(queryRequestSystemInfo);
        queryRequestDto.setQueryRequestBody(queryRequestBody);
        String str =DirectXmlHelper.BeanToXml(queryRequestDto);
        try {
           String str1 = directClient.postXmlToDirectSystem(str);



            QueryResponseDto queryResponseDto = (QueryResponseDto) DirectXmlHelper.xmlToBean(str1, QueryResponseDto.class);

           if (queryResponseDto.getQueryResponseBody().getRTNFLG().trim().equals("S")){
            System.out.print(queryResponseDto.getQueryResponseBody().getRTNFLG());
               System.out.println(queryResponseDto.getQueryResponseBody().getRTNFLG());}
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
