# 数据库初始化

-- 创建库
create database if not exists shing_pic;

-- 切换库
use shing_pic;

-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
    ) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id            BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    url           VARCHAR(512) NOT NULL COMMENT '图片 URL',
    name          VARCHAR(128) NOT NULL COMMENT '图片名称',
    introduction  VARCHAR(512) NULL COMMENT '简介',
    category      VARCHAR(64) NULL COMMENT '分类',
    tags          VARCHAR(512) NULL COMMENT '标签（JSON 数组）',
    picSize       BIGINT NULL COMMENT '图片体积',
    picWidth      INT NULL COMMENT '图片宽度',
    picHeight     INT NULL COMMENT '图片高度',
    picScale      DOUBLE NULL COMMENT '图片宽高比例',
    picFormat     VARCHAR(32) NULL COMMENT '图片格式',
    userId        BIGINT NOT NULL COMMENT '创建用户 ID',
    createTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    reviewStatus  INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    reviewerId    BIGINT NULL COMMENT '审核人 ID',
    reviewTime    DATETIME NULL COMMENT '审核时间',
    thumbnailUrl  VARCHAR(512) NULL COMMENT '缩略图 URL',
    spaceId       BIGINT NULL COMMENT '空间 ID（为空表示公共空间）',
    picColor      VARCHAR(16) NULL COMMENT '图片主色调',
    INDEX idx_name (name),                 -- 提升基于图片名称的查询性能
    INDEX idx_introduction (introduction), -- 用于模糊搜索图片简介
    INDEX idx_category (category),         -- 提升基于分类的查询性能
    INDEX idx_tags (tags),                 -- 提升基于标签的查询性能
    INDEX idx_userId (userId),             -- 提升基于用户 ID 的查询性能
    INDEX idx_reviewStatus (reviewStatus),  -- 提升基于审核状态的查询性能
    INDEX idx_spaceId (spaceId)            -- 提升基于空间 ID 的查询性能
) comment '图片' collate = utf8mb4_unicode_ci;

-- 空间表
CREATE TABLE IF NOT EXISTS space (
     id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
     spaceName    VARCHAR(128) NULL COMMENT '空间名称',
     spaceLevel   INT DEFAULT 0 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
     maxSize      BIGINT DEFAULT 0 NULL COMMENT '空间图片的最大总大小',
     maxCount     BIGINT DEFAULT 0 NULL COMMENT '空间图片的最大数量',
     totalSize    BIGINT DEFAULT 0 NULL COMMENT '当前空间下图片的总大小',
     totalCount   BIGINT DEFAULT 0 NULL COMMENT '当前空间下的图片数量',
     userId       BIGINT NOT NULL COMMENT '创建用户 ID',
     createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
     editTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
     updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     isDelete     TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
     spaceType    INT DEFAULT 0 NOT NULL COMMENT '空间类型：0-私有 1-团队',
     INDEX idx_userId (userId),                    -- 提升基于用户 ID 的查询性能
     INDEX idx_spaceName (spaceName),              -- 提升基于空间名称的查询性能
     INDEX idx_spaceLevel (spaceLevel),            -- 提升基于空间级别查询性能
     INDEX idx_spaceType (spaceType)               -- 提升基于空间类型查询性能
) COMMENT '空间' COLLATE = utf8mb4_unicode_ci;

-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 图片点赞表
create table if not exists picture_like
(
    id         bigint auto_increment comment 'id' primary key,
    userId     bigint                             not null comment '用户 id',
    pictureId  bigint                             not null comment '图片 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    index idx_userId (userId),
    index idx_pictureId (pictureId)

)comment '图片点赞' collate = utf8mb4_unicode_ci;

-- 图片收藏表
CREATE TABLE IF NOT EXISTS picture_favorite (
    id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    pictureId    BIGINT NOT NULL COMMENT '图片 ID',
    userId       BIGINT NOT NULL COMMENT '用户 ID',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '收藏时间',
    UNIQUE KEY uk_pictureId_userId (pictureId, userId), -- 唯一索引，防止重复收藏
    INDEX idx_pictureId (pictureId),       -- 提升基于图片 ID 的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) COMMENT '图片收藏' COLLATE = utf8mb4_unicode_ci;

-- 图片评论表
CREATE TABLE IF NOT EXISTS picture_comment (
   id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
   pictureId    BIGINT NOT NULL COMMENT '图片 ID',
   userId       BIGINT NOT NULL COMMENT '用户 ID',
   commentText  TEXT NOT NULL COMMENT '评论内容',
   createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '评论时间',
   updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
   isDelete     TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
   INDEX idx_pictureId (pictureId),       -- 提升基于图片的查询性能
   INDEX idx_userId (userId)              -- 提升基于用户的查询性能
) COMMENT '图片评论' COLLATE = utf8mb4_unicode_ci;

-- 空间共享表
CREATE TABLE IF NOT EXISTS space_share (
    id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    spaceId      BIGINT NOT NULL COMMENT '空间 ID',
    sharedUserId BIGINT NOT NULL COMMENT '共享的用户 ID',
    shareTime    DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '共享时间',
    isDelete     TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    INDEX idx_spaceId (spaceId),           -- 提升基于空间 ID 的查询性能
    INDEX idx_sharedUserId (sharedUserId)  -- 提升基于共享用户 ID 的查询性能
) COMMENT '空间共享记录' COLLATE = utf8mb4_unicode_ci;

