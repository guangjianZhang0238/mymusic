# Music Web Migration Matrix

| Domain | Capability | Source | Web Status | API |
|---|---|---|---|---|
| Auth | Username/password login & register | mini/app | Done | `/api/app/auth/login`, `/api/app/auth/register` |
| Auth | Current user info | app | Done | `/api/app/auth/me` |
| Home | Hot songs | mini | Done | `/api/app/music/song/hot` |
| Home | Artist list | mini | Done | `/api/app/music/artist/page` |
| Discover | Daily/personal/scene recommend | mini | Done | `/api/web/recommend/*` |
| Player | Stream play, queue sync, prev/next | mini/app | Done | `/api/app/music/song/{id}/stream`, `/api/app/music/player/playlist` |
| Player | Play history | mini/app | Done | `/api/app/music/player/history` |
| Player | Favorites | mini/app | Done | `/api/app/music/player/favorite*` |
| Lyrics | Lyrics page + in-player lines | mini/app | Done | `/api/app/music/lyrics/song/{id}` |
| Library | Playlist CRUD | mini/app | Done | `/api/app/music/playlist*` |
| Library | Playlist add/remove song | mini/app | Done | `/api/app/music/playlist/{id}/song/{songId}` |
| Profile | Favorite songs list | mini | Done | `/api/app/music/player/favorite/songs` |
| Profile | Avatar upload | mini | Done | `/api/mp/asset/avatar` |
| Search | Song search & suggestions | app | Done | `/api/app/music/song/page`, `/api/app/music/search/suggestions` |
| Album | Album detail + songs | app | Done | `/api/app/music/album/{id}`, `/api/app/music/song/page` |
| Artist | Artist detail/top songs | mini/app | Done | `/api/app/music/artist/{id}`, `/api/app/music/artist/{id}/top-songs` |
| Social | Comment CRUD + like/unlike | app | Done | `/api/app/music/comment*` |
| Social | Lyrics share | app | Done | `/api/app/music/lyrics/share*` |
| Feedback | Submit and list my feedback | app | Done | `/api/app/music/feedback`, `/api/app/music/feedback/mine` |
| Settings | User settings list/save | app | Done | `/api/app/user-setting*` |

## Verification checklist

- [x] Route map includes core pages and advanced pages.
- [x] API client covers app/miniprogram feature surfaces used by web.
- [x] Added web recommend facade under backend `com.music.web`.
- [x] Player queue persistence and history/favorite endpoints integrated.
- [ ] Run frontend `npm install && npm run build` locally (blocked: `npm` not found in current shell PATH).
- [ ] Run end-to-end manual regression against running backend on `8080`.
