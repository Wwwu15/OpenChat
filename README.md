# Open Chat

Open Chat 是一个基于 Kotlin 与 Jetpack Compose 开发的安卓 AI 助手应用。

它提供面向 OpenAI 兼容接口的移动端聊天体验，支持多 API 配置管理、本地聊天记录、附件发送、明暗主题切换等能力。

## 功能简介

- OpenAI 兼容 API 支持
  - 自定义 Base URL 与 API Key
  - 从已配置 API 拉取模型列表
  - 当接口未返回上下文信息时，可手动填写上下文大小
- 多 API 管理
  - 保存多个 API 配置
  - 支持启用、编辑、删除配置
  - 保存后返回 API 管理界面
- 聊天能力
  - 流式输出回复
  - 支持中止生成
  - 新建会话
  - 本地持久化聊天记录
  - 助手消息支持 Markdown 渲染
  - 历史会话页可全局开关 Markdown 渲染
  - 历史会话右滑删除
  - 一键清空全部历史
- 附件支持
  - 拍照
  - 从相册选择图片
  - 通过文件选择器选择文本、图片、PDF、DOC、DOCX
  - 当请求中包含 PDF / Word 附件时，优先走 `Responses API`
- 界面与交互
  - 明色 / 暗色模式切换
  - 自定义 Compose UI 组件
  - 主要页面过渡动画
  - 保存 / 测试结果以浮动文字提示展示

## 技术栈

- 开发语言：Kotlin
- UI：Jetpack Compose + Material 3
- 架构：ViewModel + Repository
- 导航：Navigation Compose
- 本地存储
  - Room：聊天记录与附件信息
  - DataStore Preferences：应用设置与 API 配置元数据
  - EncryptedSharedPreferences / Security Crypto：API Key 安全存储
- 网络
  - OkHttp
  - Moshi + KSP Codegen
- Markdown 渲染
  - mikepenz/multiplatform-markdown-renderer-m3
- 构建
  - Gradle Kotlin DSL
  - Android Gradle Plugin
  - KSP

## 项目结构

- `app/src/main/java/com/example/aiassistant/data`
  - 数据库、API 客户端、偏好设置、仓库层、安全存储
- `app/src/main/java/com/example/aiassistant/domain`
  - 附件处理、上下文相关逻辑
- `app/src/main/java/com/example/aiassistant/ui`
  - 页面、通用组件、主题、工厂类
- `app/src/main/java/com/example/aiassistant/navigation`
  - 路由定义与导航宿主
- `app/src/test/java/com/example/aiassistant`
  - 仓库、解析、上下文逻辑、ViewModel 等单元测试

## 环境要求

- Android Studio / Android IDE
- Android SDK
- JDK 17
- minSdk 26
- compileSdk 35
- targetSdk 35

## 构建与运行

### 构建 Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 输出位置：

`app/build/outputs/apk/debug/app-debug.apk`

### 运行单元测试

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

### 安装到模拟器或真机

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## 支持的接口形态

本应用面向 OpenAI 兼容接口设计，典型接口包括：

- `GET /v1/models`
- `POST /v1/chat/completions`
- `POST /v1/responses`（用于文档 / 文件类请求）

部分中转接口或代理服务不会返回模型上下文信息。遇到这种情况时，界面会显示“上下文未知”，并允许在 API 配置中手动填写上下文大小。

## 当前行为说明

- 模型上下文展示仅显示接口真实返回值，或用户手动填写的上下文值。
- 当前聊天请求默认会把完整会话历史发送给接口端，不在本地做截断。
- Markdown 渲染默认开启，仅对助手消息生效；用户消息仍按普通文本显示。
- 可在历史会话页右上角通过开关控制 Markdown 渲染是否启用，切换结果会以浮动提示显示。
- 如果服务端不支持当前所选模型，或接口格式与 OpenAI 兼容规范存在差异，请在 API 配置中调整后再测试。

## 当前版本

- versionName: `1.27`
- versionCode: `28`
