package com.bzi.taskcloud.engine;

import com.bzi.taskcloud.common.lang.TaskType;
import com.bzi.taskcloud.entity.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class TaskAnalyzer {
    public static String scanDomain(Task task) {
        var script = task.getScript()
                .replace(" ", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "")
                .replace("'", "\"");

        var startPos = script.indexOf("\"", script.indexOf("getTaskDomain()")) + 1;

        return script.substring(startPos, script.indexOf("\"", startPos));
    }

    public static Map<String, String> scanInterface(Task task) throws JsonProcessingException {
        var result = new HashMap<String, String>();
        var objectMapper = new ObjectMapper();
        var script = task.getScript()
                .replace(" ", "")
                .replace("\r", "")
                .replace("\n", "")
                .replace("\t", "");

        JsonNode parentNode = objectMapper.createObjectNode();
        switch (TaskType.values()[task.getType()]) {
            case lua -> {
                var startPos = script.indexOf("[[{", script.indexOf("getTaskFunctionList")) + 3;
                parentNode = objectMapper.readTree(
                        String.format("{%s}",
                            script.substring(
                                    startPos,
                                    script.indexOf("}]]", startPos)
                            )
                        )
                );
            }
            case python -> {
                var startPos = script.indexOf("'''{", script.indexOf("getTaskFunctionList")) + 4;
                parentNode = objectMapper.readTree(
                        String.format("{%s}",
                                script.substring(
                                        startPos,
                                        script.indexOf("}'''", startPos)
                                )
                        )
                );
            }
            case javascript -> {
                var startPos = script.indexOf("`{", script.indexOf("getTaskFunctionList")) + 2;
                parentNode = objectMapper.readTree(
                        String.format("{%s}",
                                script.substring(
                                        startPos,
                                        script.indexOf("}`", startPos)
                                )
                        )
                );
            }
        }

        JsonNode finalParentNode = parentNode;
        parentNode.fieldNames().forEachRemaining(fieldName -> result.put(fieldName, finalParentNode.get(fieldName).asText()));

        return result;
    }

    public static List<Task> resolveTask(File dataDirectory) throws IOException {
        var objectMapper = new ObjectMapper();
        Queue<File> directoryQueue = new LinkedList<>();

        // 遍历目录寻找tasks.json文件
        directoryQueue.add(dataDirectory);
        while (!directoryQueue.isEmpty()) {
            var currentDirectory = directoryQueue.poll();

            // 判断当前目录是否存在tasks.json文件
            if (new File(currentDirectory, "tasks.json").exists()) {
                dataDirectory = currentDirectory;
                break;
            }

            var files = currentDirectory.listFiles();
            if (null != files) {
                for (var file : files) {
                    if (file.isDirectory())
                        directoryQueue.add(file);
                }
            }
        }

        var tasksJsonFile = new File(dataDirectory, "tasks.json");
        Assert.isTrue(tasksJsonFile.exists(), "任务仓库中不存在tasks.json文件");

        var tasksJson = new String(Files.readAllBytes(tasksJsonFile.toPath()));
        Assert.isTrue(!tasksJson.isEmpty(), "tasks.json 为空");

        var tasksInfo = objectMapper.readTree(tasksJson);
        Assert.isTrue(tasksInfo.has("author"), "缺少作者信息");
        Assert.isTrue(tasksInfo.has("scripts"), "缺少脚本信息");

        var tasksNode = tasksInfo.get("scripts").elements();
        var result = new ArrayList<Task>();
        while (tasksNode.hasNext()) {
            var taskNode = tasksNode.next();
            var task = objectMapper.readValue(taskNode.toString(), Task.class);
            var scriptFile = new File(dataDirectory, task.getScript());

            Assert.isTrue(null != task.getName(), "缺少任务名称");
            Assert.isTrue(null != task.getScript(), task.getName() + ": 缺少任务脚本");
            Assert.isTrue(scriptFile.exists(), task.getName() + ": 缺少任务脚本文件");

            task.setAuthorId(-1L);
            task.setAuthor(tasksInfo.get("author").asText());
            task.setType(
                    task.getScript().contains(".lua") ?
                            TaskType.lua.ordinal() :
                            task.getScript().contains(".py") ?
                                    TaskType.python.ordinal() :
                                    TaskType.javascript.ordinal()
            );
            task.setScript(new String(Files.readAllBytes(scriptFile.toPath())));
            if (null == task.getDescription())
                task.setDescription("");
            if (null == task.getWarning())
                task.setWarning("无");
            if (null == task.getVersion())
                task.setVersion("1.0.0");

            result.add(task);
        }

        return result;
    }
}
