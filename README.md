# 间隔背单词 WordApp

基于艾宾浩斯遗忘曲线的 Android 背单词应用，覆盖 B4 业务核心与 P4 导出分享模块。

## 技术栈

- 语言：Kotlin（AGP 9.3 内置 Kotlin 支持，无需额外 Kotlin 插件）
- 构建：Gradle 9.5 + AGP 9.3.0，compileSdk/targetSdk 37，minSdk 24
- UI：ViewBinding + Material 3 + RecyclerView + ConstraintLayout
- 存储：原生 SQLite（SQLiteOpenHelper，无 Room 依赖）
- 通知：AlarmManager + BroadcastReceiver + NotificationChannel
- 分享：FileProvider + Intent.ACTION_SEND（全程 content://，无 file://）

## 需求与实现对照

### B4 业务核心

| 需求 | 实现文件 | 说明 |
| --- | --- | --- |
| 艾宾浩斯间隔重复 | `data/EbbinghausScheduler.kt` | 间隔序列 1/2/4/7/15/30 天；答对升阶按新间隔排期，答错降阶且 10 分钟后重学 |
| 复习队列 | `data/DbHelper.kt`、`ui/ReviewActivity.kt` | 查询 `next_review_at <= now` 的到期词，按到期时间排序，再补未学新词 |
| 词库导入 | `export/CsvUtil.kt`、`MainActivity.kt` | SAF `ACTION_OPEN_DOCUMENT` 选 CSV，UTF-8 解析，支持引号/逗号，唯一索引去重批量入库 |
| 测试 | `ui/QuizActivity.kt` | 看英文选释义四选一，随机出题、即时红绿反馈、结束给出得分与正确率 |
| 通知提醒 | `notify/NotificationHelper.kt`、`AlarmReceiver.kt`、`BootReceiver.kt` | 通知渠道 + 每日固定时间闹钟，通知显示到期数并可点开 App；开机自动重建；Android 13+ 申请通知权限，无精确闹钟权限时自动退化 |

### P4 导出分享模块

| 需求 | 实现文件 | 说明 |
| --- | --- | --- |
| 生成 Bitmap 图片 | `export/BitmapReport.kt` | 纯 Canvas 手绘 1080×1440 学习报告（渐变头、正确率、四项统计） |
| 生成 CSV 文件 | `export/CsvUtil.kt`、`export/ShareHelper.kt` | 导出全部单词及复习状态，字段含特殊字符时自动加引号转义 |
| Intent + FileProvider 跨 App 分享 | `export/ShareHelper.kt`、`res/xml/file_paths.xml`、`AndroidManifest.xml` | 文件写入 `cacheDir/share/`，经 FileProvider 生成 `content://` URI，`ACTION_SEND` 配合 `FLAG_GRANT_READ_URI_PERMISSION` 分享；另支持 SAF `ACTION_CREATE_DOCUMENT` 另存本地 |

> Android 7.0（API 24）起禁止跨 App 直接使用 `file://`，本项目所有分享 URI 均由 FileProvider 生成，代码中不出现 file://。

## 如何打开与运行

1. 打开 Android Studio，选择 **Open**（不要 New），选择本 `WordApp` 目录。
2. 等待 Gradle Sync 完成（依赖本地已缓存，可离线）。
3. 启动模拟器或连接手机，点击 Run；或直接安装已构建的
   `app/build/outputs/apk/debug/app-debug.apk`。

## 命令行构建

```
gradlew.bat clean assembleDebug
```

产物路径：`app/build/outputs/apk/debug/app-debug.apk`

## 导入 CSV 格式

每行一个单词，首行表头可选：

```
word,meaning,example
abandon,v. 放弃；抛弃,Never abandon your dream.
```

- 三列依次为：单词、释义、例句（例句可省略）
- 字段内含逗号时用英文双引号包裹，引号本身写成 `""`
- 与库中已有单词重复的行会自动跳过

应用首次启动会预置 20 个示例单词，可直接体验全部功能。
