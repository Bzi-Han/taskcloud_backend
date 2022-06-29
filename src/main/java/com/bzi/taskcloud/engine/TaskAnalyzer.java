package com.bzi.taskcloud.engine;

import com.bzi.taskcloud.common.lang.TaskType;
import com.bzi.taskcloud.entity.Task;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

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
                var startPos = script.indexOf("'''{", script.indexOf("getTaskFunctionList")) + 3;
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
                var startPos = script.indexOf("`{", script.indexOf("getTaskFunctionList")) + 3;
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
        parentNode.fieldNames().forEachRemaining(fieldName -> {
            result.put(fieldName, finalParentNode.get(fieldName).asText());
        });

        return result;
    }
}
