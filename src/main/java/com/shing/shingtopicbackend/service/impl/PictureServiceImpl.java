package com.shing.shingtopicbackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.shing.shingtopicbackend.exception.BusinessException;
import com.shing.shingtopicbackend.exception.ErrorCode;
import com.shing.shingtopicbackend.exception.ThrowUtils;
import com.shing.shingtopicbackend.manager.upload.FilePictureUpload;
import com.shing.shingtopicbackend.manager.upload.PictureUploadTemplate;
import com.shing.shingtopicbackend.manager.upload.UrlPictureUpload;
import com.shing.shingtopicbackend.mapper.PictureMapper;
import com.shing.shingtopicbackend.model.dto.file.UploadPictureResult;
import com.shing.shingtopicbackend.model.dto.picture.PictureQueryRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureReviewRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureUploadByBatchRequest;
import com.shing.shingtopicbackend.model.dto.picture.PictureUploadRequest;
import com.shing.shingtopicbackend.model.entity.Picture;
import com.shing.shingtopicbackend.model.entity.User;
import com.shing.shingtopicbackend.model.enums.PictureReviewStatusEnum;
import com.shing.shingtopicbackend.model.vo.PictureVO;
import com.shing.shingtopicbackend.model.vo.UserVO;
import com.shing.shingtopicbackend.service.PictureService;
import com.shing.shingtopicbackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Shing
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-02-14 17:18:15
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 本地缓存
     */
    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();


    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }

    }


    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest) {
        // 获取当前登录用户
        User loginUser = userService.getLoginUser();
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 处理更新逻辑：如果 pictureId 存在，校验权限并获取旧的图片信息
        Picture picture = new Picture();
        if (pictureUploadRequest != null && pictureUploadRequest.getId() != null) {
            long pictureId = pictureUploadRequest.getId();
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可编辑图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin()) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }

            // 更新操作：填充编辑时间并设置其他信息
            picture = oldPicture;
            picture.setEditTime(new Date());
        }

        // 确定上传策略（本地文件或 URL）
        PictureUploadTemplate pictureUploadTemplate = (inputSource instanceof String) ? urlPictureUpload : filePictureUpload;
        // 按照用户 id 划分目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        // 根据 inputSource 类型区分上传方式
        UploadPictureResult uploadResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
        // 填充图片信息
        picture.setUrl(uploadResult.getUrl());
        // 支持外层上传图片名称
        picture.setName((pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName()))
                ? pictureUploadRequest.getPicName()
                : uploadResult.getPicName());
        picture.setPicSize(uploadResult.getPicSize());
        picture.setPicWidth(uploadResult.getPicWidth());
        picture.setPicHeight(uploadResult.getPicHeight());
        picture.setPicScale(uploadResult.getPicScale());
        picture.setPicFormat(uploadResult.getPicFormat());
        picture.setUserId(loginUser.getId());

        // 补充审核参数
        this.fillReviewParams(picture);

        // 保存或更新数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败，数据库操作失败");

        return PictureVO.objToVo(picture);
    }

    @Override
    public PictureVO getPictureVO(Picture picture) {
        // 对象转封装类  
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息  
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public Page<PictureVO> getPictureVOPageWithCache(PictureQueryRequest pictureQueryRequest) {
        // 1. 参数校验与默认值设置
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

        // 2. 构建缓存Key
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = String.format("shingpicture:listPictureVOByPage:%s", hashKey);

        // 3. 先从本地缓存中查询
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            // 如果缓存命中，返回结果
            // 使用TypeReference保留泛型信息，避免转换错误
            return JSONUtil.toBean(cachedValue, new TypeReference<Page<PictureVO>>() {
            }, false);
        }
        // 4. 本地缓存未命中，查询 Redis 分布式缓存
        try {
            ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
            cachedValue = valueOps.get(cacheKey);
            if (cachedValue != null) {
                // 如果缓存命中，更新本地缓存，返回结果
                LOCAL_CACHE.put(cacheKey, cachedValue);
                return JSONUtil.toBean(cachedValue, new TypeReference<Page<PictureVO>>() {
                }, false);
            }
        } catch (Exception e) {
            // Redis故障时继续执行，不阻断主流程
            log.error("Redis 缓存查询失败", e);
        }

        // 5. 缓存未命中时查询数据库
        Page<Picture> picturePage = this.page(
                new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                getQueryWrapper(pictureQueryRequest)
        );
        if (CollUtil.isNotEmpty(picturePage.getRecords())) {
            // 缓存空值（短时间）
            String emptyJson = JSONUtil.toJsonStr(new Page<PictureVO>());
            try {
                stringRedisTemplate.opsForValue().set(
                        cacheKey, emptyJson, 60L + RandomUtil.randomInt(0, 60), TimeUnit.SECONDS
                );
            } catch (Exception e) {
                log.warn("空值缓存失败", e);
            }
            return new Page<>();
        }

        // 6. 复用已有的 VO 转换方法 getPictureVOPage 获取封装类
        Page<PictureVO> pictureVOPage = getPictureVOPage(picturePage);

        // 7. 更新缓存
        // 异步更新缓存
        CompletableFuture.runAsync(() -> {
            String cacheValue = JSONUtil.toJsonStr(pictureVOPage);
            try {
                // 更新 Redis 缓存
                // 5 - 10 分钟随机过期，防止雪崩
                int cacheExpireTime = 300 + RandomUtil.randomInt(0, 300);
                stringRedisTemplate.opsForValue().set(cacheKey, cacheValue, cacheExpireTime, TimeUnit.SECONDS);
                // 写入本地缓存
                LOCAL_CACHE.put(cacheKey, cacheValue);
            } catch (Exception e) {
                log.warn("Redis 缓存更新失败", e);
            }
        }, Executors.newFixedThreadPool(2));
        // 获取封装类
        return pictureVOPage;
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 直接从对象中取值，减少局部变量的定义
        queryWrapper.eq(ObjUtil.isNotEmpty(pictureQueryRequest.getId()), "id", pictureQueryRequest.getId())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getUserId()), "userId", pictureQueryRequest.getUserId())
                .eq(StrUtil.isNotBlank(pictureQueryRequest.getCategory()), "category", pictureQueryRequest.getCategory())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicWidth()), "picWidth", pictureQueryRequest.getPicWidth())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicHeight()), "picHeight", pictureQueryRequest.getPicHeight())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicSize()), "picSize", pictureQueryRequest.getPicSize())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getPicScale()), "picScale", pictureQueryRequest.getPicScale())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewStatus()), "reviewStatus", pictureQueryRequest.getReviewStatus())
                .eq(ObjUtil.isNotEmpty(pictureQueryRequest.getReviewerId()), "reviewerId", pictureQueryRequest.getReviewerId())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getName()), "name", pictureQueryRequest.getName())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getIntroduction()), "introduction", pictureQueryRequest.getIntroduction())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getPicFormat()), "picFormat", pictureQueryRequest.getPicFormat())
                .like(StrUtil.isNotBlank(pictureQueryRequest.getReviewMessage()), "reviewMessage", pictureQueryRequest.getReviewMessage());

        // 多字段模糊查询
        if (StrUtil.isNotBlank(pictureQueryRequest.getSearchText())) {
            queryWrapper.and(qw -> qw.like("name", pictureQueryRequest.getSearchText())
                    .or()
                    .like("introduction", pictureQueryRequest.getSearchText()));
        }

        // JSON 数组查询（使用 Lambda 表达式优化 for 循环）
        if (CollUtil.isNotEmpty(pictureQueryRequest.getTags())) {
            pictureQueryRequest.getTags().forEach(tag -> queryWrapper.like("tags", "\"" + tag + "\""));
        }

        // 排序优化，避免空值问题
        if (StrUtil.isNotEmpty(pictureQueryRequest.getSortField())) {
            queryWrapper.orderBy(true, "ascend".equalsIgnoreCase(pictureQueryRequest.getSortOrder()), pictureQueryRequest.getSortField());
        }

        return queryWrapper;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复，已是该状态
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 4. 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(userService.getLoginUser().getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

    }

    @Override
    public void fillReviewParams(Picture picture) {
        if (userService.isAdmin()) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(StpUtil.getLoginIdAsLong());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，无论是编辑还是创建默认都是待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }

    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest) {
        // 1. 基础参数校验
        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(StrUtil.isBlank(searchText), ErrorCode.PARAMS_ERROR, "搜索内容不能为空");
        ThrowUtils.throwIf(count == null || count <= 0, ErrorCode.PARAMS_ERROR, "数量必须大于0");
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "单次最多处理30条");

        // 2. 处理图片名称前缀（默认使用搜索关键词）
        String namePrefix = Optional.ofNullable(pictureUploadByBatchRequest.getNamePrefix())
                .filter(StrUtil::isNotBlank)
                .orElse(searchText);

        // 3. 构建安全的搜索URL（自动处理特殊字符）
        String encodedSearch = URLEncoder.encode(searchText, StandardCharsets.UTF_8);
        // 抓取内容
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", encodedSearch);

        // 4. 使用JSoup获取页面内容（添加超时和重试机制）
        Document document;
        try {
            document = Jsoup.connect(fetchUrl)
                    .timeout(10000)
                    .maxBodySize(0)
                    .get();
        } catch (IOException e) {
            log.error("获取Bing图片页面失败 | url={} | error={}", fetchUrl, e.getMessage());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片搜索服务暂时不可用");
        }

        // 5. 定位图片容器元素
        Element div = document.getElementsByClass("dgControl").first();
        if (div == null) {
            log.warn("未找到图片容器元素 | html={}", document.html());
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片源站结构已变更");
        }

        // 6. 获取图片元素列表
        Elements imgElementList = div.select("img.mimg");
        if (imgElementList.isEmpty()) {
            log.warn("未找到有效图片元素 | div={}", div.html());
            return 0;  // 没有图片时返回0而不是抛出异常
        }
        //  7. 遍历图片元素，依次处理上传图片
        int uploadCount = 0;
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过：{}", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突的问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 9. 构建上传请求（自动编号）
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            try {
                // 10. 执行单图片上传
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest);
                log.info("图片上传成功，id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        log.info("批量上传完成 | 成功数={} | 请求数={}", uploadCount, count);
        return uploadCount;
    }
}



