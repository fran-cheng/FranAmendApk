package com.amend.apk;

import com.amend.utils.FileUtils;

/**
 * @author 程良明
 * @date 2022/3/30
 * * 说明:
 **/
public class Main {


    public static void main(String[] args) {
        String workPath = "G:\\Java\\public\\app";
        String packageName = "heiqi.demo";
        boolean saveFiles = true;
        boolean changeOtherFiles = true;

        FileUtils fileUtils = new FileUtils(workPath, null, saveFiles, changeOtherFiles);
        fileUtils.execute();


    }


}
