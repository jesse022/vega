package com.vega.web;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

/**
 * Mail: xiao@terminus.io <br>
 * Date: 2016-05-11 10:02 AM  <br>
 * Author: xiao
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseWebTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AnnotationConfigEmbeddedWebApplicationContext context;


    protected int getPort() {
        return context.getEmbeddedServletContainer().getPort();
    }


    protected <T> T getForObject(String url, Class<T> responseType) {
        return restTemplate.getForObject(url(url), responseType);
    }

    protected <T> ResponseEntity<T> postForEntity(String url, Object requestObject, Class<T> responseType) {
        return restTemplate.postForEntity(url(url), requestObject, responseType);
    }

    protected <T> T postForObject(String url, Object requestObject, Class<T> responseType) {
        return restTemplate.postForObject(url(url), requestObject, responseType);
    }

    protected <T> T postForObject(String url, Object requestObject, Class<T> responseType, HttpHeaders headers) {
        HttpEntity<Object> entity = new HttpEntity<>(requestObject, headers);
        ResponseEntity<T> response = restTemplate.exchange(url(url), HttpMethod.POST, entity , responseType);
        return response.getBody();
    }

    protected <T> ResponseEntity<T> putForEntity(String url, Object requestObject, Class<T> responseType, HttpHeaders httpHeaders) {
        HttpEntity<Object> entity;
        if (httpHeaders  == null) {
            HttpHeaders headers = new HttpHeaders();
            entity = new HttpEntity<>(requestObject, headers);
        } else {
            entity = new HttpEntity<>(requestObject, httpHeaders);
        }
        return restTemplate.exchange(url(url), HttpMethod.PUT, entity , responseType);
    }

    protected <T> T putForObject(String url, Object object, Class<T> responseType) {
        ResponseEntity<T> response = putForEntity(url, object, responseType, null);
        return response.getBody();
    }

    protected <T> T putForObject(String url, Object object, Class<T> responseType, HttpHeaders httpHeaders) {
        ResponseEntity<T> response = putForEntity(url, object, responseType, httpHeaders);
        return response.getBody();
    }


    protected <T> ResponseEntity<T> postFormForEntity(String url, Map<String, Object> form, Class<T> responseType, HttpHeaders httpHeaders) {
        MultiValueMap<String, Object> params = new LinkedMultiValueMap<>(form.size());
        params.setAll(form);

        if (httpHeaders == null) {
            httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        }


        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(params, httpHeaders);
        return restTemplate.exchange(url(url), HttpMethod.POST, entity, responseType);
    }

    protected <T> T postFormForObject(String url, Map<String, Object> form, Class<T> responseType) {
        ResponseEntity<T> response = postFormForEntity(url, form, responseType, null);
        return response.getBody();
    }

    protected <T> T postFormForObject(String url, Map<String, Object> form, Class<T> responseType, HttpHeaders headers) {
        ResponseEntity<T> response = postFormForEntity(url, form, responseType, headers);
        return response.getBody();
    }

    private String url(String url) {
        return "http://localhost:" + getPort() + url;
    }

}
