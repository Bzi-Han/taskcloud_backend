package com.bzi.taskcloud.common.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.zip.ZipFile;

public class ZipUtil {

    public static boolean unzip(File zipFile) throws IOException {
        var zip = new ZipFile(zipFile);
        var entries = zip.entries();

        while (entries.hasMoreElements()) {
            var entry = entries.nextElement();
            var file = new File(zipFile.getParentFile(), entry.getName());

            if (entry.isDirectory()) {
                if (!file.mkdirs())
                    return false;
            } else {
                // 判断是否存在扩展名后缀
                if (entry.getName().contains(".")) {
                    // 判断是否为以下文件类型，如果不是则跳过
                    if (!entry.getName().contains(".lua")
                        && !entry.getName().contains(".py")
                        && !entry.getName().contains(".js")
                        && !entry.getName().contains(".json")
                    ) {
                        continue;
                    }
                }

                if (!file.getParentFile().exists()) {
                    if (!file.getParentFile().mkdirs())
                        return false;
                }

                Files.copy(zip.getInputStream(entry), file.toPath());
            }
        }

        return true;
    }

}
