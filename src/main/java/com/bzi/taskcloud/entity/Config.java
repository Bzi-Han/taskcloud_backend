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
@ApiModel(value = "Config对象", description = "")
public class Config implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @ApiModelProperty("ID")
    private Long userId;

    private String domain;

    private String passport;

    private Integer referenceCount;


}
