package com.bzi.taskcloud.common.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TaskDetailInfoDeveloperVO extends TaskDetailInfoUserVO {
    private String script;

    private Integer state;

    private String stateMessage;
}
