package com.campus.assistant.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 存储服务接口：本地磁盘实现 + 预留 MinIO/OSS 扩展位。
 * 生产切换只需实现本接口并替换 Bean。
 */
public interface StorageService {

    /**
     * 保存上传文件，返回 ossKey（相对键，如 img/20261116/xxxx.jpg）。
     * 实现方负责校验格式（jpg/png/webp）与大小（<=8MB）、EXIF 清洗、随机文件名。
     */
    String store(MultipartFile file);

    /** 按 ossKey 读取原始字节（OCR 等场景使用） */
    byte[] read(String ossKey);
}
