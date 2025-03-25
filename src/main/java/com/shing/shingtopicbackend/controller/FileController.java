package com.shing.shingtopicbackend.controller;


import cn.dev33.satoken.annotation.SaCheckRole;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.shing.shingtopicbackend.annotation.AuthCheck;
import com.shing.shingtopicbackend.common.BaseResponse;
import com.shing.shingtopicbackend.common.ResultUtils;
import com.shing.shingtopicbackend.constant.UserConstant;
import com.shing.shingtopicbackend.exception.BusinessException;
import com.shing.shingtopicbackend.exception.ErrorCode;
import com.shing.shingtopicbackend.manager.CosManager;
import com.shing.shingtopicbackend.model.dto.picture.PictureUploadRequest;
import com.shing.shingtopicbackend.model.vo.PictureVO;
import com.shing.shingtopicbackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/file")
public class FileController {

    @Resource
    private CosManager cosManager;

    @Resource
    private PictureService pictureService;

    /**
     * 测试文件上传
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath, null);
            multipartFile.transferTo(file);
            cosManager.putObject(filepath, file);
            // 返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = {}", filepath);
                }
            }
        }
    }


    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     */
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) {
        try (COSObjectInputStream cosObjectInput = cosManager.getObject(filepath).getObjectContent();
             ServletOutputStream outputStream = response.getOutputStream()) {

            // 获取文件 MIME 类型
            String contentType = Files.probeContentType(Path.of(filepath));
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            response.setContentType(contentType);

            // 处理文件名编码，兼容不同浏览器
            String encodedFilename = URLEncoder.encode(filepath, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20"); // 处理 "+" 变空格问题

            // 设置响应头
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
            response.setHeader(HttpHeaders.PRAGMA, "no-cache");
            response.setHeader(HttpHeaders.EXPIRES, "0");

            // 直接流拷贝，减少内存占用
            IOUtils.copy(cosObjectInput, outputStream);
            outputStream.flush();

        } catch (Exception e) {
            log.error("【文件下载失败】filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    //@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest
    ) {
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest);
        return ResultUtils.success(pictureVO);
    }


}
