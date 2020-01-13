package com.netdiskteam.netdisk.controller;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

public class ParamsChecker {

    public static boolean checkStringLength(String str) {
        return str.length() >= 1 && str.length() <= 256;
    }

    public static boolean checkFilePath(String path) {
        if ("".equals(path)) return true;
        if (StringUtils.right(path, 1).equals("/")) return false;
        if (StringUtils.containsAny(path, "<>\\|:\"*?")) return false;
        if (path.equals(".") || path.equals("..")) return false;
        if (StringUtils.replace(path, ".", "").length() == 0) return false;  // 全部由 . 构成
        if (path.endsWith(".")) return false;
        if (path.startsWith(" ")) return false;
        if (path.endsWith(" ")) return false;
        if (StringUtils.contains(path, "//")) return false;
        if (Pattern.matches("/.+/", path)) return false;
        if (!StringUtils.startsWith(path, "/")) return false;
        return checkStringLength(path);
    }

    public static boolean checkFileName(String name) {
        if (StringUtils.containsAny(name, "<>/\\|:\"*?")) return false;
        if (name.equals(".") || name.equals("..")) return false;
        if (StringUtils.replace(name, ".", "").length() == 0) return false;  // 全部由 . 构成
        if (name.endsWith(".")) return false;
        if (name.startsWith(" ")) return false;
        if (name.endsWith(" ")) return false;
        return checkStringLength(name);
    }

    public static boolean checkFileFullPath(String path) {
        if ("".equals(path)) return false;
        return checkFilePath(path);
    }
}
