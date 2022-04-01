package com.amend.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 程良明
 * @date 2022/3/31
 * * 说明: file 文件操作
 **/
public class FileUtils {
    private static final String KEY_HEX = "0x7f";
    private static final String KEY_WORK_SPACE = "workSpace";
    private static final String KEY_AMEND = "amend";
    private static final String KEY_ORIGINAL = "original";
    private static Map<String, Map<String, String>> mTypeNameMap;
    /**
     * 修正后的文件
     */
    private List<File> mAmendFiles = new ArrayList<>();
    /**
     * 由旧的id映射到新的id
     */
    private Map<String, String> mValueMap = new HashMap<>();
    /**
     * 保存源来apk 就存在的id冲突，并通过这个来尝试修复，其实如果不使用的话问题不大
     */
    private Map<String, String> mMultipleOldId = new HashMap<>();
    /**
     * 工作路径，指apktool解压后的路径
     */
    private final String mWorkPath;

    private List<File> mRFiles = new ArrayList<>();
    private List<File> mOtherFiles = new ArrayList<>();

    public FileUtils(String workPath) {
        mWorkPath = workPath;
        File publicFile = new File(linkPath(workPath, new String[]{"res", "values", "public.xml"}));
        try {
            mTypeNameMap = XmlPullParsePublicXml.parsePublicXml(publicFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析文件夹
     *
     * @param file 文件夹
     */
    public void parseFiles(File file) {
        if (file.isDirectory()) {
            File[] tempList = file.listFiles();
            assert tempList != null;
            for (File tempFile : tempList) {
                parseFiles(tempFile);
            }
        } else {
            if (file.getName().startsWith("R$")) {
                mRFiles.add(file);
            } else {
                mOtherFiles.add(file);
            }
        }
    }

    /**
     * 保存修改前的文件
     *
     * @param all 是否保存全部，默认保存R文件
     */
    public void saveOriginalFiles(boolean all) {
        copyOriginalFiles(mRFiles);
        if (all) {
            copyOriginalFiles(mOtherFiles);
        }
    }


    public void generateRFile() {
        for (File tempFile : mRFiles) {
            String fileName = tempFile.getName();
            File tempOutPutFile = new File(tempFile.getPath().replace(mWorkPath, linkPath(mWorkPath, new String[]{KEY_WORK_SPACE, KEY_AMEND})));
            createDirs(tempOutPutFile);
            int startIndex = fileName.lastIndexOf('$') + 1;
            int endIndex = fileName.lastIndexOf(".smali");
            String resType = fileName.substring(startIndex, endIndex);

            if (resType.equals("styleable")) {
                changeStyleableRFile(tempFile);
                continue;
            }


            Map<String, String> nameTypeMap = mTypeNameMap.get(resType);
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
                 BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempOutPutFile))) {
                String lineString = bufferedReader.readLine();
                while (lineString != null) {
                    String[] resSource = parseRFileLine(lineString);
                    if (resSource != null) {
                        String resName = resSource[0];
                        String resValue = resSource[1];
                        String affirmId = nameTypeMap.get(resName);
                        if (resValue != null) {
                            if (mValueMap.containsKey(resValue)) {
                                if (mMultipleOldId.containsKey(affirmId)) {

                                    continue;
                                }
                                String existId = mValueMap.get(resValue);
                                if (existId != null && affirmId != null && !existId.equals(affirmId)) {
                                    System.err.println("映射可能出错，解析旧R文件存在多个相同id");
                                    System.err.println("原apk可能存在id冲突  : " + existId + "  " + affirmId);
                                    System.out.println("已存在映射： " + resValue + "=" + existId);
                                    System.out.println("更新为： " + resValue + "=" + affirmId);

                                    mMultipleOldId.put(existId, resValue);
                                    mMultipleOldId.put(affirmId, resValue);
                                }

                            }
                            mValueMap.put(resValue, affirmId);
                        }
                        lineString = amendLine(lineString, resValue, affirmId);
                    }

                    bufferedWriter.write(lineString + "\r\n");

                    lineString = bufferedReader.readLine();
                }
                bufferedWriter.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }

            mAmendFiles.add(tempFile);
        }

    }

    /**
     * 拼接地址
     *
     * @param basePath 基础路径
     * @param dentrys  拼接路径 String[]
     * @return path
     */