-- 图片下载记录表
CREATE TABLE IF NOT EXISTS picture_download (
    id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    pictureId    BIGINT NOT NULL COMMENT '图片 ID',
    userId       BIGINT NOT NULL COMMENT '用户 ID',
    downloadTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '下载时间',
    downloadIp   VARCHAR(45) NULL COMMENT '下载 IP 地址',
    INDEX idx_pictureId (pictureId),       -- 提升基于图片 ID 的查询性能
    INDEX idx_userId (userId)              -- 提升基于用户 ID 的查询性能
) COMMENT '图片下载记录' COLLATE = utf8mb4_unicode_ci;

-- 图片标签关联表
CREATE TABLE IF NOT EXISTS picture_tag_relation (
    id         BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    pictureId  BIGINT NOT NULL COMMENT '图片 ID',
    tagId      BIGINT NOT NULL COMMENT '标签 ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    INDEX idx_pictureId (pictureId),       -- 提升基于图片 ID 的查询性能
    INDEX idx_tagId (tagId)                -- 提升基于标签 ID 的查询性能
) COMMENT '图片标签关联' COLLATE = utf8mb4_unicode_ci;

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
   id         BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
   name       VARCHAR(128) NOT NULL COMMENT '标签名称',
   createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
   UNIQUE KEY uk_name (name)              -- 防止重复标签
) COMMENT '标签' COLLATE = utf8mb4_unicode_ci;

-- 图片历史版本表
CREATE TABLE IF NOT EXISTS picture_version (
   id           BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
   pictureId    BIGINT NOT NULL COMMENT '图片 ID',
   version      INT NOT NULL COMMENT '版本号',
   fileUrl      VARCHAR(512) NOT NULL COMMENT '图片文件 URL',
   updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '更新时间',
   createdBy    BIGINT NOT NULL COMMENT '更新用户 ID',
   INDEX idx_pictureId (pictureId),       -- 提升基于图片 ID 的查询性能
   INDEX idx_version (version)            -- 提升基于版本号的查询性能
) COMMENT '图片历史版本' COLLATE = utf8mb4_unicode_ci;

-- 图片评论点赞表
CREATE TABLE IF NOT EXISTS picture_comment_like (
    id         BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userId     BIGINT NOT NULL COMMENT '用户 ID',
    pictureId  BIGINT NOT NULL COMMENT '图片 ID',
    commentId  BIGINT NOT NULL COMMENT '评论 ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    -- 索引优化
    INDEX idx_picture_comment (pictureId, commentId), -- 联合索引，提高评论点赞查询性能
    INDEX idx_userId (userId),                         -- 提升用户查询性能
    INDEX idx_pictureId (pictureId),                   -- 提升图片查询性能
    INDEX idx_commentId (commentId)                    -- 提升评论查询性能
) COMMENT '图片评论点赞' COLLATE = utf8mb4_unicode_ci;


-- 图片评论回复表
CREATE TABLE IF NOT EXISTS picture_comment_reply (
    id         BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userId     BIGINT NOT NULL COMMENT '用户 ID',
    pictureId  BIGINT NOT NULL COMMENT '图片 ID',
    commentId  BIGINT NOT NULL COMMENT '评论 ID',
    reply      VARCHAR(512) NOT NULL COMMENT '回复内容',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    -- 索引优化
    INDEX idx_picture_comment (pictureId, commentId), -- 联合索引，提高图片评论回复查询性能
    INDEX idx_userId (userId),                         -- 提升用户查询性能
    INDEX idx_pictureId (pictureId),                   -- 提升图片查询性能
    INDEX idx_commentId (commentId)                    -- 提升评论查询性能
) COMMENT '图片评论回复' COLLATE = utf8mb4_unicode_ci;


-- 图片评论回复点赞表
CREATE TABLE IF NOT EXISTS picture_comment_reply_like (
    id         BIGINT AUTO_INCREMENT COMMENT 'ID' PRIMARY KEY,
    userId     BIGINT NOT NULL COMMENT '用户 ID',
    pictureId  BIGINT NOT NULL COMMENT '图片 ID',
    commentId  BIGINT NOT NULL COMMENT '评论 ID',
    replyId    BIGINT NOT NULL COMMENT '回复 ID',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   TINYINT DEFAULT 0 NOT NULL COMMENT '是否删除',
    -- 索引优化
    INDEX idx_picture_comment_reply (pictureId, commentId, replyId), -- 联合索引，提高查询性能
    INDEX idx_userId (userId),                                       -- 提升用户查询性能
    INDEX idx_pictureId (pictureId),                                 -- 提升图片查询性能
    INDEX idx_commentId (commentId),                                 -- 提升评论查询性能
    INDEX idx_replyId (replyId)                                      -- 提升回复查询性能
) COMMENT '图片评论回复点赞' COLLATE = utf8mb4_unicode_ci;