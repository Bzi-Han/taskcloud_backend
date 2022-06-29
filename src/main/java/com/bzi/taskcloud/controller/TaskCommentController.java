package com.bzi.taskcloud.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bzi.taskcloud.common.dto.TaskCommentSendDTO;
import com.bzi.taskcloud.common.lang.Result;
import com.bzi.taskcloud.common.utils.AccountUtil;
import com.bzi.taskcloud.common.utils.PageUtil;
import com.bzi.taskcloud.common.vo.TaskCommentDetailInfoVO;
import com.bzi.taskcloud.entity.Task;
import com.bzi.taskcloud.entity.TaskComment;
import com.bzi.taskcloud.security.data.DecryptRequest;
import com.bzi.taskcloud.security.data.EncryptResponse;
import com.bzi.taskcloud.service.ITaskCommentService;
import com.bzi.taskcloud.service.ITaskService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

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
@RequestMapping("/taskComment")
@Api(value = "任务评价模块", description = "任务评价模块")
public class TaskCommentController {
    @Autowired
    private ITaskCommentService taskCommentService;
    @Autowired
    private ITaskService taskService;
    @Autowired
    private DataSourceTransactionManager dataSourceTransactionManager;
    @Autowired
    private TransactionDefinition transactionDefinition;

    @ApiOperation(value = "对指定任务发送评论与评分", notes = "用户接口")
    @PostMapping("/send")
    public Result send(@Validated @RequestBody TaskCommentSendDTO taskCommentSendDTO) {
        Task task = taskService.getById(taskCommentSendDTO.getTaskId());
        Assert.notNull(task, "任务不存在！");

        // 判断是否已经评论过
        TaskComment taskComment = taskCommentService.getOne(new QueryWrapper<TaskComment>()
                .eq("task_id", taskCommentSendDTO.getTaskId())
                .eq("author_id", AccountUtil.getProfile().getId())
        );
        Assert.isNull(taskComment, "不能重复评论！");

        // 创建评论
        taskComment = new TaskComment();
        BeanUtils.copyProperties(taskCommentSendDTO, taskComment);
        taskComment.setAuthor(AccountUtil.getProfile().getNickname());
        taskComment.setAuthorId(AccountUtil.getProfile().getId());
        taskComment.setPublishTime(LocalDateTime.now());

        // 保存到数据库
        TransactionStatus transactionStatus = dataSourceTransactionManager.getTransaction(transactionDefinition);
        try {
            // 保存评论
            Assert.isTrue(taskCommentService.save(taskComment), "评论发送失败！");

            // 更新任务评分
            task.setRating(taskCommentService.getAvgRating(task.getId()));
            Assert.isTrue(taskService.updateById(task), "评论发送失败！");

            dataSourceTransactionManager.commit(transactionStatus);
        } catch (Exception exception) {
            dataSourceTransactionManager.rollback(transactionStatus);
            throw exception;
        }

        // 返回数据
        TaskCommentDetailInfoVO taskCommentDetailInfoVO = new TaskCommentDetailInfoVO();
        BeanUtils.copyProperties(taskComment, taskCommentDetailInfoVO);

        return Result.succeed(taskCommentDetailInfoVO);
    }

    @ApiOperation(value = "分页查询指定任务的评论与评分", notes = "用户接口")
    @GetMapping("/items/{taskId}/{pageIndex}")
    public Result items(@PathVariable Long taskId, @PathVariable Integer pageIndex) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        IPage<TaskComment> queryPage = new Page<>();
        queryPage.setCurrent(pageIndex);
        queryPage.setSize(10);

        // 从数据库中查询评论
        IPage<TaskComment> dataPage = taskCommentService.page(queryPage, new QueryWrapper<TaskComment>()
                .eq("task_id", taskId)
                .orderByDesc("publish_time")
        );

        return Result.succeed(PageUtil.filterResult(dataPage, TaskCommentDetailInfoVO.class));
    }
}
