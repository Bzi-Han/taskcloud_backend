package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzi.taskcloud.common.dto.HelpPublishDTO;
import com.bzi.taskcloud.common.dto.HelpReviewDTO;
import com.bzi.taskcloud.common.dto.HelpUpdateDTO;
import com.bzi.taskcloud.common.lang.HelpState;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.lang.TaskState;
import com.bzi.taskcloud.common.lang.UserType;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.PageUtil;
import com.bzi.taskcloud.common.vo.HelpDetailInfoVO;
import com.bzi.taskcloud.entity.Help;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.IHelpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.lang.reflect.InvocationTargetException;
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
@RequestMapping("/help")
@Api(value = "帮助模块", description = "帮助模块")
public class HelpController {
    @Autowired
    private IHelpService helpService;

    @ApiOperation(value = "添加一篇帮助文章", notes = "开发者接口")
    @PostMapping("/publish")
    @RolesAllowed("developer")
    public Result publish(@Validated @RequestBody HelpPublishDTO helpPublishDTO) {
        Help help = helpService.getOne(new QueryWrapper<Help>()
                .eq("title", helpPublishDTO.getTitle())
        );
        Assert.isNull(help, "已存在同样的文章标题！");
        help = new Help();

        // 设置帮助属性值
        BeanUtils.copyProperties(helpPublishDTO, help);
        help.setAuthor(AccountUtil.getProfile().getNickname());
        help.setAuthorId(AccountUtil.getProfile().getId());
        help.setPublishTime(LocalDateTime.now());
        help.setState(HelpState.review.ordinal());
        help.setStateMessage("帮助文章审核中");

        // 添加到数据库中
        Assert.isTrue(helpService.save(help), "帮助文章提交失败！");

        return Result.succeed(help);
    }

    @ApiOperation(value = "帮助文章审核", notes = "管理员接口")
    @PutMapping("/review")
    @RolesAllowed("admin")
    public Result review(@Validated @RequestBody HelpReviewDTO helpReviewDTO) {
        Help help = helpService.getById(helpReviewDTO.getId());
        Assert.notNull(help, "帮助文章不存在，请检查参数是否正确！");

        // 更新值
        help.setState(helpReviewDTO.getState());
        if (StringUtils.isBlank(helpReviewDTO.getStateMessage()) ||
                "帮助文章审核中".equals(helpReviewDTO.getStateMessage()) ||
                "帮助文章正在重新审核中".equals(helpReviewDTO.getStateMessage())) {
            help.setStateMessage(HelpState.accept.ordinal() == help.getState() ? "审核已通过" : "审核未通过");
        } else
            help.setStateMessage(helpReviewDTO.getStateMessage());

        // 更新数据库
        Assert.isTrue(
                helpService.updateById(help),
                "更新帮助文章信息失败，请查看服务器日志！"
        );

        return Result.succeed();
    }

