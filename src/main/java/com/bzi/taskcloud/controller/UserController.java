package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.bzi.taskcloud.common.dto.UserLoginDTO;
import com.bzi.taskcloud.common.dto.UserRegisterDTO;
import com.bzi.taskcloud.common.dto.UserUpdateInfoDTO;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.UserState;
import com.bzi.taskcloud.common.lang.UserType;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.JwtUtil;
import com.bzi.taskcloud.entity.User;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.IHelpService;
import com.bzi.taskcloud.service.ITaskCommentService;
import com.bzi.taskcloud.service.ITaskService;
import com.bzi.taskcloud.service.IUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@RestController
@DecryptRequest
@EncryptResponse
@RequestMapping("/user")
@Api(value = "用户模块", description = "用户模块")
public class UserController {
    private final IUserService userService;
    private final ITaskService taskService;
    private final IHelpService helpService;
    private final ITaskCommentService taskCommentService;
    private final DataSourceTransactionManager dataSourceTransactionManager;
    private final TransactionDefinition transactionDefinition;

    @Autowired
    public UserController(IUserService userService, ITaskService taskService, IHelpService helpService, ITaskCommentService taskCommentService, DataSourceTransactionManager dataSourceTransactionManager, TransactionDefinition transactionDefinition) {
        this.userService = userService;
        this.taskService = taskService;
        this.helpService = helpService;
        this.taskCommentService = taskCommentService;
        this.dataSourceTransactionManager = dataSourceTransactionManager;
        this.transactionDefinition = transactionDefinition;
    }

    @ApiOperation(value = "用户注册", notes = "用户接口")
    @PostMapping("/register")
    public Result register(@Validated @RequestBody UserRegisterDTO registerInfo) throws NoSuchAlgorithmException {
        // 检查注册的用户类型
        Assert.isTrue(UserType.admin != UserType.values()[registerInfo.getRegisterType()], "不存在的用户类型。");

        // 检查账户名是否已经注册
        User queryUser = userService.getOne(new QueryWrapper<User>()
                .eq("username", registerInfo.getUsername())
                .ne("type", UserType.admin.ordinal())
        );
        Assert.isNull(queryUser, "该账户名已存在，请换一个账户名重试！");

        // 注册账户
        User registerUser = new User();

        if (StringUtils.isBlank(registerInfo.getNickname()))
            registerUser.setNickname(registerInfo.getUsername());
        registerUser.setUsername(registerInfo.getUsername());
        registerUser.setPassword(AccountUtil.makePassword(registerInfo.getPassword()));
        registerUser.setType(registerInfo.getRegisterType());
        registerUser.setState(UserState.offline.ordinal());
        registerUser.setRegistedTime(LocalDateTime.now());

        // 更新数据库
        Assert.isTrue(userService.save(registerUser), "注册失败，请联系管理员解决！");

        return Result.succeed(AccountUtil.copyUserInfo(registerUser));
    }

    @ApiOperation(value = "用户登陆", notes = "用户接口")
    @PutMapping("/login")
    public Result login(@Validated @RequestBody UserLoginDTO userLoginInfo, HttpServletRequest request, HttpServletResponse response) throws NoSuchAlgorithmException {
        User user = userService.getOne(new QueryWrapper<User>().eq("username", userLoginInfo.getUsername()));
        Assert.notNull(user, "用户名或密码错误。"); // 用户不存在

        Assert.isTrue(
                user.getPassword().equals(AccountUtil.makePassword(userLoginInfo.getPassword())),
                "用户名或密码错误。"
        ); // 密码错误

        // 更新用户状态
        user.setState(UserState.online.ordinal()); // 设置当前状态为在线状态
        user.setLastLoginTime(LocalDateTime.now()); // 设置最后登陆时间为当前时间
        user.setLastLoginIp(request.getRemoteHost()); // 设置最后登陆IP为当前IP
        Assert.isTrue(
            userService.updateById(user),
                "登陆失败，请联系管理员处理！"
        );

        // 登陆成功
        response.setHeader("Authorization", JwtUtil.generateToken(user));
        response.setHeader("Access-Control-Expose-Headers", "Authorization");

        return Result.succeed(AccountUtil.copyUserInfo(user));
    }

    @ApiOperation(value = "用户资料更新", notes = "用户接口")
    @PutMapping("/update")
    public Result update(@Validated @RequestBody UserUpdateInfoDTO userUpdateInfoDTO) {
        User user = AccountUtil.getProfile();
        if (user.getNickname().equals(userUpdateInfoDTO.getNickname()))
            return Result.succeed();

        // 更新信息
        user.setNickname(userUpdateInfoDTO.getNickname());

        // 更新数据库
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            // 如果是开发者则根据情况更新任务与帮助
            if (UserType.developer.ordinal() == AccountUtil.getProfile().getType()) {
                Assert.isTrue(
                        taskService.updateAuthor(AccountUtil.getProfile().getId(), user.getNickname()),
                        "用户资料更新失败001！"
                );
                Assert.isTrue(
                        helpService.updateAuthor(AccountUtil.getProfile().getId(), user.getNickname()),
                        "用户资料更新失败002！"
                );
            }
            Assert.isTrue(
                    taskCommentService.updateAuthor(AccountUtil.getProfile().getId(), user.getNickname()),
                    "用户资料更新失败003！"
            );

            // 更新用户信息
            Assert.isTrue(userService.updateById(user), "用户资料更新失败004！");

            dataSourceTransactionManager.commit(transactionStatus);
        } catch (Exception exception) {
            dataSourceTransactionManager.rollback(transactionStatus);
            throw exception;
        }

        return Result.succeed();
    }

    @ApiOperation(value = "用户资料查询", notes = "用户接口")
    @GetMapping("/detail/{userId}")
    public Result detail(@PathVariable Long userId) {
        // 判断是否为查询自己的信息
        if (-1 == userId || Objects.equals(userId, AccountUtil.getProfile().getId()))
            return Result.succeed(AccountUtil.copyUserInfo(AccountUtil.getProfile()));

        User user = userService.getById(userId);
        Assert.notNull(user, "用户不存在。");
        Assert.isTrue(UserState.delete.ordinal() != user.getState(), "用户不存在。");
        Assert.isTrue(UserType.admin.ordinal() != user.getType(), "用户不存在。"); // 不让查管理员用户信息

        return Result.succeed(AccountUtil.copyUserInfo(user));
    }

    @ApiOperation(value = "用户登出", notes = "用户接口")
    @DeleteMapping("/logout")
    public Result logout() {
        AccountUtil.getProfile().setState(UserState.offline.ordinal());

        Assert.isTrue(
                userService.updateById(AccountUtil.getProfile()),
                "登出失败，请联系管理员！"
        );

        return Result.succeed("登出成功。");
    }
}
