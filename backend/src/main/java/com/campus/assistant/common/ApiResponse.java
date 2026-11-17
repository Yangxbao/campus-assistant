package com.campus.assistant.common;

import lombok.Data;

/**
 * 【新手导读】统一响应包 —— 后端所有接口都返回这个结构，前后端约定的"普通话"。
 *
 *   { "code": 0,        // 0 = 成功；非 0 = 各种失败（401 未登录 / 403 没权限 / 400 参数错…）
 *     "message": "ok",  // 给人看的提示语
 *     "data": {...}     // 真正的业务数据，失败时为 null
 *   }
 *
 * 好处：前端只需写一套处理逻辑（先判 code 再用 data），不用猜每个接口的返回格式。
 * 泛型 <T>：data 里装什么都行（列表、对象、分页结果……），编译期就有类型检查。
 */
@Data
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;

    /** 成功 + 带数据。比如 ApiResponse.ok(list) */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = 0;
        r.message = "ok";
        r.data = data;
        return r;
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        ApiResponse<T> r = new ApiResponse<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
