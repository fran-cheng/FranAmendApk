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

    public static void main(String[] args) {
        File publicFile = new File("G:\\Java\\public\\app\\res\\values\\public.xml");

        Main main = new Main();
        try {
            main.parsePublicXml(publicFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        File smaliFile = new File("G:\\Java\\public\\app\\smali\\android\\support\\percent");
        main.parseFiles(smaliFile);
        main.changeRFileWithPublic();
    }

    private void printRFile() {
        for (File tempFile : mRFiles) {
            System.out.println("clm ==> R_File: " + tempFile.getPath());
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
            File tempOutPutFile = new File("G:\\Java\\temp" + File.separator + fileName);
            System.out.println("clm file ==> " + tempFile.toPath());
            System.out.println("clm fileName ==> " + fileName);

            int startIndex = fileName.lastIndexOf('$') + 1;
            int endIndex = fileName.lastIndexOf(".smali");
            System.out.println("clm startIndex ==> " + startIndex);
            System.out.println("clm endIndex ==> " + endIndex);
            String resType = fileName.substring(startIndex, endIndex);
            System.out.println("clm resType ==> " + resType);

            if (resType.equals("styleable")) {
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
            resValue = line.substring(startIndex, startIndex + 10);
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
    private void parseRSmaliFile() {
        Map<String, String> idMap = new HashMap<>();
    }
}
