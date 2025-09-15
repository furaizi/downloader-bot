# Downloader Bot
A Telegram bot that downloads media from links you send it and replies with the files.
It uses yt-dlp for video extraction and can optionally use gallery-dl for image albums.

## Supported Sources
- TikTok (videos)
- YouTube Shorts
- More sources will be added over time.

## Requirements
You don't need to have any dependencies installed on your host.  
The only thing you need is `Docker` and `Docker Compose`.

## Tech Stack
- Kotlin v1.9.25
- Spring Boot v3.5.4
- Redis
- yt-dlp
- ffmpeg
- (Optional) gallery-dl

## Quick Start
1. Set up environment variables:
```bash
export TELEGRAM_BOT_TOKEN=
```
2. Start up the application:
```bash
docker compose up -d
```

## Configuration Reference
Only the token is required to start. Other settings are optional with sensible defaults.
Look up `src/main/resources/application.yml` for more details.

## How To Use
- DM your bot a supported link. The bot replies with the downloaded media.
- Examples:
  - TikTok video: `https://www.tiktok.com/@user/video/1234567890`
  - TikTok video (short form): `https://www.tiktok.com/video/1234567890`
  - YouTube Shorts: `https://www.youtube.com/shorts/abcdefghijk`

Tip: The default allowlist only permits the patterns above. You can extend `downloader.sources.allow` via application config if you build a custom image.

## Contributing
Small PRs and issue reports are welcome. Please keep changes focused and include a brief rationale and testing notes.

## License
This repository’s license governs usage. If a LICENSE file is present, its terms apply; otherwise, all rights are reserved by the author.
