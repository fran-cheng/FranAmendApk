package com.amend.apk;

import com.amend.utils.FileUtils;

import java.io.*;

/**
 * @author 程良明
 * @date 2022/3/30
 * * 说明:
 **/
public class Main {

    private static final String workPath = "G:\\Java\\public\\app";

    public static void main(String[] args) {

        FileUtils fileUtils = new FileUtils(workPath);

        File smaliFile = new File(fileUtils.linkPath(workPath, new String[]{"smali"}));

        fileUtils.parseFiles(smaliFile);

        fileUtils.saveOriginalFiles(false);

        fileUtils.generateRFile();

        fileUtils.generateOtherFiles();
    }


}
