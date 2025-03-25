package com.shing.shingtopicbackend.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.shing.shingtopicbackend.constant.SystemConstants;
import com.shing.shingtopicbackend.exception.BusinessException;
import com.shing.shingtopicbackend.exception.ErrorCode;
import com.shing.shingtopicbackend.model.dto.User.UserQueryRequest;
import com.shing.shingtopicbackend.model.enums.UserRoleEnum;
import com.shing.shingtopicbackend.model.vo.LoginUserVO;
import com.shing.shingtopicbackend.model.vo.TokenLoginUserVo;
import com.shing.shingtopicbackend.model.vo.UserVO;
import com.shing.shingtopicbackend.service.UserService;
import com.shing.shingtopicbackend.mapper.UserMapper;
import com.shing.shingtopicbackend.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

import static com.shing.shingtopicbackend.constant.SystemConstants.SALT;

/**
 * @author Shing
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-02-14 17:18:15
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**
     * 用户注册函数
     *
     * @param userAccount 用户账号，要求长度至少为4
     * @param userPassword 用户密码，要求长度至少为8
     * @param checkPassword 用于确认的密码，必须与userPassword相同
     * @return 注册成功后用户的ID
     * @throws BusinessException 当参数不合法或注册过程中发生错误时抛出
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数是否为空或长度是否符合要求
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        // 检查密码和校验密码是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 使用同步块确保并发情况下用户账号的唯一性
        synchronized (userAccount.intern()) {
            // 检查账户是否重复
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("userAccount", userAccount);
            long count = this.baseMapper.selectCount(queryWrapper);
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
            }
            // 2. 加密密码
            String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
            // 3. 插入数据到数据库
            User user = new User();
            user.setUserAccount(userAccount);
            user.setUserPassword(encryptPassword);
            boolean saveResult = this.save(user);
            if (!saveResult) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
            }
            return user.getId();
        }
    }


    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword) {
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        StpUtil.login(user.getId());
        StpUtil.getTokenSession().set(SystemConstants.USER_LOGIN_STATE, user);
        return this.getTokenLoginUserVO(user);
    }

    /**
     * 根据用户对象获取登录用户的 Token 信息
     *
     * @param user 用户对象，包含用户的基本信息
     * @return 返回一个包含用户信息和 Token 信息的 TokenLoginUserVo 对象，
     *         如果传入的用户对象为 null，则返回 null
     */
    public TokenLoginUserVo getTokenLoginUserVO(User user) {
        // 检查传入的用户对象是否为 null
        if (user == null) {
            return null;
        }
        // 创建一个 TokenLoginUserVo 对象，用于存储登录用户的详细信息
        TokenLoginUserVo loginUserVO = new TokenLoginUserVo();
        // 将用户对象的属性复制到 TokenLoginUserVo 对象中
        BeanUtils.copyProperties(user, loginUserVO);

        // 获取 Token 相关参数
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        // 将 Token 信息设置到 loginUserVO 对象中
        loginUserVO.setSaTokenInfo(tokenInfo);

        // 返回包含用户信息和 Token 信息的 loginUserVO 对象
        return loginUserVO;
    }

    /**
     * 获取加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取登录用户
     * 获取当前登录用户
     *
     * @return {@link User}
     */
    @Override
    public User getLoginUser() {
        if (!StpUtil.isLogin()) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 先判断是否已登录
        Object userObj = StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        return null;
    }

    @Override
    public UserVO getUserVO(User user) {
        return null;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        return List.of();
    }

    @Override
    public boolean userLogout() {
        return false;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        return null;
    }


    /**
     * 判断当前登录用户是否为管理员
     *
     * @return boolean
     */
    @Override
    public boolean isAdmin() {
        // 从 Token 会话中获取用户登录状态
        Object userObj = StpUtil.getTokenSession().get(SystemConstants.USER_LOGIN_STATE);

        // 检查用户对象是否为 null
        if (userObj == null) {
            // 可以记录日志，提示会话中未找到用户
            log.warn("无法获取当前用户信息，可能未登录或会话已过期");
            return false; // 或者抛出异常，视业务需求而定
        }

        // 强制转换为 User 对象
        User user = (User) userObj;

        // 返回是否为管理员
        return isAdmin(user);
    }

    /**
     * 判断指定用户是否为管理员
     *
     * @param user 用户对象
     * @return boolean
     */
    @Override
    public boolean isAdmin(User user) {
        // 检查用户对象是否为 null
        if (user == null) {
            return false; // 或者抛出异常，视业务需求而定
        }

        // 判断用户角色是否为管理员
        return UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }


}




