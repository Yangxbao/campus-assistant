package com.campus.assistant.ocr;

import lombok.AllArgsConstructor;
import lombok.Data;

/** OCR 识别结果 */
@Data
@AllArgsConstructor
public class OcrResult {
    /** OCR 服务是否可用 */
    private boolean available;
    /** 识别出的原始文本 */
    private String rawText;
    /** 失败/未配置时的提示 */
    private String message;

    public static OcrResult notConfigured() {
        return new OcrResult(false, null, "OCR 服务未配置，请手动录入");
    }
}
