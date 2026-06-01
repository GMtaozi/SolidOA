package com.solidoa.file.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.client.SystemClient;
import com.solidoa.file.entity.File;
import com.solidoa.file.mapper.FileMapper;
import com.solidoa.file.service.FileService;
import com.solidoa.file.service.FileAccessService;
import com.solidoa.file.vo.FileVO;
import com.solidoa.common.vo.PageVO;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import jakarta.servlet.http.HttpServletResponse;
import com.solidoa.file.util.MagicNumberUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
        "jpg", "jpeg", "png", "gif", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "zip", "rar", "7z",
        "txt", "csv"
    );

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileAccessService fileAccessService;

    @Autowired
    private SystemClient systemClient;

    @Value("${minio.bucket.common:solidoa-files}")
    private String defaultBucket;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String minioEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Override
    @Transactional
    public FileVO upload(MultipartFile file, String businessType, Long businessId, Long userId) {
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_TYPES.contains(extension)) {
            throw new BusinessException("不支持的文件类型: " + extension);
        }

        // 文件大小限制：严格小于10MB
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过10MB");
        }

        // 增强文件安全检测：读取更多内容用于恶意脚本检测
        // 文本文件读取 256KB，二进制文件读取 32KB
        byte[] header;
        byte[] scriptCheckBytes = null;
        boolean isTextType = isTextExtension(extension);
        boolean isArchiveType = isArchiveExtension(extension);

        try {
            if (isTextType) {
                // 文本文件：读取 256KB 用于完整脚本检测
                scriptCheckBytes = file.getInputStream().readNBytes(256 * 1024);
                header = scriptCheckBytes;
            } else if (isArchiveType) {
                // 压缩包：读取 32KB 用于检测
                scriptCheckBytes = file.getInputStream().readNBytes(32 * 1024);
                header = scriptCheckBytes;
            } else {
                // 二进制文件：读取 32KB
                header = file.getInputStream().readNBytes(32 * 1024);
            }
        } catch (java.io.IOException e) {
            throw new BusinessException("文件读取失败");
        }

        // 验证 magic number
        boolean magicValid;
        try {
            magicValid = MagicNumberUtil.validateMagicNumber(
                new ByteArrayInputStream(header), extension);
        } catch (java.io.IOException e) {
            throw new BusinessException("文件类型验证失败");
        }
        if (!magicValid) {
            throw new BusinessException("文件类型验证失败，请上传真实的 " + extension + " 文件");
        }

        // 检测恶意脚本内容（使用增强的检测逻辑）
        if (scriptCheckBytes != null && (isTextType || isArchiveType)) {
            boolean hasScript;
            try {
                hasScript = MagicNumberUtil.containsScriptContent(
                    new ByteArrayInputStream(scriptCheckBytes), extension);
            } catch (java.io.IOException e) {
                hasScript = false;
            }
            if (hasScript) {
                throw new BusinessException("文件包含非法内容，禁止上传");
            }
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String fileName = uuid + "." + extension;
        String bucket = determineBucket(businessType);

        // 先创建数据库记录（status=pending）
        File fileEntity = new File();
        fileEntity.setFileName(fileName);
        fileEntity.setOriginalName(originalFilename);
        fileEntity.setFilePath(bucket + "/" + fileName);
        fileEntity.setFileSize(file.getSize());
        fileEntity.setFileType(extension);
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setBucket(bucket);
        fileEntity.setBusinessType(businessType);
        fileEntity.setBusinessId(businessId);
        fileEntity.setUploaderId(userId);
        fileEntity.setCreateTime(LocalDateTime.now());
        fileMapper.insert(fileEntity);

        // 再上传到 MinIO
        try (InputStream uploadStream = file.getInputStream()) {
            minioClient.putObject(
                io.minio.PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(uploadStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );
        } catch (Exception e) {
            log.error("文件上传失败，删除数据库记录", e);
            fileMapper.deleteById(fileEntity.getId());
            throw new BusinessException("文件上传失败");
        }

        log.info("文件上传成功: id={}, originalName={}", fileEntity.getId(), originalFilename);
        return convertToVO(fileEntity);
    }

    @Override
    public PageVO<FileVO> list(String businessType, Long businessId, Integer pageNum, Integer pageSize) {
        Page<File> page = new Page<>(pageNum, pageSize);
        Page<File> result = fileMapper.selectByBusinessPage(page, businessType, businessId);

        List<FileVO> records = result.getRecords().stream().map(this::convertToVO).toList();

        PageVO<FileVO> pageVO = new PageVO<>();
        pageVO.setRecords(records);
        pageVO.setTotal(result.getTotal());
        pageVO.setPageNum(pageNum);
        pageVO.setPageSize(pageSize);
        return pageVO;
    }

    @Override
    public void preview(Long id, Long userId, HttpServletResponse response) {
        File file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 权限校验
        if (!fileAccessService.canPreview(file, userId)) {
            log.warn("用户 {} 尝试非法预览文件 {}", userId, id);
            throw new BusinessException("无权限预览此文件");
        }

        try {
            GetObjectResponse stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                    .bucket(file.getBucket())
                    .object(file.getFileName())
                    .build()
            );

            response.setContentType(file.getMimeType());
            String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);

            try (BufferedInputStream input = new BufferedInputStream(stream);
                 BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream())) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.flush();
            }
        } catch (Exception e) {
            log.error("文件预览失败", e);
            throw new BusinessException("文件预览失败");
        }
    }

    @Override
    public void download(Long id, Long userId, HttpServletResponse response) {
        File file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 权限校验
        if (!fileAccessService.canDownload(file, userId)) {
            log.warn("用户 {} 尝试非法下载文件 {}", userId, id);
            throw new BusinessException("无权限下载此文件");
        }

        try {
            GetObjectResponse stream = minioClient.getObject(
                io.minio.GetObjectArgs.builder()
                    .bucket(file.getBucket())
                    .object(file.getFileName())
                    .build()
            );

            response.setContentType("application/octet-stream");
            String encodedName = URLEncoder.encode(file.getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(file.getFileSize());

            try (BufferedInputStream input = new BufferedInputStream(stream);
                 BufferedOutputStream output = new BufferedOutputStream(response.getOutputStream())) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }

                output.flush();
            }
        } catch (Exception e) {
            log.error("文件下载失败", e);
            throw new BusinessException("文件下载失败");
        }
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId) {
        File file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 统一权限校验：fileAccessService.canAccess() 或管理员权限
        if (!fileAccessService.canAccess(file, userId)) {
            if (!checkIsAdmin(userId)) {
                log.warn("用户 {} 尝试非法删除文件 {}", userId, id);
                throw new BusinessException("无权限删除此文件");
            }
        }

        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(file.getBucket())
                    .object(file.getFileName())
                    .build()
            );
        } catch (Exception e) {
            log.error("删除MinIO文件失败", e);
            throw new BusinessException("删除文件失败，MinIO删除异常");
        }

        fileMapper.deleteById(id);
        log.info("删除文件: id={}", id);
    }

    /**
     * 检查用户是否为系统管理员
     */
    private boolean checkIsAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        try {
            var result = systemClient.getUserPermissions(userId);
            if (result != null && result.getCode() == 200 && result.getData() != null) {
                return result.getData().contains("SYSTEM_ADMIN");
            }
        } catch (Exception e) {
            log.warn("检查用户权限失败: userId={}, error={}", userId, e.getMessage());
        }
        return false;
    }

    @Override
    public FileVO getById(Long id, Long userId) {
        File file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException("文件不存在");
        }

        // 权限校验
        if (!fileAccessService.canAccess(file, userId)) {
            log.warn("用户 {} 尝试非法获取文件信息 {}", userId, id);
            throw new BusinessException("无权限访问此文件");
        }

        return convertToVO(file);
    }

    private static final Set<String> ALLOWED_BUCKET_TYPES = Set.of(
        "LEAVE", "EXPENSE", "ATTENDANCE"
    );

    private String determineBucket(String businessType) {
        if (businessType == null) {
            throw new BusinessException("businessType不能为空");
        }
        if (!ALLOWED_BUCKET_TYPES.contains(businessType)) {
            throw new BusinessException("不支持的业务类型: " + businessType);
        }
        return switch (businessType) {
            case "LEAVE" -> "leave-attach";
            case "EXPENSE" -> "expense-invoice";
            case "ATTENDANCE" -> "attendance-files";
            default -> throw new BusinessException("不支持的业务类型: " + businessType);
        };
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    private FileVO convertToVO(File file) {
        if (file == null) return null;
        FileVO vo = new FileVO();
        BeanUtils.copyProperties(file, vo);
        if (file.getCreateTime() != null) {
            vo.setCreateTime(file.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        return vo;
    }

    // 文本类型扩展名（需要256KB读取用于脚本检测）
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
        "txt", "csv", "html", "htm", "xml", "json", "js", "css", "svg", "md",
        "yml", "yaml", "sql", "sh", "bat", "ini", "conf", "properties"
    );

    // 压缩包类型扩展名（需要额外内容检测）
    private static final Set<String> ARCHIVE_EXTENSIONS = Set.of(
        "zip", "rar", "7z", "tar", "gz", "bz2"
    );

    private static boolean isTextExtension(String extension) {
        return extension != null && TEXT_EXTENSIONS.contains(extension.toLowerCase());
    }

    private static boolean isArchiveExtension(String extension) {
        return extension != null && ARCHIVE_EXTENSIONS.contains(extension.toLowerCase());
    }
}