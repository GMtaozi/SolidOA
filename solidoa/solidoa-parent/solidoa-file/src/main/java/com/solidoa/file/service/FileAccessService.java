package com.solidoa.file.service;

import com.solidoa.file.entity.File;

/**
 * 文件访问权限校验接口
 */
public interface FileAccessService {

    /**
     * 校验用户是否有权访问文件
     * @param file 文件实体
     * @param userId 当前用户ID
     * @return true=有权限, false=无权限
     */
    boolean canAccess(File file, Long userId);

    /**
     * 校验用户是否有权下载文件
     * @param file 文件实体
     * @param userId 当前用户ID
     * @return true=有权限, false=无权限
     */
    boolean canDownload(File file, Long userId);

    /**
     * 校验用户是否有权预览文件
     * @param file 文件实体
     * @param userId 当前用户ID
     * @return true=有权限, false=无权限
     */
    boolean canPreview(File file, Long userId);
}