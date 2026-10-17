# 画音 MusicArt 🎵

一个**全部手绘、极致圆角**的原生 Android 音乐播放器。
不使用任何系统原生控件（Button/TextView/ListView/Dialog 等一概不用），
所有界面元素均为自定义 View 在 `onDraw()` 中用 Canvas 手绘制，圆角 + 渐变 + 柔和阴影。

## 特性
- 自绘开屏动画（旋转唱片 logo + 呼吸光晕，约 2.6s）
- 搜索页：自绘圆角搜索胶囊、热门搜索标签、圆角歌曲卡片列表
- 播放页：旋转大封面、可拖拽进度条、播放/上下曲、逐行自动滚动歌词
- 自绘 BottomSheet 播放列表弹窗（无系统 Dialog）
- 首页编辑精选（打包插画封面）+ 在线推荐
- 图片加载：自写 LruCache + 网络下载；网络：HttpURLConnection；播放：MediaPlayer

## 接口
- 搜索：`https://music.163.com/api/search/get?s=..&type=1`
- 播放直链 / 歌词：`https://api.injahow.cn/meting/?server=netease&type=url|lrc&id=..`
- 在线推荐：`https://api.mnchen.cn/`（失败时用本地精选兜底）

## 构建
- minSdk 21（现代 AGP/SDK 最低可行；代码兼容老式 MediaPlayer/HttpURLConnection 写法）
- targetSdk 34，arm64-v8a
- `./gradlew assembleDebug`

## 安装
见 `releases/MusicArt-arm64.apk`（约 15MB，含手绘插画资源）。
