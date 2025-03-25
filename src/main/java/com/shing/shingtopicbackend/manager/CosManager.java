package com.shing.shingtopicbackend.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;

import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.shing.shingtopicbackend.config.CosClientConfig;
import com.shing.shingtopicbackend.exception.BusinessException;
import com.shing.shingtopicbackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;

/**
 * 腾讯云Cos 对象存储操作
 *
 * @author shing
 */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传对象（支持本地文件路径或 File）
     *
     * @param key        唯一键
     * @param fileOrPath 本地文件路径 或 File 对象
     * @return {@link PutObjectResult}
     */
    public PutObjectResult putObject(String key, Object fileOrPath) {
        try {
            if (fileOrPath == null) {
                throw new IllegalArgumentException("文件路径或对象不能为空");
            }

            File file = (fileOrPath instanceof String path) ? new File(path) : (File) fileOrPath;

            // 校验文件是否存在且可读
            if (!file.exists() || !file.isFile() || !file.canRead()) {
                throw new IllegalArgumentException("文件不存在或不可读取: " + file.getAbsolutePath());
            }

            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

            // 设置 HTTP Headers
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentDisposition("inline");
            putObjectRequest.setMetadata(metadata);

            PutObjectResult result = cosClient.putObject(putObjectRequest);

            log.info("【文件上传成功】key: {}, 大小: {} KB", key, file.length() / 1024);
            return result;
        } catch (Exception e) {
            log.error("【文件上传失败】key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }

    /**
     * 上传图片对象（支持图片处理）
     *
     * @param key  唯一键
     * @param file 图片文件
     * @return {@link PutObjectResult}
     */
    public PutObjectResult putPictureObject(String key, File file) {
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
            // 设置图片处理参数
            PicOperations picOperations = new PicOperations();
            picOperations.setIsPicInfo(1); // 1 表示返回原图信息
            putObjectRequest.setPicOperations(picOperations);

            PutObjectResult result = cosClient.putObject(putObjectRequest);
            log.info("【图片上传成功】key: {}", key);
            return result;
        } catch (Exception e) {
            log.error("【图片上传失败】key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     * @return {@link COSObject}
     */
    public COSObject getObject(String key) {
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
            COSObject cosObject = cosClient.getObject(getObjectRequest);
            log.info("【文件下载成功】key: {}", key);
            return cosObject;
        } catch (Exception e) {
            log.error("【文件下载失败】key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        }
    }

    /**
     * 删除对象
     *
     * @param key 唯一键
     */
    public void deleteObject(String key) {
        try {
            cosClient.deleteObject(cosClientConfig.getBucket(), key);
            log.info("【文件删除成功】key: {}", key);
        } catch (Exception e) {
            log.error("【文件删除失败】key: {}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除失败");
        }
    }

}
