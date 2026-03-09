# Music App (Android)







基于 Jetpack Compose 的音乐播放器 Android 应用，后端服务运行在本地 `http://localhost:8080`，API 前缀为 `/api/app/music/**`。







## 功能特性







### 核心功能



- 热门歌曲推荐与播放



- 歌曲搜索



- 专辑浏览与详情



- 歌手浏览与详情



- 歌词同步滚动与偏移校准







### 播放器



- 全局底部迷你播放器



- 完整播放页（专辑封面、进度条、歌词高亮）



- 播放模式切换（顺序 / 单曲循环 / 随机）



- 上一首 / 下一首切换







### 个人中心



- 最近播放记录



- 收藏歌曲管理



- 歌手列表







## API 接口







后端服务地址：`http://localhost:8080`







### 音乐内容







| 接口 | 说明 |



|------|------|



| `GET /api/app/music/song/page` | 歌曲分页 |



| `GET /api/app/music/song/hot` | 热门歌曲 |



| `GET /api/app/music/song/by-ids` | 批量获取歌曲 |



| `GET /api/app/music/album/page` | 专辑分页 |



| `GET /api/app/music/album/{albumId}` | 专辑详情 |



| `GET /api/app/music/artist/page` | 歌手分页 |



| `GET /api/app/music/artist/{artistId}` | 歌手详情 |



| `GET /api/app/music/lyrics/song/{songId}` | 获取歌词 |







### 播放记录与收藏







| 接口 | 说明 |



|------|------|



| `POST /api/app/music/player/history` | 记录播放历史 |



| `GET /api/app/music/player/history/recent` | 获取最近播放 |



| `DELETE /api/app/music/player/history` | 清空播放历史 |



| `POST /api/app/music/player/favorite` | 添加收藏 |



| `DELETE /api/app/music/player/favorite` | 取消收藏 |



| `GET /api/app/music/player/favorite/check` | 检查收藏状态 |



| `GET /api/app/music/player/favorite/songs` | 获取收藏歌曲 |







### 歌单管理







| 接口 | 说明 |



|------|------|



| `GET /api/app/music/playlist/user` | 获取当前用户的歌单列表 |



| `GET /api/app/music/playlist/public` | 获取公开歌单列表 |



| `GET /api/app/music/playlist/{playlistId}` | 获取歌单详情 |



| `POST /api/app/music/playlist` | 创建歌单 |



| `PUT /api/app/music/playlist` | 更新歌单 |



| `DELETE /api/app/music/playlist/{playlistId}` | 删除歌单 |



| `GET /api/app/music/playlist/{playlistId}/songs` | 获取歌单中的歌曲ID列表 |



| `POST /api/app/music/playlist/{playlistId}/song/{songId}` | 向歌单添加歌曲 |



| `DELETE /api/app/music/playlist/{playlistId}/song/{songId}` | 从歌单移除歌曲 |



| `POST /api/app/music/playlist/{playlistId}/play` | 增加歌单播放次数 |







### 歌曲评论







| 接口 | 说明 |



|------|------|



| `POST /api/app/music/comment` | 添加评论 |



| `DELETE /api/app/music/comment/{commentId}` | 删除评论 |



| `GET /api/app/music/comment/song/{songId}` | 获取歌曲评论列表（分页） |



| `POST /api/app/music/comment/{commentId}/like` | 点赞评论 |



| `DELETE /api/app/music/comment/{commentId}/like` | 取消点赞评论 |







### 歌词分享







| 接口 | 说明 |



|------|------|



| `POST /api/app/music/lyrics/share` | 创建歌词分享 |



| `DELETE /api/app/music/lyrics/share/{shareId}` | 删除歌词分享 |



| `GET /api/app/music/lyrics/share/user` | 获取当前用户的歌词分享列表（分页） |



| `GET /api/app/music/lyrics/share/lyrics/{lyricsId}` | 获取歌词的分享列表（分页） |



| `GET /api/app/music/lyrics/share/{shareId}` | 获取分享详情 |







## 项目结构







```



app/src/main/java/com/music/app/



├── ui/



│   ├── navigation/     # 导航管理



│   ├── screen/         # 页面（Home/Discover/Search/Mine/Player 等）



│   └── theme/          # 主题配置



├── data/



│   ├── remote/         # Retrofit 接口与数据模型



│   └── repository/     # 数据仓库层



├── player/             # ExoPlayer 播放器控制



└── MainActivity.kt     # 入口



```







## 快速开始







### 1. 启动后端服务







在 `music-server/music-server-integrated` 目录下运行：







```bash



mvn spring-boot:run



```







后端将启动在 `http://localhost:8080`







### 2. 配置网络地址







编辑 `app/src/main/java/com/music/app/data/remote/NetworkModule.kt`：







```kotlin



const val BASE_URL = "http://10.0.2.2:8080/"



```







| 场景 | 配置 |



|------|------|



| Android 模拟器 | `http://10.0.2.2:8080/` |



| 真机调试 | `http://<电脑局域网IP>:8080/` |







### 3. 运行应用







1. 使用 Android Studio 打开 `music-app` 目录



2. 等待 Gradle Sync 完成



3. 点击 Run 运行应用







## 技术栈







- **UI**: Jetpack Compose、Material 3



- **架构**: MVVM、StateFlow



- **网络**: Retrofit、OkHttp



- **播放器**: ExoPlayer (Media3)



- **依赖注入**: Hilt



- **构建**: Gradle Kotlin DSL（已配置阿里云镜像加速）







## 后续规划







- [x] 用户登录注册（已完成）



- [x] 歌单创建与管理（已完成）



- [x] 歌曲评论（已完成）



- [x] 歌词分享（已完成）



- [x] 后台播放与通知栏控制（已完成）







## 规划执行检查与完成情况







### 1) 歌词分享







- 接口层：已接入创建/删除/查询相关 API。



- 仓库层：已封装歌词分享创建、删除、列表与详情能力。



- 状态层：`MusicViewModel` 已实现加载、发布、删除与状态管理。



- 页面层：播放页支持歌词分享；我的页面新增歌词分享记录展示。







### 2) 后台播放与通知栏控制







- 服务层：已实现 `MusicPlaybackService`（基于 `MediaSessionService`）并创建通知渠道。



- 配置层：`AndroidManifest.xml` 已声明前台服务权限与 `mediaPlayback` 类型服务。



- 播放联动：播放时自动拉起服务，生命周期结束时释放并停止服务。



- 控制能力：支持系统通知栏/会话侧播放控制（播放、暂停、切歌）。



## 新的后续规划（这只是一个私人的音乐分享平台）

### 1) 小而美的核心体验
- [ ] 优化首页信息密度，只保留「我常听」「最近播放」「私藏歌单」三块核心内容
- [ ] 增加「一键继续播放」入口（上次播放歌曲 + 进度恢复）
- [ ] 提升搜索命中质量（支持拼音、别名、大小写宽松匹配）


2.添加反馈功能，发现歌词不对、歌词有错，歌词校准不对、没有想要的歌曲，以及其他意见，都可以反馈
特别地：如果搜歌没搜到想要的，那么直接展示反馈按钮
