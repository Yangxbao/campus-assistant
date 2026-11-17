package com.campus.assistant.ocr;

import org.springframework.stereotype.Component;

/**
 * OCR 未配置时的默认实现：返回 50301 提示，绝不伪造识别结果。
 */
@Component
public class NoopOcrService implements OcrService {

    @Override
    public OcrResult recognize(byte[] image) {
        return OcrResult.notConfigured();
    }
}
