package com.campus.assistant.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 第三方 OCR HTTP 实现：POST multipart file 到配置的 OCR 接口，取返回 JSON 中的文本字段。
 * 接口地址/Token 通过 OCR_HTTP_URL、OCR_HTTP_TOKEN 环境变量配置。
 */
@Slf4j
@Component
public class HttpOcrService implements OcrService {

    @Value("${campus.ocr.http-url:}")
    private String httpUrl;

    @Value("${campus.ocr.http-token:}")
    private String httpToken;

    @Value("${campus.ocr.text-field:data.text}")
    private String textField;

    private final RestClient restClient = RestClient.create();

    @Override
    public OcrResult recognize(byte[] image) {
        try {
            var multipart = new org.springframework.util.LinkedMultiValueMap<String, Object>();
            var resource = new org.springframework.core.io.ByteArrayResource(image);
            multipart.add("file", resource);
            if (httpToken != null && !httpToken.isBlank()) {
                multipart.add("token", httpToken);
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resp = restClient.post()
                    .uri(httpUrl)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipart)
                    .retrieve()
                    .body(java.util.Map.class);
            String text = extractField(resp, List.of(textField.split("\\.")));
            return new OcrResult(text != null, text, text == null ? "OCR 返回中未找到文本字段" : null);
        } catch (Exception e) {
            log.warn("HTTP OCR 调用失败: {}", e.getMessage());
            return new OcrResult(false, null, "OCR 调用失败：" + e.getMessage());
        }
    }

    /** 按点分路径从嵌套 JSON 取文本，如 data.text */
    @SuppressWarnings("unchecked")
    private String extractField(java.util.Map<String, Object> map, List<String> path) {
        Object cur = map;
        for (String p : path) {
            if (!(cur instanceof java.util.Map<?, ?> m)) {
                return null;
            }
            cur = m.get(p);
        }
        return cur == null ? null : String.valueOf(cur);
    }
}
