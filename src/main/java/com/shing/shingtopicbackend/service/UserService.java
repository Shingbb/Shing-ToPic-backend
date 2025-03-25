package com.shing.shingtopicbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.shing.shingtopicbackend.model.dto.User.UserQueryRequest;
import com.shing.shingtopicbackend.model.entity.User;
import com.shing.shingtopicbackend.model.vo.LoginUserVO;
import com.shing.shingtopicbackend.model.vo.UserVO;

import java.util.List;

/**
 * @author Shing
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-02-14 17:18:15
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword);

    /**
     * 获取加密后的密码
     *
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取当前登录用户
     */
    User getLoginUser();


    /**
     * 获得脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获得脱敏后的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     *
     * @param userList
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 用户注销
     *
     * @return
     */
    boolean userLogout();

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @return boolean
     */
    boolean isAdmin();

    /**
     * 是否为管理员
     */
    boolean isAdmin(User user);



}
