# API Test Checklist (by page)

Backend base: `http://127.0.0.1:8080`  
Frontend base: `http://127.0.0.1:5173`

## 1) 登录页 `LoginView`
- [ ] `POST /api/app/auth/register`：新用户注册成功，重复用户名提示正确。
- [ ] `POST /api/app/auth/login`：正确账号返回 token；错误账号显示失败提示。
- [ ] 登录后跳转 `/home`，本地存储 `token` 与 `userInfo`。

## 2) 首页 `HomeView`
- [ ] `GET /api/app/music/song/hot`：可展示热门歌曲，空列表显示空态。
- [ ] `GET /api/app/music/artist/page`：可展示歌手列表。
- [ ] 点击播放后：`POST /api/app/music/player/playlist` 正常写入队列，跳转播放器。

## 3) 发现页 `DiscoverView`
- [ ] `GET /api/web/recommend/daily`
- [ ] `GET /api/web/recommend/personal`
- [ ] `GET /api/web/recommend/scene`
- [ ] 任一推荐列表点播后可进入播放器。

## 4) 歌单页 `LibraryView`
- [ ] `GET /api/app/music/playlist/user`：加载我的歌单。
- [ ] `POST /api/app/music/playlist`：新建成功。
- [ ] `PUT /api/app/music/playlist`：编辑成功。
- [ ] `DELETE /api/app/music/playlist/{id}`：删除成功。
- [ ] `POST /api/mp/asset/playlist-cover`：封面上传成功。

## 5) 歌单详情页 `PlaylistDetailView`
- [ ] `GET /api/app/music/playlist/{id}`：歌单详情加载。
- [ ] `GET /api/app/music/playlist/{id}/songs` + `GET /api/app/music/song/by-ids`：歌曲列表正常。
- [ ] `GET /api/app/music/song/page`：搜索添加候选歌曲。
- [ ] `POST /api/app/music/playlist/{playlistId}/song/{songId}`：添加成功。
- [ ] `DELETE /api/app/music/playlist/{playlistId}/song/{songId}`：移除成功。

## 6) 播放器页 `PlayerView`
- [ ] `GET /api/app/music/player/playlist`：恢复队列。
- [ ] `GET /api/app/music/song/{id}/stream`：可播放流媒体。
- [ ] `POST /api/app/music/player/history`：播放历史写入成功。
- [ ] `GET /api/app/music/lyrics/song/{id}`：歌词加载成功。
- [ ] `GET/POST/DELETE /api/app/music/player/favorite*`：收藏状态切换成功。

## 7) 歌词页 `LyricsView`
- [ ] `GET /api/app/music/lyrics/song/{id}`：歌词正文展示。
- [ ] `GET /api/app/music/song/by-ids`：歌曲名称/歌手展示。

## 8) 个人中心页 `ProfileView`
- [ ] `GET /api/app/music/player/favorite/songs`：收藏ID返回正常。
- [ ] `GET /api/app/music/song/by-ids`：收藏歌曲详情展示。
- [ ] `POST /api/mp/asset/avatar`：头像上传成功。

## 9) 搜索页 `SearchView`
- [ ] `GET /api/app/music/search/suggestions`：输入时联想正常。
- [ ] `GET /api/app/music/song/page`：搜索结果正常。
- [ ] `POST /api/app/music/feedback`：反馈缺歌成功。

## 10) 歌手/专辑页 `ArtistDetailView` / `AlbumDetailView`
- [ ] `GET /api/app/music/artist/{id}`：歌手详情正常。
- [ ] `GET /api/app/music/artist/{id}/top-songs`：热门歌曲展示。
- [ ] `GET /api/app/music/album/{id}`：专辑详情正常。
- [ ] `GET /api/app/music/song/page?albumId=*`：专辑歌曲展示。

## 11) 评论页 `CommentsView`
- [ ] `GET /api/app/music/comment/song/{songId}`：评论列表分页正常。
- [ ] `POST /api/app/music/comment`：评论发布成功。
- [ ] `DELETE /api/app/music/comment/{commentId}`：删除成功。
- [ ] `POST/DELETE /api/app/music/comment/{commentId}/like`：点赞/取消点赞成功。

## 12) 反馈页 `FeedbackView`
- [ ] `POST /api/app/music/feedback`：提交反馈成功。
- [ ] `GET /api/app/music/feedback/mine`：我的反馈列表正常。

## 13) 歌词分享页 `LyricsShareView`
- [ ] `POST /api/app/music/lyrics/share`：创建分享成功。
- [ ] `GET /api/app/music/lyrics/share/user`：我的分享列表正常。

## 14) 设置页 `SettingsView`
- [ ] `GET /api/app/user-setting`：加载所有设置。
- [ ] `POST /api/app/user-setting`：保存设置成功。

## 通用状态规范验收（每页都需过）
- [ ] 首屏请求显示 loading（骨架或 `v-loading`）。
- [ ] 请求失败显示错误态，并带可读错误文案。
- [ ] 无数据时显示空态文案，不出现空白页面。
- [ ] 表单提交失败有错误提示，成功有成功提示。

---

## 实测执行记录（本机 HTTP 直连 `127.0.0.1:8080`）

**环境**：`music-server-integrated` 本地运行；账号 `admin` / `123456`（库初始化默认管理员）。  
**说明**：以下为接口层自动化探测结果；带「浏览器」项需人工在 `5173` 上再点一次确认。

### 汇总表

