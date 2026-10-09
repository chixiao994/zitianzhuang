# 字体安装器 (FontInstaller)

一个安卓字体预览 / 安装小工具。

## 功能

- **打开**：通过系统文件选择器选择 `.ttf` / `.otf` / `.ttc` 字体文件，校验字体头后复制到应用私有目录，立即生效。
- **测试**：弹出测试面板，用当前字体显示不同字号、粗体 / 斜体、中英文标点等效果。
- **卸载**：删除已安装字体，恢复系统默认字体。
- **预览**：中间编辑区可以随便打字，自动使用新安装的字体渲染。
- 应用重启后会自动恢复上次安装的字体。

## 构建

推送到 `main` 或 `master` 分支后，进入仓库的 **Actions** 页面，
等待 `Build APK` 工作流跑完，在该次运行页面底部的 **Artifacts** 中
下载 `FontInstaller-debug`，解压得到 `FontInstaller-debug.apk`，传到手机上安装即可。

也可以在 Actions 页面点击 **Run workflow** 手动触发构建。

## 说明

本应用把字体安装在自己的私有目录（`/data/data/包名/files/fonts`）中，
并在应用内预览，**不需要 root**，但也不会替换系统全局字体。
如需替换系统字体，需要 root 权限或 Magisk 模块等方案。

## 环境

- minSdk 21（Android 5.0）
- targetSdk 34
- AGP 8.5.2 / Gradle 8.7 / JDK 17
