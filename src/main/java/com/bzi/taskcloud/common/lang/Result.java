package com.bzi.taskcloud.common.lang;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel(value = "结果模型")
public class Result {
    @ApiModelProperty(value = "结果代码")
    private int code;

    @ApiModelProperty(value = "结果信息")
    private String message;

    @ApiModelProperty(value = "结果数据")
    private Object data;

    public static Result make(int code, String message, Object data) {
        Result result = new Result();

        result.code = code;
        result.message = message;
        result.data = data;

        return result;
    }

    public static Result succeed(Object data) {
        return make(200, "操作成功", data);
    }

    public static Result succeed(String message) {
        return make(200, message, null);
    }

    public static Result succeed() {
        return make(200, "操作成功", null);
    }

    public static Result failed(String message, Object data) {
        return make(400, message, data);
    }

    public static Result failed(String message) {
        return failed(message, null);
    }
}
