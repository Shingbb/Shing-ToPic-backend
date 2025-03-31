package com.shing.shingtopicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shing.shingtopicbackend.model.dto.picture.PictureQueryRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureReviewRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureUploadByBatchRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureUploadRequest;
import com.shing.shingtopicbackend.model.entity.Picture;
import com.shing.shingtopicbackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Shing
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-14 17:18:15
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest);


    /**
     * 获取图片包装类（单条）
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片包装类（分页）
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 通过缓存获取图片包装类（分页）
     */
    Page<PictureVO> getPictureVOPageWithCache(PictureQueryRequest request);
    /**
     * 获取查询对象
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 图片审核
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest);

    /**
     * 填充审核参数
     */
    void fillReviewParams(Picture picture);

    /**
     * 批量抓取和创建图片
     *
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest);


}
