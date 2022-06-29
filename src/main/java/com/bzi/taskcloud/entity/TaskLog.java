package com.bzi.taskcloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author Bzi_Han
 * @since 2022-05-11
 */
@Getter
@Setter
@TableName("task_log")
@ApiModel(value = "TaskLog对象", description = "")
public class TaskLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("ID")
    private Long userId;

    @ApiModelProperty("ID")
    private Long packageId;

    @ApiModelProperty("ID")
    private Long taskId;

    private String packageName;

    private String taskName;

    private String taskVersion;

    private String functions;

    private Integer state;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executeOnTime;

    private String fromWhere;


}
