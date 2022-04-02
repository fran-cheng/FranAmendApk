package com.amend.apk;

import com.amend.utils.FileUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author 程良明
 * @date 2022/3/30
 * * 说明:
 **/
public class Main {


    public static void main(String[] args) {

        String workPath = null;
        String packageName = null;
        boolean saveFiles = false;
        boolean changeOtherFiles = false;
        //         -w  workPath
//         -p  packageName
//         -s  saveFiles
//         -c  saveFiles


        String lastKey = "";
        Map<String, String> stringStringMap = new HashMap<>();
        for (String s : args) {
            switch (lastKey) {
                case "-w":
                    workPath = s;
                    break;
                case "-p":
                    packageName = s;
                    break;
                case "-s":
                    saveFiles = true;
                    break;
                case "-c":
                    changeOtherFiles = true;
                    break;
            }
            lastKey = s;
        }

        if (workPath == null) {
            throw new RuntimeException("参数-w 是必须的");
        }


        FileUtils fileUtils = new FileUtils(workPath, packageName, saveFiles, changeOtherFiles);
        fileUtils.execute();


    }


}
