# 听课打标（TingKe）

面向中学**随堂听课 / 评课 / 教研复盘**的安卓 App：**计时 + 一键打标 + 同步录音 + 独立评课标记 + 环节占比**。

上课时轻点标签就能记下"这一刻在干什么"，下课自动生成时间轴与环节占比，可导出文本 / CSV / 长图发到教研群，
也支持回听录音核对课堂原声。

- 平台：Android 8.0+（minSdk 26 / targetSdk 34）
- 技术栈：Kotlin + Jetpack Compose（Material 3）、Room、MediaRecorder、前台服务录音、Canvas 导图
- 界面：仅手机竖屏，单 Activity
- 当前版本：**v1.6.1**（versionCode 6）
- 许可：[MIT](LICENSE)

## 下载安装

到 [**Releases**](../../releases/latest) 页下载 `tingke-app-<版本>-release.apk`，传到手机点击安装
（首次需在系统里允许"安装未知来源应用"）。这是**正式签名包**，可覆盖升级，不会丢历史记录。

> 从 v1.2 的 debug 包升级到正式签名包时，因签名不同需要先卸载再装（历史记录会丢，可先在旧版里把报告导出 CSV）。

## 功能

### 听课计时与一键打标

- 开始页填听课信息，默认勾选**同时录音**（可取消），点「开始听课」即计时 + 录音
- 14 个课堂环节标签常驻底部标签坞：导入 / 目标呈现 / 新授讲解 / 实验演示 / 学生实验 / 探究活动 /
  提问互动 / 当堂检测 / 巩固练习 / 小结 / 作业布置 / 板书板图 / 多媒体与信息技术 / 拓展延伸
- **轻点**立即落一条打标（顶部提示可撤销）；**长按**补充子标签与备注；子标签支持「自定义…」自填
- 计时页实时显示当前环节及已持续时长；「上一条」是当前环节之前那条，可就地修改
- 计时中屏幕常亮；返回键有二次确认

### 独立评课标记

- 亮点 ⭐ / 不足 ⚠️ / 疑问 ❓：轻点立即记录，长按可加备注
- 它是时间轴上的**点事件**，不占环节时长、不计入环节占比，报告里单独成段

### 录音

- 前台服务（`foregroundServiceType="microphone"`）保证熄屏 / 切后台持续录音
- 分段录制；报告页逐段回听，进度可拖动、播完复位
- 单段 / 多段分享（`ACTION_SEND` + FileProvider），可发微信、存网盘
- 音频只存在 App 私有目录，不上传；删除听课记录会一并删除音频

### 报告与导出

- 环节占比（累计时长 ÷ 总计时，保留 1 位小数）+ 未打标段
- 文本时间轴、CSV（UTF-8 BOM，Excel 直开不乱码）
- 一键生成「环节占比长图」，直接发群

### 历史记录

- 结束即存档；冷启动可恢复未结束的计时
- 可修改听课信息（课题 / 学段 / 科目 / 教师 / 单位 / 班级 / 听课人 / 单位 / 日期 / 节次 / 人数 / 地点）
- 删除有二次确认，并连带删除该节录音文件

### 听课信息字段

| 字段 | 取值 | 默认 |
|---|---|---|
| 学段 | 初中 / 高中 / 自定义 | 初中 |
| 科目 | 语文、数学、英语、物理、化学、生物、道德与法治、历史、地理、科学、体育与健康、音乐、美术、信息技术、通用技术 / 自定义 | 物理 |
| 授课教师单位、听课人单位 | 自由文本 | 晋江市陈埭民族中学 |

> 学校名与选项表集中在 `app/src/main/java/com/phygroup/tingke/domain/Core.kt` 的 `object Meta`，改一处即可。

## 截图

| 开始页 | 计时页（一键打标） |
|---|---|
| ![开始页](docs/screenshots/01-开始页-默认勾选录音.png) | ![计时页](docs/screenshots/02-计时页-一键打标标签坞.png) |