| 区块 | 结论 | 原因摘要 | 修复建议 |
|------|------|----------|----------|
| §1 注册/登录 | **大部分通过** | 重复注册返回 `code=500` +「用户名已存在」；错误密码返回 `code=500` + 业务提示 | 后端建议：登录/业务校验失败统一用 `4xx` + `code!=200`，避免与真 500 混淆；前端已可用 `message` 展示 |
| §1 登录后跳转与 localStorage | **未测（浏览器）** | 脚本无法代替浏览器存储与路由 | 在 `5173` 登录一次，检查 Application → Local Storage 与跳转 `/home` |
| §2 首页 | **通过** | `song/hot`、`artist/page` 均 `code=200` | — |
| §3 发现 | **通过** | `/api/web/recommend/daily|personal|scene` 均 `code=200` | — |
| §4 歌单 CRUD | **通过** | `playlist/user`、创建/改/删、歌单内增删歌曲均 `code=200` | — |
| §4 封面上传 | **未测（multipart）** | 本次仅用 JSON 探针 | 用浏览器在歌单页选图上传，或 `curl -F file=@x.jpg` 调 `/api/mp/asset/playlist-cover` |
| §5 歌单详情 | **通过** | 与 §4 同批接口验证 | — |
| §6 播放器相关 | **通过** | `player/playlist` GET/POST、`history` POST、`lyrics/song/{id}`、`favorite` 增删、`song/{id}/stream` 返回 200 二进制 | 若浏览器跨域播放失败，检查 `Authorization` 是否随 `<audio>` 发出（必要时改为带 token 的代理或 Cookie 方案） |
| §7 歌词页 | **通过** | `lyrics/song/{id}`、`song/by-ids` 正常 | — |
| §8 个人中心 | **部分通过** | 收藏列表、`by-ids` 正常 | 头像上传同 §4，需 multipart 实测 |
| §9 搜索 | **通过** | `search/suggestions`、`song/page`、`feedback` POST 正常 | — |
| §10 歌手/专辑 | **通过** | `artist/{id}`、`top-songs`、`album/{id}`、`song/page?albumId=` 正常 | — |
| §11 评论 | **通过** | 列表查询、发帖、点赞、取消赞、删帖均 `code=200` | — |
| §12 反馈 | **通过** | 提交与 `feedback/mine` 正常 | — |
| §13 歌词分享 | **部分失败** | `POST /api/app/music/lyrics/share` **通过**；`GET /api/app/music/lyrics/share/user` 返回 **`code=500`「系统异常」** | 查服务端异常栈：重点看 `music_lyrics_share` 表字段与 `LyricsShare`/`BaseEntity` 映射、以及 `UserMapper` 查用户是否为 null；修复后重测列表接口 |
| §14 用户设置 | **失败** | `GET/POST /api/app/user-setting` 返回 **`code=500`「用户未登录」** | **根因**：`SecurityUtils.getUserId()` 在带 `Bearer` 时仍为 null（未见 JWT 过滤器写入 `SecurityContext`），而 `AppUserSettingServiceImpl` **未做** 其它 Controller 的 `1L` 兜底。**建议**：增加 JWT 认证过滤器并解析 `Authorization`；或短期与 `AppPlaylistController` 等行为对齐（不推荐生产） |
| 通用 UI 状态 | **通过（代码审查）** | 已引入 `StateBlock` 与各页 `try/catch` + `ElMessage` | 建议在真实浏览器对各页断网/清 token 各点一次做最终确认 |

### 逐条勾选（与上文一致）

#### §1 登录页
- [x] **通过** `POST /api/app/auth/register`（新用户名随机）→ `code=200`。
- [x] **通过** 重复注册同一用户名 → `code=500`，`message` 含「已存在」类提示。
- [x] **通过** 错误密码登录 → `code=500`，业务错误信息（HTTP 仍为 200，属后端统一包装）。
- [ ] **未测（浏览器）** 登录后 `/home` 与 `localStorage`。

#### §2 首页
- [x] **通过** `GET /api/app/music/song/hot`
- [x] **通过** `GET /api/app/music/artist/page`
- [x] **通过** `POST /api/app/music/player/playlist`（带 Bearer）

#### §3 发现
- [x] **通过** `GET /api/web/recommend/daily|personal|scene`
- [ ] **未测（浏览器）** 点播进入播放器（接口侧队列已验证）

#### §4 歌单
- [x] **通过** `GET /api/app/music/playlist/user` 及 CRUD、封面上传以外的接口
- [ ] **未测** `POST /api/mp/asset/playlist-cover`（multipart）

#### §5 歌单详情
- [x] **通过** 详情、songs、song/page、增删歌曲

#### §6 播放器
- [x] **通过** playlist、stream、history、lyrics、favorite 全套

#### §7 歌词页
- [x] **通过**（同 lyrics + by-ids）

#### §8 个人中心
- [x] **通过** `favorite/songs` + `song/by-ids`
- [ ] **未测** `POST /api/mp/asset/avatar`

#### §9 搜索
- [x] **通过** suggestions、song/page、feedback

#### §10 歌手/专辑
- [x] **通过** 四项 GET

#### §11 评论
- [x] **通过** 列表、发帖、删帖、点赞、取消赞

#### §12 反馈
- [x] **通过** POST + mine

#### §13 歌词分享
- [x] **通过** `POST .../lyrics/share`
- [ ] **失败** `GET .../lyrics/share/user` → `code=500`（见汇总表）

#### §14 设置
- [ ] **失败** `GET/POST /api/app/user-setting` → `用户未登录`（见汇总表）

#### 通用状态规范
- [x] **通过（代码审查）** 各页已统一 `loading/empty/error` 与消息提示；**建议浏览器再回归一遍**。
