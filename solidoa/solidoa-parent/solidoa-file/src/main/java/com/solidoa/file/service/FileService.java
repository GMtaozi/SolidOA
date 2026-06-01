package com.solidoa.file.service;

import com.solidoa.file.vo.FileVO;
import com.solidoa.common.vo.PageVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    FileVO upload(MultipartFile file, String businessType, Long businessId, Long userId);

    PageVO<FileVO> list(String businessType, Long businessId, Integer pageNum, Integer pageSize);

    void preview(Long id, Long userId, HttpServletResponse response);

    void download(Long id, Long userId, HttpServletResponse response);

    void delete(Long id, Long userId);

    FileVO getById(Long id, Long userId);
}