| 打标弹窗（时刻已冻结） | 时间轴 |
|---|---|
| ![打标](docs/screenshots/03-打标弹窗-时刻已冻结.png) | ![时间轴](docs/screenshots/04-时间轴抽屉.png) |

| 报告页（环节占比） | 历史记录 |
|---|---|
| ![报告](docs/screenshots/05-报告页-环节占比.png) | ![历史](docs/screenshots/07-历史记录.png) |

截图取自 v1.2 交互原型，功能与当前一致；`docs/交互原型.html` 是可点按的完整原型。

## 构建

需要 **JDK 17** 与 Android SDK（platform android-34、build-tools 34.0.0）。

```bash
git clone https://github.com/dashixdot/tingke-app.git
cd tingke-app
echo "sdk.dir=/path/to/Android/Sdk" > local.properties   # 或用 Android Studio 直接打开
./gradlew assembleDebug                                  # Windows: gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`
（本地路径含非 ASCII 字符时，需在 `gradle.properties` 加 `android.overridePathCheck=true`。）

Gradle 8.7 + AGP 8.5.2 + Kotlin 1.9.24 + Compose BOM 2024.06.00 + Room 2.6.1（KSP）。
wrapper 指向腾讯云镜像，如需官方源改 `gradle/wrapper/gradle-wrapper.properties`。

### 打正式包

仓库不带任何签名材料。要出 release 包，在仓库根目录自建 `keystore.properties`（已在 `.gitignore` 中排除）：

```properties
storeFile=/absolute/path/to/your-release.jks
storePassword=****
keyAlias=tingke
keyPassword=****
```

没有该文件时 `assembleRelease` 产出未签名包（可用来校验编译）；有则产出已签名包。

## 目录结构

```
.
├── app/src/main/
│   ├── AndroidManifest.xml          权限 / 竖屏 / FileProvider / 录音前台服务
│   ├── java/com/phygroup/tingke/
│   │   ├── MainActivity.kt           单 Activity + Compose，锁竖屏、计时常亮
│   │   ├── audio/                    MediaRecorder 分段录制、录音前台服务
│   │   ├── data/                     Room 实体/DAO（sessions/marks/audio_segments）、仓库
│   │   ├── domain/Core.kt            标签体系、学段科目选项、统计口径、文本/CSV 生成
│   │   ├── export/Exports.kt         分享文本 / CSV / 长图 / 录音
│   │   └── ui/                       开始页、计时页、打标弹窗、时间轴、报告页、历史页
│   └── res/
├── docs/
│   ├── 设计说明.md                   目标、页面流、标签体系、数据结构、统计口径、实现要点
│   ├── 交互原型.html                 可点按的 HTML 原型
│   └── screenshots/
└── gradle/, gradlew, *.gradle.kts
```

## 统计口径

- 环节时长：某条环节标记的时长 = 下一条**环节**标记时刻 − 本条时刻；末段 = 结束时刻 − 末条时刻
- 未打标：0 → 首个环节标记；无环节标记则整节未打标
- 占比：该类累计时长 ÷ 总计时，保留 1 位小数
- 评课标记：不参与以上计算

## 版本

| 版本 | 主要内容 |
|---|---|
| v1.2 | 首版：计时、一键打标、录音、报告、历史 |
| v1.3 | 熄屏/后台录音（前台服务）、修复历史打标丢失、回放进度、提示遮挡、返回键、录音分享 |
| v1.4 | 全竖屏；历史记录可修改听课信息 |
| v1.5 | 历史删除二次确认；子标签可自填 |
| v1.6 | 补充细节归位当前环节、「上一条」可单独修改；**评课标记独立成条且不进环节占比**；删除记录连带删录音 |
| v1.6.1 | 听课信息增加教师单位 / 听课人单位 / 学段 / 科目；Room 升级到 v2（显式迁移，历史记录不丢） |

详细设计与实现要点见 [docs/设计说明.md](docs/设计说明.md)。

## 许可

[MIT](LICENSE) © 2026 dashixdot