    public String linkPath(String basePath, String[] dentrys) {
        StringBuilder stringBuilder = new StringBuilder(basePath);
        for (String dentry : dentrys) {
            stringBuilder.append(File.separator);
            stringBuilder.append(dentry);
        }
        return stringBuilder.toString();
    }

    /**
     * 如果上一级目录不存在，则生成上一级目录文件夹
     *
     * @param file 文件
     */
    private void createDirs(File file) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
    }

    /**
     * 修改Styleable
     *
     * @param styleableFile file
     */
    private void changeStyleableRFile(File styleableFile) {
        File tempOutPutFile = new File(styleableFile.getPath().replace(mWorkPath, linkPath(mWorkPath, new String[]{KEY_WORK_SPACE, KEY_AMEND})));
        createDirs(tempOutPutFile);
        generateAmendFile(styleableFile, tempOutPutFile);
    }

    /**
     * 解析R文件的一行
     *
     * @param line R文件的1行
     * @return string[0]是resName string[1]是resValue
     */
    private String[] parseRFileLine(String line) {
        String[] resSource = null;
        if (line.startsWith(".field public static final")) {
            String[] strings = line.split(":");
            String resName = strings[0].substring(strings[0].lastIndexOf(" ") + 1);
            String resValue = getHexString(strings[1]);
            resSource = new String[2];
            resSource[0] = resName;
            resSource[1] = resValue;
        }
        return resSource;
    }

    private String getHexString(String line) {
        String resValue = null;
        if (line.contains(KEY_HEX)) {
            int startIndex = line.indexOf(KEY_HEX);
            try {
                resValue = line.substring(startIndex, startIndex + 10);
            } catch (Exception ignored) {
                resValue = null;
            }
        }

        return resValue;
    }


    private String amendLine(String line, String sourceString, String targetString) {
        if (sourceString != null && targetString != null) {
            line = line.replace(sourceString, targetString);
        }
        return line;
    }

    /**
     * 使用映射值来修改文件
     *
     * @param tempFile       源文件
     * @param tempOutPutFile 输出文件
     */
    private void generateAmendFile(File tempFile, File tempOutPutFile) {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempOutPutFile))) {

            String lineString = bufferedReader.readLine();
            while (lineString != null) {
//                        可能是旧的R文件值
                String resValue = getHexString(lineString);
                if (resValue != null) {
//                             从value 映射拿到最新的值，可能存在多个映射, 需要注意
                    if (mMultipleOldId.containsKey(resValue)) {
                        // TODO: 2022/4/1  通过一些方法来确定 使用那个id来匹配，    使用原包名下的R文件做模板？！匹配的概率最大，然后就是根据路径最近？！
                    }
                    String targetValue = mValueMap.get(resValue);
                    lineString = amendLine(lineString, resValue, targetValue);
                }
                bufferedWriter.write(lineString + "\r\n");
                lineString = bufferedReader.readLine();
            }
            bufferedWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
        mAmendFiles.add(tempFile);
    }


    /**
     * key 是原始的id值
     * value 是type_name
     */
    public void generateOtherFiles() {
        for (File tempFile : mOtherFiles) {
            File tempOutPutFile = new File(tempFile.getPath().replace(mWorkPath, linkPath(mWorkPath, new String[]{KEY_WORK_SPACE, KEY_AMEND})));
            createDirs(tempOutPutFile);
            try {
                FileReader fileReader = new FileReader(tempFile);
                StringBuilder stringBuilder = new StringBuilder();
                char[] chars = new char[1024];
                int length = 0;
                while (length != -1) {
                    length = fileReader.read(chars);
                    stringBuilder.append(chars);
                }
                fileReader.close();
                String fileCount = stringBuilder.toString();
                if (fileCount.contains(KEY_HEX)) {
                    generateAmendFile(tempFile, tempOutPutFile);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将原来的文件保存一份
     */
    private void copyOriginalFiles(List<File> files) {
        for (File tempFile : files) {
            String outPutPath = tempFile.getPath().replace(mWorkPath, linkPath(mWorkPath, new String[]{KEY_WORK_SPACE, KEY_ORIGINAL}));
            File outPutFile = new File(outPutPath);
            createDirs(outPutFile);
            try (FileReader fileReader = new FileReader(tempFile);
                 FileWriter fileWriter = new FileWriter(outPutFile)) {
                char[] chars = new char[1024];
                int length = fileReader.read(chars);
                while (length != -1) {
                    fileWriter.write(chars);
                    length = fileReader.read(chars);
                }
                fileWriter.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
