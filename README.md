# 超级点击器 Core · 核心功能版

用 Kotlin + AccessibilityService 实现的安卓自动化点击工具，对应原版「超级点击器」的**核心功能**（不含图像识别/OCR）。

## 功能

| 步骤类型 | 参数 |
|---|---|
| 单击 | 坐标 X/Y、时长、连击次数、连击间隔 |
| 多点点击 | 多个坐标同时按下（`x,y;x,y;...`） |
| 滑动 | 起点 X/Y、终点 X/Y、时长 |
| 等待 | 等待毫秒数 |
| 返回键 / Home 键 | 无参数 |

其他：步骤启用/禁用、规则保存与加载、循环次数（0=无限）、悬浮控制台（开始/暂停/停止，可拖动）、屏幕坐标拾取。

## 拿到 APK 的三种方式

### 方式一：GitHub Actions 云编译（推荐，本机无需 Java）

1. 在 GitHub 新建一个空仓库，例如 `SuperClicker`
2. 把本目录（`SuperClicker/`）里的**所有文件**上传到仓库（包括 `.github/` 隐藏目录）
   - 网页上传：进入仓库 → Add file → Upload files → 全选拖入
   - 或命令行：
     ```bash
     cd SuperClicker
     git init
     git add .
     git commit -m "first commit"
     git remote add origin https://github.com/<你的用户名>/SuperClicker.git
     git branch -M main
     git push -u origin main
     ```
3. push 后自动触发 `.github/workflows/build-apk.yml`
4. 进入仓库 → Actions 标签 → 点最新的 workflow run → 页面底部 Artifacts → 下载 `SuperClicker-debug-apk`
5. 解压得到 `app-debug.apk`，传到手机安装

> 修改代码后再次 push，会重新生成 APK。

### 方式二：Android Studio 本地编译

1. 用 Android Studio 打开本目录
2. 自动同步 Gradle（需联网下载依赖）
3. Build → Build Bundle(s)/APK(s) → Build APK(s)
4. 产物在 `app/build/outputs/apk/debug/app-debug.apk`

### 方式三：命令行（需本机装 JDK 17 + Android SDK）

```bash
gradle assembleDebug
```

## 安装与使用

1. 手机安装 `app-debug.apk`（需允许"未知来源"安装）
2. 打开 App → 点「检查并开启无障碍权限」→ 在设置里找到「超级点击器 Core」→ 开启
3. 添加步骤：
   - 单击 / 滑动：可点「拾取」按钮，进入全屏页点屏幕取坐标
   - 多点点击：输入 `300,500;600,700`
4. 设置循环次数（0 = 无限），点「开始」
5. 需要随时控制时点「悬浮窗」，授权后屏幕出现可拖动的控制台

## 权限说明

- **无障碍权限**：执行点击/滑动手势的核心，必需
- **悬浮窗权限**：仅悬浮控制台需要，不用可不授权
- **通知权限**：Android 13+ 悬浮服务前台通知，用于保活

本应用不联网、不收集任何数据。规则以 JSON 存在应用内部目录。

## 工程结构

```
app/src/main/java/com/example/superclicker/
├── model/Step.kt                    步骤与规则数据模型
├── service/
│   ├── ClickerAccessibilityService.kt   执行引擎（dispatchGesture）
│   └── FloatingControlService.kt         悬浮控制台
├── ui/
│   ├── MainActivity.kt                   主界面
│   ├── StepAdapter.kt                    步骤列表
│   ├── StepEditorDialog.kt               步骤编辑
│   └── CoordinatePickerActivity.kt       坐标拾取
└── util/RuleStore.kt                     规则 JSON 存取
```

## 合规

本项目为功能演示与学习用途，不含任何破解、绕过授权、图像识别内容。请仅在自己设备与合法场景下使用。
