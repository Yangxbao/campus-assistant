package com.campus.assistant.service;

import com.campus.assistant.common.BizException;
import com.campus.assistant.common.RedisHelper;
import com.campus.assistant.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 【新手导读】上传服务 —— 所有图片上传的公共入口（管理端照片、小程序照片、头像都走这里）。
 * 三件事：① Redis 限流（防刷）→ ② 委托 StorageService 存文件（校验格式/大小/EXIF 清洗）
 *        → ③ 返回 { ossKey, url }（ossKey 是相对路径存数据库，url 是前端可直接 <img> 的地址）。
 */
@Service
@RequiredArgsConstructor
public class UploadService {

    private final StorageService storageService;
    private final RedisHelper redis;

    /**
     * multipart 上传（浏览器/管理后台走这个）：前端 FormData 里的 file 字段由 Spring 自动包装成 MultipartFile。
     */
    public Map<String, Object> uploadImage(long uid, MultipartFile file) {
        checkRate(uid);                              // 每人每分钟最多 10 张，超了抛 42901
        String ossKey = storageService.store(file);  // 存盘，返回相对路径
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ossKey", ossKey);
        result.put("url", "/api/files/" + ossKey);   // /api/files/** 已映射到 upload-dir 目录
        return result;
    }

    /**
     * base64 上传（小程序开发工具场景）：
     * 微信开发者工具的 wx.uploadFile 对本地临时文件支持不稳，所以小程序把图片读成
     * base64 文本放进 JSON body 发过来，这里解码还原成字节再走同样的存储流程。
     * 真机上也可以直接用 multipart 接口（/api/mp/upload/image）。
     */
    public Map<String, Object> uploadBase64(long uid, String base64, String ext) {
        checkRate(uid);
        if (base64 == null || base64.isBlank()) {
            throw BizException.of(BizException.BAD_REQUEST, "图片内容不能为空");
        }
        // data:image/png;base64,xxxx → 去掉逗号前的"说明头"，只留纯 base64 数据
        String data = base64.contains(",") ? base64.substring(base64.indexOf(',') + 1) : base64;
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            throw BizException.of(BizException.BAD_REQUEST, "base64 解码失败");
        }
        if (bytes.length > 8 * 1024 * 1024) {
            throw BizException.of(BizException.BAD_REQUEST, "文件超过 8MB 限制");
        }
        String e = ext == null ? "png" : ext.toLowerCase();
        if (!List.of("jpg", "jpeg", "png", "webp").contains(e)) {
            throw BizException.of(BizException.BAD_REQUEST, "仅支持 jpg/png/webp 格式");
        }
        MultipartFile mf = new InMemoryMultipartFile(bytes, "image." + ("jpeg".equals(e) ? "jpg" : e));
        String ossKey = storageService.store(mf);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ossKey", ossKey);
        result.put("url", "/api/files/" + ossKey);
        return result;
    }

    /**
     * 限流：每个 uid 每分钟最多 10 张（Redis 计数器实现）。
     * Redis 挂了会自动放行（项目约定：Redis 只做加速/防护，不可用时功能降级不中断）。
     */
    private void checkRate(long uid) {
        // 每分钟 10 张（Redis 不可用时自动放行）
        if (redis.rateLimited("rate:upload:" + uid, 10, Duration.ofMinutes(1))) {
            throw BizException.of(BizException.RATE_LIMITED, "上传过于频繁，请稍后再试");
        }
    }

    /** 内存 MultipartFile 适配器 */
    static class InMemoryMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String name;

        InMemoryMultipartFile(byte[] bytes, String name) {
            this.bytes = bytes;
            this.name = name;
        }

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public boolean isEmpty() {
            return bytes.length == 0;
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        @Override
        public byte[] getBytes() {
            return bytes;
        }

        @Override
        public java.io.InputStream getInputStream() {
            return new java.io.ByteArrayInputStream(bytes);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
