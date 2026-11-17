package com.campus.assistant.ocr;

/**
 * OCR 服务接口（契约第 5 节）。
 * 实现：NoopOcrService（未配置）、HttpOcrService（第三方 HTTP 接口）。
 * 预留 TesseractOcrService 扩展位。
 */
public interface OcrService {

    /**
     * 识别图片文本，返回原始文本与是否可用。
     * 未配置实现时可用性为 false，上层返回 code=50301"OCR 服务未配置"，绝不伪造识别结果。
     */
    OcrResult recognize(byte[] image);
}
