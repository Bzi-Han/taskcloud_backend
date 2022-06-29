package com.bzi.taskcloud.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
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
@ApiModel(value = "Task对象", description = "")
public class Task implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String warning;

    private String interfaces;

    private String domain;

    private String author;

    @ApiModelProperty("ID")
    private Long authorId;

    private Integer type;

    private String script;

    private String version;

    private Float rating;

    private Integer state;

    private String stateMessage;


}
