package com.amend.apk;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author 程良明
 * @date 2022/3/30
 * * 说明:
 **/
public class Main {
    /**
     * 修正后的文件
     */
    private List<File> mAmendNotRFiles = new ArrayList<>();
    /**
     * 修正后的R文件
     */
    private List<File> mAmendRFiles = new ArrayList<>();
    /**
     * 所有文件路径
     */
    private List<File> mNotRFiles = new ArrayList<>();
    /**
     * 所有R文件路径
     */
    private List<File> mRFiles = new ArrayList<>();


    private Map<String, Map<String, String>> mTypeNameMap;

    /**
     * 由旧的id映射到新的id
     */
    private Map<String, String> mValueMap = new HashMap<>();


    private static final String workPath = "G:\\Java\\public\\app";

    public static void main(String[] args) {


        File publicFile = new File(linkPath(workPath, new String[]{"res", "values", "public.xml"}));

        Main main = new Main();
        try {
            main.parsePublicXml(publicFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        File smaliFile = new File(linkPath(workPath, new String[]{"smali"}));
        main.parseFiles(smaliFile);
        main.changeRFileWithPublic();
        main.checkSmaliFile();
        main.printAmendFile();
    }

    private static String linkPath(String basePath, String[] dentrys) {

        StringBuilder stringBuilder = new StringBuilder(basePath);
        for (String dentry : dentrys) {
            stringBuilder.append(File.separator);
            stringBuilder.append(dentry);
        }
        return stringBuilder.toString();
    }

    private void printAmendFile() {
        for (File tempFile : mAmendNotRFiles) {
            System.out.println("clm ==> mAmendNotRFiles: " + tempFile.getPath());
        }

        for (File tempFile : mAmendRFiles) {
            System.out.println("clm ==> mAmendRFiles: " + tempFile.getPath());
        }

    }

    /**
     * 解析文件夹
     *
     * @param file 文件夹
     */
    private void parseFiles(File file) {
        if (file.isDirectory()) {
            File[] tempList = file.listFiles();
            for (File tempFile : tempList) {
                parseFiles(tempFile);
            }
        } else {
            if (file.getName().startsWith("R$")) {
                mRFiles.add(file);
            } else {
                mNotRFiles.add(file);
            }
        }

    }

    private void changeRFileWithPublic() {
        for (File tempFile : mRFiles) {
            String fileName = tempFile.getName();
            File tempOutPutFile = new File(tempFile.getPath().replace(workPath, linkPath(workPath, new String[]{"workSpace", "amend"})));
            createDirs(tempOutPutFile);
            System.out.println("clm file ==> " + tempFile.toPath());
            System.out.println("clm fileName ==> " + fileName);

            int startIndex = fileName.lastIndexOf('$') + 1;
            int endIndex = fileName.lastIndexOf(".smali");
            System.out.println("clm startIndex ==> " + startIndex);
            System.out.println("clm endIndex ==> " + endIndex);
            String resType = fileName.substring(startIndex, endIndex);
            System.out.println("clm resType ==> " + resType);

            if (resType.equals("styleable")) {
                changeStyleableRFile(tempFile);
                continue;
            }


            Map<String, String> nameTypeMap = mTypeNameMap.get(resType);
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempOutPutFile));

                String lineString = bufferedReader.readLine();
                while (lineString != null) {
                    String[] resSource = parseRFileLine(lineString);
                    if (resSource != null) {
                        String resName = resSource[0];
                        String resValue = resSource[1];
                        System.out.println("clm resNameLenght: " + resName.length());
                        String affirmId = nameTypeMap.get(resName);
                        System.out.println("clm affirmId " + affirmId);
                        if (resValue != null) {
                            if (mValueMap.containsKey(resValue)) {
                                String existId = mValueMap.get(resValue);
                                if (existId != null && affirmId != null && !existId.equals(affirmId)) {
                                    System.err.println("映射可能出错，解析旧R文件存在多个相同id");
                                    System.err.println("原apk可能存在id冲突  : " + existId + "  " + affirmId);
                                    System.out.println("已存在映射： " + resValue + "=" + existId);
                                    System.out.println("更新为： " + resValue + "=" + affirmId);
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
                bufferedWriter.close();
                bufferedReader.close();


            } catch (IOException e) {
                e.printStackTrace();
            }

            mAmendRFiles.add(tempFile);
        }

    }

    private void createDirs(File tempOutPutFile) {
        if (!tempOutPutFile.getParentFile().exists()) {
            tempOutPutFile.getParentFile().mkdirs();
        }
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
            System.out.println("clm  resName : " + resName + "    resValue : " + resValue);
            resSource = new String[2];
            resSource[0] = resName;
            resSource[1] = resValue;
        }
        return resSource;
    }

    private String getHexString(String line) {
        String resValue = null;
        if (line.contains("0x7f")) {
            int startIndex = line.indexOf("0x7f");
            try {
                resValue = line.substring(startIndex, startIndex + 10);
            } catch (Exception e) {
                System.err.println("clm getHexString error");
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
     * 解析public.xml
     * 以这个  Map<String, Map<String, String>> 为基准
     *
     * @param publicFile public.xml文件
     * @return Map<String, Map < String, String>>  ==>  Map<type, Map<name, id>>
     * @throws XmlPullParserException,IOException
     */
    private Map<String, Map<String, String>> parsePublicXml(File publicFile) throws XmlPullParserException, IOException {
        Map<String, Map<String, String>> typeNameMap = new HashMap<>();
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser xmlPullParser = factory.newPullParser();
        xmlPullParser.setInput(new FileInputStream(publicFile), "UTF-8");
        int eventType = xmlPullParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = xmlPullParser.getName();
                if (tagName.equals("public")) {
                    String type = xmlPullParser.getAttributeValue(0);
                    String name = xmlPullParser.getAttributeValue(1);
                    String id = xmlPullParser.getAttributeValue(2);
                    if (!typeNameMap.containsKey(type)) {
                        Map<String, String> nameIdMap = new HashMap<>();
                        typeNameMap.put(type, nameIdMap);
                    }
                    typeNameMap.get(type).put(name, id);
                }
            }
            eventType = xmlPullParser.next();
        }

        mTypeNameMap = typeNameMap;
        System.out.println("clm ==> " + mTypeNameMap);
        System.out.println("attr ==> " + mTypeNameMap.get("attr"));
        return typeNameMap;
    }

    /**
     * key 是原始的id值
     * value 是type_name
     */
    private void checkSmaliFile() {
        for (File tempFile : mNotRFiles) {
            System.out.println("clm checkSmaliFile ==> " + tempFile);
            File tempOutPutFile = new File(tempFile.getPath().replace(workPath, linkPath(workPath, new String[]{"workSpace", "amend"})));
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
                if (fileCount.contains("0x7f")) {
                    System.out.println("clm checkSmaliFile has  ==> " + tempFile);
                    generateAmendFile(tempFile, tempOutPutFile, mAmendNotRFiles);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void generateAmendFile(File tempFile, File tempOutPutFile, List<File> recordList) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(tempFile));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(tempOutPutFile));
        String lineString = bufferedReader.readLine();
        while (lineString != null) {
//                        可能是旧的R文件值
            String resValue = getHexString(lineString);
            if (resValue != null) {
//                             从value 映射拿到最新的值，可能存在多个映射, 需要注意
                String targetValue = mValueMap.get(resValue);
                lineString = amendLine(lineString, resValue, targetValue);
            }
            bufferedWriter.write(lineString + "\r\n");
            lineString = bufferedReader.readLine();
        }
        bufferedWriter.flush();
        bufferedWriter.close();
        bufferedReader.close();
        recordList.add(tempFile);
    }

    private void changeStyleableRFile(File styleableFile) {
        File tempOutPutFile = new File(styleableFile.getPath().replace(workPath, linkPath(workPath, new String[]{"workSpace", "amend"})));
        createDirs(tempOutPutFile);
        try {
            generateAmendFile(styleableFile, tempOutPutFile, mAmendRFiles);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
