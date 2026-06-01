package com.solidoa.file.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

/**
 * 文件 Magic Number（文件头）校验工具
 */
public class MagicNumberUtil {

    private static final Map<String, byte[][]> MAGIC_NUMBERS = new HashMap<>();
    private static final Set<String> TEXT_EXTENSIONS = new HashSet<>();

    static {
        // 文本类型文件扩展名（需要执行脚本检测）
        TEXT_EXTENSIONS.add("txt");
        TEXT_EXTENSIONS.add("csv");
        TEXT_EXTENSIONS.add("html");
        TEXT_EXTENSIONS.add("htm");
        TEXT_EXTENSIONS.add("xml");
        TEXT_EXTENSIONS.add("json");
        TEXT_EXTENSIONS.add("js");
        TEXT_EXTENSIONS.add("css");
        TEXT_EXTENSIONS.add("svg");
        TEXT_EXTENSIONS.add("md");
        TEXT_EXTENSIONS.add("yml");
        TEXT_EXTENSIONS.add("yaml");
        TEXT_EXTENSIONS.add("sql");
        TEXT_EXTENSIONS.add("sh");
        TEXT_EXTENSIONS.add("bat");
        TEXT_EXTENSIONS.add("ini");
        TEXT_EXTENSIONS.add("conf");
        TEXT_EXTENSIONS.add("properties");
    }

    static {
        // 图片
        MAGIC_NUMBERS.put("jpg", new byte[][]{new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}});
        MAGIC_NUMBERS.put("jpeg", new byte[][]{new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}});
        MAGIC_NUMBERS.put("png", new byte[][]{new byte[]{(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47}});
        MAGIC_NUMBERS.put("gif", new byte[][]{new byte[]{(byte) 0x47, (byte) 0x49, (byte) 0x46}});
        MAGIC_NUMBERS.put("webp", new byte[][]{new byte[]{(byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46}});

        // PDF
        MAGIC_NUMBERS.put("pdf", new byte[][]{new byte[]{(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46}});

        // Office 文档 (ZIP 格式)
        MAGIC_NUMBERS.put("docx", new byte[][]{new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}});
        MAGIC_NUMBERS.put("xlsx", new byte[][]{new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}});
        MAGIC_NUMBERS.put("pptx", new byte[][]{new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}});

        // 旧版 Office
        MAGIC_NUMBERS.put("doc", new byte[][]{new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0}});
        MAGIC_NUMBERS.put("xls", new byte[][]{new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0}});
        MAGIC_NUMBERS.put("ppt", new byte[][]{new byte[]{(byte) 0xD0, (byte) 0xCF, (byte) 0x11, (byte) 0xE0}});

        // 压缩文件
        MAGIC_NUMBERS.put("zip", new byte[][]{new byte[]{(byte) 0x50, (byte) 0x4B, (byte) 0x03, (byte) 0x04}});
        MAGIC_NUMBERS.put("rar", new byte[][]{new byte[]{(byte) 0x52, (byte) 0x61, (byte) 0x72, (byte) 0x21}});
        MAGIC_NUMBERS.put("7z", new byte[][]{new byte[]{(byte) 0x37, (byte) 0x7A, (byte) 0xBC, (byte) 0xAF}});

        // 文本
        MAGIC_NUMBERS.put("txt", new byte[][]{}); // 文本文件无固定头部
        MAGIC_NUMBERS.put("csv", new byte[][]{});
    }

    /**
     * 验证文件 Magic Number
     * @param inputStream 文件输入流
     * @param expectedExtension 期望的文件扩展名
     * @return true=验证通过, false=验证失败
     */
    public static boolean validateMagicNumber(InputStream inputStream, String expectedExtension) throws IOException {
        String ext = expectedExtension.toLowerCase();
        byte[][] magicNumbers = MAGIC_NUMBERS.get(ext);

        if (magicNumbers == null || magicNumbers.length == 0) {
            // 无 Magic Number 的文件类型，只检查是否为可读文本
            return validateAsText(inputStream);
        }

        // 读取文件头部
        byte[] header = inputStream.readNBytes(8);

        for (byte[] magic : magicNumbers) {
            if (matchesMagicNumber(header, magic)) {
                return true;
            }
        }

        return false;
    }

    private static boolean matchesMagicNumber(byte[] header, byte[] magic) {
        if (header.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (header[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean validateAsText(InputStream inputStream) throws IOException {
        byte[] bytes = inputStream.readNBytes(512);
        for (byte b : bytes) {
            if (b == 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * 检测恶意文件（包含 PHP/JSP 等脚本特征）
     * 仅对文本类型文件执行检测，避免二进制文件误报
     */
    public static boolean containsScriptContent(InputStream inputStream, String extension) throws IOException {
        // 非文本类型文件跳过脚本检测
        if (extension == null || !TEXT_EXTENSIONS.contains(extension.toLowerCase())) {
            return false;
        }

        byte[] bytes = inputStream.readNBytes(4096);
        String content = new String(bytes).toLowerCase();

        String[] maliciousPatterns = {
            // PHP 模式
            "<?php", "<?=", "<?",
            // JSP/ASP 模式
            "<%@", "<%", "<script", "javascript:",
            // Shell 脚本 shebang
            "#!/bin/bash", "#!/bin/sh", "#!/usr/bin/env python", "#!/usr/bin/python",
            "#!/usr/bin/perl", "#!/bin/ruby",
            // Windows 脚本
            "cmd /c", "powershell", "[kernel]",
            // Java 代码执行
            "jshell", "runtime.exec", "system(", "processbuilder",
            // PHP 危险函数
            "eval(", "base64_decode", "exec(", "passthru", "shell_exec",
            "proc_open", "popen", "create_function", "assert(",
            // 正则/代码注入
            "preg_replace", "preg_match",
            // 配置文件利用
            ".htaccess", "<?xml",
            // 编码/混淆
            "\\x00", "\\x90", "\\xff", "\\x exploit",
            // 脚本语言标识符
            "python", "ruby", "perl",
            // 常见 webshell 特征
            "eval($", "assert($", "base64_decode($", "gzinflate", "str_rot13",
            "chr(", "ord(", "sleep(", "waitfor", "delay",
            // PowerShell 危险命令
            "-encodedcommand", "-enc ", "iex(", "invoke-expression",
            // 文件包含漏洞利用
            "file://", "phar://", "zip://", "data://",
            // SQL 注入尝试
            "union select", "'; drop ", "1=1", "-- ",
            // XSS 向量
            "<img ", "<iframe", "onerror=", "onload=", "onclick="
        };

        for (String pattern : maliciousPatterns) {
            if (content.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}