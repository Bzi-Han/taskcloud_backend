package com.bzi.taskcloud.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LoggerUtil {
    private static final PrintWriter m_runtimeLogFile;
    private static final PrintWriter m_runtimeTraceFile;

    static {
        try {
            File logPath = new File(System.getProperty("user.dir") + "/logs");
            if (!logPath.exists()) {
                if (!logPath.mkdir()) {
                    throw new IOException("无法创建日志目录");
                }
            }

            m_runtimeLogFile = new PrintWriter(System.getProperty("user.dir") + "/logs/runtime_log.log");
            m_runtimeTraceFile = new PrintWriter(System.getProperty("user.dir") + "/logs/runtime_trace.log");
        } catch (IOException e) {
            throw new java.lang.Error("====================================初始化日志输出失败====================================");
        }
    }

    private static void print(String type, String message, StackTraceElement stackTraceElement) {
        String log = String.format(
                "[%s] [%s][%s] -> [%s::%s::%d] : %s",
                type,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                stackTraceElement.getFileName(),
                stackTraceElement.getClassName(),
                stackTraceElement.getMethodName(),
                stackTraceElement.getLineNumber(),
                message
        );
        m_runtimeLogFile.println(log);
        m_runtimeLogFile.flush();
        if ("-".equals(type) && !message.contains("密失败")) {
            m_runtimeTraceFile.println(log);
            new Throwable().printStackTrace(m_runtimeTraceFile);
            m_runtimeTraceFile.flush();
        }

        System.out.println(log);
    }

    public static void operation(String message) {
        print("*", message, new Throwable().getStackTrace()[1]);
    }

    public static void operation(Throwable throwable) {
        print("*", throwable.getMessage(), new Throwable().getStackTrace()[1]);
    }

    public static void operation(String message, Throwable throwable) {
        print("*", String.format("%s : %s", message, throwable.getMessage()), new Throwable().getStackTrace()[1]);
    }

    public static void info(String message) {
        print("=", message, new Throwable().getStackTrace()[1]);
    }

    public static void info(Throwable throwable) {
        print("=", throwable.getMessage(), new Throwable().getStackTrace()[1]);
    }

    public static void info(String message, Throwable throwable) {
        print("=", String.format("%s : %s", message, throwable.getMessage()), new Throwable().getStackTrace()[1]);
    }

    public static void succeed(String message) {
        print("+", message, new Throwable().getStackTrace()[1]);
    }

    public static void succeed(Throwable throwable) {
        print("+", throwable.getMessage(), new Throwable().getStackTrace()[1]);
    }

    public static void succeed(String message, Throwable throwable) {
        print("+", String.format("%s : %s", message, throwable.getMessage()), new Throwable().getStackTrace()[1]);
    }

    public static void failed(String message) {
        print("-", message, new Throwable().getStackTrace()[1]);
    }

    public static void failed(Throwable throwable) {
        print("-", throwable.getMessage(), new Throwable().getStackTrace()[1]);
    }

    public static void failed(String message, Throwable throwable) {
        print("-", String.format("%s : %s", message, throwable.getMessage()), new Throwable().getStackTrace()[1]);
    }
}
