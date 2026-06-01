package com.solidoa.file.controller;

import com.solidoa.common.result.Result;
import com.solidoa.file.service.FileService;
import com.solidoa.file.vo.FileVO;
import com.solidoa.common.vo.PageVO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/file")
@Slf4j
public class FileController {

    @Autowired
    private FileService fileService;

    @PostMapping("/upload")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                  @RequestParam(required = false) String businessType,
                                  @RequestParam(required = false) Long businessId,
                                  @RequestHeader("X-User-Id") Long userId) {
        return Result.success(fileService.upload(file, businessType, businessId, userId));
    }

    @GetMapping("/list")
    public Result<PageVO<FileVO>> list(@RequestParam(required = false) String businessType,
                                        @RequestParam(required = false) Long businessId,
                                        @RequestParam(defaultValue = "1") Integer pageNum,
                                        @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.success(fileService.list(businessType, businessId, pageNum, pageSize));
    }

    @GetMapping("/{id}/preview")
    public void preview(@PathVariable Long id,
                       @RequestHeader("X-User-Id") Long userId,
                       HttpServletResponse response) {
        fileService.preview(id, userId, response);
    }

    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id,
                         @RequestHeader("X-User-Id") Long userId,
                         HttpServletResponse response) {
        fileService.download(id, userId, response);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        fileService.delete(id, userId);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<FileVO> getById(@PathVariable Long id,
                                @RequestHeader("X-User-Id") Long userId) {
        return Result.success(fileService.getById(id, userId));
    }
}