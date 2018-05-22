package com.sanlux.pay.allinpay.request;

import com.allinpay.ets.client.SecurityUtil;
import com.allinpay.ets.client.util.Base64;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sanlux.pay.allinpay.token.AllinpayToken;
import com.sanlux.pay.allinpay.trans.AllinpayTrans;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.CollectionUtils;

import javax.net.ssl.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkState;

/**
 * 封装支付请求参数
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 10/17/16
 * Time: 5:29 PM
 */
@Slf4j
public class AllinpayTransRequest extends AllinpayRequest {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormat.forPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    private AllinpayTransRequest(AllinpayToken allinpayToken) {
        super(allinpayToken);
    }

    public static AllinpayTransRequest build(AllinpayToken allinpayToken) {
        return new AllinpayTransRequest(allinpayToken);
    }


    /**
     * 结算时间
     * @param settleDate 结算时间
     * @return this
     */
    public AllinpayTransRequest settleDate(String settleDate) {
            params.put("settleDate", settleDate);
        return this;
    }


    public String url() {
        sign();
         Map<String, Object> paramsMap = Maps.newTreeMap();
        paramsMap.put("mchtCd",params.get("merchantId"));
        paramsMap.put("settleDate",params.get("settleDate"));
        paramsMap.put("signMsg",params.get("signMsg"));
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(paramsMap);
        return allinpayToken.getBillGateway() + "?" + suffix;
    }


    /**
     * 向通联网关发送账务查询请求
     *
     * @return 退货查询请求结果
     */
    public Response<List<AllinpayTrans>> trandQuery() {
        String url = url();
        //创建变量srcStr-对账文件source
        StringBuilder srcStr = new StringBuilder();
        //创建变量mac-签名信息
        String mac = "";
        //定义标志位 0表示初始位 1表示读取签名位
        int flag = 0;
        List<String> trans = Lists.newArrayList();
        log.debug("allinpay trans query url: {}", url);
        HttpURLConnection httpConn  = HttpRequest.post(url).getConnection();
        try {
            httpConn.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            String lines;
            while ((lines = reader.readLine()) != null) {

                if (flag == 1) {
                    mac = lines;
                    break;
                }
                if (lines.length() > 0) {
                    trans.add(lines);
                    srcStr.append(lines).append("\r\n");
                } else {
                    flag = 1;
                }
            }
            //关闭BufferedReader
            reader.close();
            // 断开连接
            httpConn.disconnect();
        } catch (IOException e) {
            log.error("allinpay trans query fail cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("allinpay.trans.query.fail");
        }
        log.info("allinpay trans is:{}",srcStr.toString());
        return convertToResponse(trans,srcStr.toString(),mac);
    }


    protected Response<List<AllinpayTrans>> convertToResponse(List<String>trans , String srcStr, String signMsg) {
        Response<List<AllinpayTrans>> result = new Response<List<AllinpayTrans>>();
        checkState(!CollectionUtils.isEmpty(trans), "allinpay.trans.query.fail");

        List<AllinpayTrans> transDatas = Lists.newArrayList();
        String errorCode = trans.get(0);

        //验签是商户为了验证接收到的报文数据确实是支付网关发送的。
        //构造订单结果对象，验证签名。
        if(!errorCode.contains("ERRORCODE")) {
            //如果errorCode为空，说明返回正确报文信息，接下来对报文进行解析验签
            Splitter splitter = Splitter.on("|");
            Boolean isSkip =Boolean.TRUE;
            for (String data : trans){
                if(isSkip){
                    isSkip = Boolean.FALSE;
                     continue;
                }
                AllinpayTrans transData = new AllinpayTrans();
                List<String> list = splitter.splitToList(data);
                Date transDate = DATE_TIME_FORMAT.parseDateTime(list.get(1)).toDate();
                Date tradeAt = TIME_FORMATTER.parseDateTime(list.get(3)).toDate();

                transData.setTransCodeMsg(list.get(0));
                transData.setTransDate(transDate);
                transData.setSellerAccount(list.get(2));
                transData.setTradeAt(tradeAt);
                transData.setTransOutOrderNo(list.get(4));
                transData.setTradeNo(list.get(5));
                transData.setTotalFee(list.get(6));
                transData.setServiceFee(list.get(7));
                transData.setServiceFeeRatio(null);
                transData.setSettlementFee(list.get(8));
                transData.setCurrency(list.get(9));
                transData.setOrderOriginFee(list.get(10));
                transData.setMemo("");
                transData.setCreatedAt(new Date());
                transData.setUpdatedAt(new Date());
                transDatas.add(transData);

            }
            if (verify(srcStr,signMsg)) {
                result.setResult(transDatas);
            }else{
                result.setError("验签失败");
            }
        }else {
            result.setError(errorCode);
        }
        return result;
    }

    @Override
    public void sign(){
        String  mchtCd =String.valueOf(params.get("merchantId"));
        String  settleDate =String.valueOf(params.get("settleDate"));
        String  md5key =String.valueOf(params.get("key"));
        // 得到摘要(MD5Encode函数的传入参数为商户号+结算日期+md5key)
        String signMsg = SecurityUtil.MD5Encode(mchtCd + settleDate + md5key);
        params.put("signMsg", signMsg);
    }

    private Boolean verify(String signString,String singMsg){
        String fileMd5String = SecurityUtil.MD5Encode(signString);
        return SecurityUtil.verifyByRSA(allinpayToken.getCertPath(),fileMd5String.getBytes(), Base64.decode(singMsg));
    }




    public HttpURLConnection getHttpsURLConnection(URL url) {

        try {
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();

            if ("https".equals(url.getProtocol())) // 如果是https协议
            {
                HttpsURLConnection httpsConn = (HttpsURLConnection) httpConnection;
                TrustManager[] managers = { new MyX509TrustManager() };// 证书过滤
                SSLContext sslContext;
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, managers, new SecureRandom());
                SSLSocketFactory ssf = sslContext.getSocketFactory();
                httpsConn.setSSLSocketFactory(ssf);
                httpsConn.setHostnameVerifier(new MyHostnameVerifier());// 主机名过滤
                return httpsConn;

            }
            return httpConnection;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        return null;

    }
    /*
     * 编写证书过滤器
     * @author: Robin
     * @date : 2011-03-10
     */
    public class MyX509TrustManager implements X509TrustManager
    {

        /**
         * 该方法体为空时信任所有客户端证书
         *
         * @param chain
         * @param authType
         * @throws CertificateException
         */
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }
        /**
         * 该方法体为空时信任所有服务器证书
         *
         * @param chain
         * @param authType
         * @throws CertificateException
         */

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
        }

        /**
         * 返回信任的证书
         * @return
         */
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }
    /*
     * 编写主机名过滤器
     * @author: Robin
     * @date : 2011-03-10
     */
    private class MyHostnameVerifier implements HostnameVerifier
    {

        /**
         * 返回true时为通过认证 当方法体为空时，信任所有的主机名
         *
         * @param hostname
         * @param session
         * @return
         */
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }

    }

}