    @ApiOperation(value = "更新一篇帮助文章", notes = "开发者接口")
    @PutMapping("/update")
    @RolesAllowed("developer")
    public Result update(@Validated @RequestBody HelpUpdateDTO helpUpdateDTO) {
        Help help = helpService.getOne(new QueryWrapper<Help>()
                .eq("id", helpUpdateDTO.getId())
                .eq("author_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(help, "不存在的帮助文章！");

        // 检查是否更改了文章标题
        if (!help.getTitle().equals(helpUpdateDTO.getTitle())) {
            Help uniqueHelp = helpService.getOne(new QueryWrapper<Help>()
                    .ne("id", help.getId())
                    .eq("title", helpUpdateDTO.getTitle())
            );

            Assert.isNull(uniqueHelp, "已存在同样标题的帮助文章！");
        }

        // 更新帮助文章属性值
        BeanUtils.copyProperties(helpUpdateDTO, help, "id");
        help.setState(HelpState.review.ordinal());
        help.setStateMessage("帮助文章正在重新审核中");

        // 更新数据库
        Assert.isTrue(helpService.updateById(help), "更新帮助文章失败，请联系管理员！");

        return Result.succeed(help);
    }

    @ApiOperation(value = "分页查询帮助文章", notes = "用户接口")
    @GetMapping("/items/{pageIndex}")
    public Result items(@PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<Help> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 根据用户类型返回不同的结果
        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员能看到的帮助文章
            IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                    // .ne("state", HelpState.delete.ordinal())
                    .eq("state", HelpState.review.ordinal())
            );

            return Result.succeed(PageUtil.filterResult(dataPage));
        }

        // 用户与开发者能看到的帮助文章
        IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                .eq("state", HelpState.accept.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, HelpDetailInfoVO.class));
    }

    @ApiOperation(value = "分页查询自己发布的帮助文章", notes = "开发者接口")
    @GetMapping("/devitems/{pageIndex}")
    @RolesAllowed("developer")
    public Result devItems(@PathVariable Integer pageIndex) {
        IPage<Help> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                .ne("state", HelpState.delete.ordinal())
                .eq("author_id", AccountUtil.getProfile().getId())
        );

        return Result.succeed(PageUtil.filterResult(dataPage));
    }

    @ApiOperation(value = "帮助文章详细信息", notes = "用户接口")
    @GetMapping("/detail/{helpId}")
    public Result detail(@PathVariable Long helpId) {
        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员可见数据
            Help help = helpService.getOne(new QueryWrapper<Help>()
                    .eq("id", helpId)
                    .ne("state", HelpState.delete.ordinal())
            );
            Assert.notNull(help, "帮助文章不存在！");

            return Result.succeed(help);
        }

        // 用户于开发者可见数据
        Help help = helpService.getOne(new QueryWrapper<Help>()
                .eq("id", helpId)
                .eq("state", HelpState.accept.ordinal())
        );
        Assert.notNull(help, "帮助文章不存在！");

        HelpDetailInfoVO helpDetailInfoVO = new HelpDetailInfoVO();
        BeanUtils.copyProperties(help, helpDetailInfoVO);

        return Result.succeed(helpDetailInfoVO);
    }

    @ApiOperation(value = "自己发布的帮助文章的详细信息", notes = "开发者接口")
    @GetMapping("/devdetail/{helpId}")
    @RolesAllowed("developer")
    public Result devDetail(@PathVariable Long helpId) {
        Help help = helpService.getOne(new QueryWrapper<Help>()
                .eq("id", helpId)
                .eq("author_id", AccountUtil.getProfile().getId())
        );
        Assert.notNull(help, "帮助文章不存在！");

        return Result.succeed(help);
    }

    @ApiOperation(value = "分页搜索帮助文章", notes = "用户接口")
    @GetMapping("/search/{keywords}/{pageIndex}")
    public Result search(@PathVariable String keywords, @PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<Help> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        if (UserType.admin.ordinal() == AccountUtil.getProfile().getType()) {
            // 管理员能看到的帮助文章
            IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                    .like("title", keywords)
                    .or()
                    .like("author", keywords)
                    // .ne("state", HelpState.delete.ordinal())
                    .eq("state", HelpState.review.ordinal())
            );

            return Result.succeed(PageUtil.filterResult(dataPage));
        }

        // 用户与开发者能看到的帮助文章
        IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                .like("title", keywords)
                .or()
                .like("author", keywords)
                .eq("state", HelpState.accept.ordinal())
        );

        return Result.succeed(PageUtil.filterResult(dataPage, HelpDetailInfoVO.class));
    }

    @ApiOperation(value = "分页搜索自己的帮助文章", notes = "开发者接口")
    @GetMapping("/devsearch/{keywords}/{pageIndex}")
    @RolesAllowed("developer")
    public Result devSearch(@PathVariable String keywords, @PathVariable Integer pageIndex) {
        IPage<Help> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询数据
        IPage<Help> dataPage = helpService.page(queryPage, new QueryWrapper<Help>()
                .like("title", keywords)
                .or()
                .like("author", keywords)
                .eq("author_id", AccountUtil.getProfile().getId())
                .ne("state", HelpState.delete.ordinal())
        );

        return Result.succeed(dataPage);
    }

    @ApiOperation(value = "删除帮助文章", notes = "开发者与管理员接口")
    @DeleteMapping("/delete/{helpId}")
    @RolesAllowed({"developer", "admin"})
    public Result delete(@PathVariable Long helpId) {
        Help help = helpService.getById(helpId);
        Assert.notNull(help, "帮助文章不存在！");

        // 更新帮助文章信息
        help.setState(HelpState.delete.ordinal());

        // 如果不是管理员，则需要校验是否为帮助文章的所有者
        if (UserType.admin.ordinal() != AccountUtil.getProfile().getType()) {
            // 检查是否为要删除的任务的所有者
            Assert.isTrue(Objects.equals(help.getAuthorId(), AccountUtil.getProfile().getId()), "帮助文章不存在！");
        }

        // 更新数据库
        Assert.isTrue(helpService.updateById(help), "删除帮助文章失败！");

        return Result.succeed();
    }
}
