package com.campus.assistant.common;

import lombok.Getter;

/**
 * 业务异常：携带业务码（非 0），由全局异常处理器统一包装为响应包。
 */
@Getter
public class BizException extends RuntimeException {
    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException of(int code, String message) {
        return new BizException(code, message);
    }

    // 常用业务码
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int BAD_REQUEST = 400;
    public static final int RATE_LIMITED = 42901;
    public static final int OCR_NOT_CONFIGURED = 50301;
}
