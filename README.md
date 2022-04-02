# FranAmendApk
用来修正合并后的apk的id冲突
使用命令示例
java -jar FranAmendApk.jar -w G:\Java\public\app -p com.fran.test -s -a
命令解释
       -w  workPath     工作路径，指apktool解压后的路径
       -p  packageName  包名，使用改包名路径下的R文件来修正 samli文件下写死的id值
       -s  saveFiles    是否保存工作过程目录, -s指保存，默认生成在workPath下的workSpace
       -a  changeOtherFiles 修改所有的smali文件的id，默认指修改R$开头的文件，使用该参数强制修改所以smali文件
