package com.bzi.taskcloud.engine;

@FunctionalInterface
public interface TaskCompleteCallback {
    void run(boolean result);
}
