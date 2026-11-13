# 单词记忆（WordMemo）

《Android 移动终端应用开发技术》课程设计项目，同一项目产出两个独立可评分成果：

| 部分 | 课程 | 定位 |
| --- | --- | --- |
| 主 App —— **B4 单词记忆** | Android 移动终端应用开发技术（2 学分） | 追求技术深度 |
| 伴生模块 —— **P4 分享 / 导出卡片** | 移动互联网应用开发实战（1 学分） | 小而完整、可独立验收 |

## 选题

- 主 App：**B4 单词记忆**——基于艾宾浩斯遗忘曲线的复习调度、复习队列、词库导入、测试模式。
- 伴生模块：**P4 分享 / 导出卡片**——生成统计卡片位图与 CSV 数据文件，通过系统分享面板发送到其他 App。

选题说明见 [`docs/01-选题说明.md`](docs/01-选题说明.md)，功能清单见 [`docs/02-功能清单.md`](docs/02-功能清单.md)，伴生模块设计见 [`docs/03-伴生模块-P4-设想.md`](docs/03-伴生模块-P4-设想.md)，数据模型与算法见 [`docs/04-数据模型与技术选型.md`](docs/04-数据模型与技术选型.md)。

## 当前进度

已完成数据层、调度算法与界面骨架，可直接运行：

- 词库列表 → 词库详情 → 词条详情 三级导航可点击；
- 词库与词条真实写入 Room，重启 App 后仍在；
- 词条详情提供四档自评按钮，点击即按算法重算下次复习时间并写入复习记录；
- 首次启动自动写入内置示例词库。

## 技术栈

- 语言：Kotlin（原生 Android，不使用 Flutter / React Native / uni-app 等跨平台框架）
- UI：Jetpack Compose + Material 3
- 持久化：Room（词库、词条、复习记录）
- 异步：Kotlin 协程 + Flow + ViewModel
- 伴生模块跨进程通信：Intent、FileProvider、Bitmap 缓存目录
- 构建：Gradle 9.5.0 + AGP 9.3.0，`compileSdk 37`、`minSdk 26`

## 构建与运行

用 Android Studio 打开本目录（工程根目录），等待 Gradle 同步完成后直接运行 `app` 模块即可。
首次同步需要联网下载 AndroidX / Compose 依赖。命令行构建：

```powershell
.\gradlew.bat :app:assembleDebug
```

本机 JDK 直接使用 Android Studio 自带的运行时（`Android Studio\jbr`），无需单独安装 JDK。

### 单元测试说明

算法测试位于 `app/src/test/java/.../EbbinghausSchedulerTest.kt`。在 Android Studio 内直接点测试类运行即可。
命令行 `.\gradlew.bat :app:testDebugUnitTest` 在本机 JDK 25 环境下会因 Gradle 测试 worker 无法启动而失败（详见 `docs/04-数据模型与技术选型.md` 的已知限制），改用 JUnitCore 直接执行编译产物可得到同样结果。

## 目录结构

```
WordMemo/
├── app/                          # 主 App 模块（B4 单词记忆）
│   └── src/main/java/com/wuyuhui/wordmemo/
│       ├── data/
│       │   ├── local/            # Room 实体、DAO、数据库
│       │   └── repository/       # 仓库层，聚合数据访问
│       ├── domain/scheduler/     # 艾宾浩斯调度算法（纯函数，可单测）
│       └── ui/
│           ├── components/       # 加载 / 空 / 错误三态组件
│           ├── library/          # 词库列表、词库详情
│           ├── word/             # 词条详情与自评
│           ├── placeholder/      # 今日、测试、统计（后续里程碑接入）
│           ├── navigation/       # 导航图与底部栏
│           └── theme/            # 主题
├── app/src/test/                 # 调度算法单元测试
├── docs/                         # 选题、功能清单、伴生模块、数据模型文档
├── gradle/libs.versions.toml      # 依赖版本集中管理
├── build.gradle.kts
└── settings.gradle.kts
```

## 开发里程碑

课程要求 10 次实验课推进，当前完成：

- [x] 里程碑 1：选题登记 + 主 App 功能清单 + 伴生模块设想 + 建仓首次提交
- [x] 里程碑 2：原型图 + 技术选型 + 数据模型
- [x] 里程碑 3：主 App 骨架 + 导航 + 基础 UI
- [ ] 里程碑 4：主 App 核心功能 ①：数据持久化
- [ ] 里程碑 5：主 App 核心功能 ②：网络或设备能力
- [ ] 里程碑 6：主 App 核心功能 ③ + 主流程整合
- [ ] 里程碑 7：权限 / 异常 / 空态 + 伴生模块立项与骨架
- [ ] 里程碑 8：伴生模块核心功能 + 主 App UI 打磨
- [ ] 里程碑 9：伴生模块完成 + 录屏 / 问答卷 / PPT
- [ ] 里程碑 10：正式提交 + 抽查答辩

## 学术诚信

允许使用 LLM / AI 辅助，但不直接抄袭他人项目、不购买代码、不伪造 Git 记录或录屏。项目代码需能自行解释，抽象问答时将逐行说明关键实现。
