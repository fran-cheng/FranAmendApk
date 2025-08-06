package com.amend.apk;

import com.amend.utils.FileUtils;

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
		//         -a  changeOtherFiles


		String lastKey = "";
		for (int i = 0; i < args.length; i++) {

			String s = args[i];
			if (i == args.length - 1 && s.startsWith("-")) {
				lastKey = s;
			}
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
				case "-a":
					changeOtherFiles = true;
					break;
			}
			lastKey = s;
		}

		if (workPath == null) {
			throw new RuntimeException("参数-w 是必须的");
		}


		// TODO: 2025/8/6 考虑更新方法， 直接获取包名下的R$文件，收集所有R￥文件，copy并修改smali里面的路径
		FileUtils fileUtils = new FileUtils(workPath, packageName, saveFiles, changeOtherFiles);
		fileUtils.execute();


	}


}
