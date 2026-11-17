package com.campus.assistant.ocr;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * OCR 网关：按配置在 HttpOcrService 与 NoopOcrService 之间选择。
 * OCR_ENABLED=false 或未配置 OCR_HTTP_URL 时走 Noop（返回 50301 提示）。
 */
@Component
@Primary
public class OcrGateway implements OcrService {

    private final HttpOcrService httpOcrService;
    private final NoopOcrService noopOcrService;

    @Value("${campus.ocr.enabled}")
    private boolean enabled;

    @Value("${campus.ocr.http-url:}")
    private String httpUrl;

    public OcrGateway(HttpOcrService httpOcrService, NoopOcrService noopOcrService) {
        this.httpOcrService = httpOcrService;
        this.noopOcrService = noopOcrService;
    }

    @Override
    public OcrResult recognize(byte[] image) {
        if (enabled && httpUrl != null && !httpUrl.isBlank()) {
            return httpOcrService.recognize(image);
        }
        return noopOcrService.recognize(image);
    }
}
