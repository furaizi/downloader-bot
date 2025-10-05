# Downloader Bot
The official repository for [`Zavantazhnyk (Завантажник) bot`](https://t.me/zavantazhnyk_bot) - Telegram bot for downloading media from various sources.

## Supported Sources
- TikTok
- Instagram Reels

## Requirements
No dependencies required on the host system.  
You only need `Docker` and `Docker Compose`.

## Used Technologies
- Kotlin v1.9.23
- Spring Boot v3.5.4
- Redis
- ffmpeg
- yt-dlp
- gallery-dl
- instaloader
- Docker Compose

## Quick Start
1. Clone the repository:
    ```bash
    git clone https://github.com/furaizi/downloader-bot
    cd downloader-bot
    ```

2. Set up environment variables:
    ```bash
    echo "TELEGRAM_BOT_TOKEN=<your_bot_token>" > .env
    ```

3. Start up the application:
    ```bash
    docker compose up -d
    ```

## How To Use
- Private chat:
  1. Start a conversation with the bot: [@zavantazhnyk_bot](https://t.me/zavantazhnyk_bot)
  2. DM your bot a supported link. The bot replies with the downloaded media.

- Group chat:
  1. Add the bot to your group.
  2. Grant it admin rights. This is necessary for the bot to be able to read messages and send media.
  3. Send a supported link in the group. The bot replies with the downloaded media.

## Configuration Reference
Only the token is required to start. Other settings are optional with sensible defaults.  
Look up [`application.yml`](./src/main/resources/application.yml) for more details.

## Deployment
Each release tag (`v*`) triggers automatic deployment to an AWS EC2 instance via GitHub Actions and AWS SSM.

Builds and tests are verified in CI before release:
- Linting: Detekt, Ktlint
- Tests: JUnit (unit + integration)
- Code coverage: Kover (temporary off)
- Pull Request title validation: semantic-pr
- Release automation: Release-Please

## Metrics
Prometheus metrics are exposed when running with the `prod` profile.  
They are available at `localhost:8081/actuator/prometheus`:
    ```bash
    curl http://localhost:8081/actuator/prometheus | head
    ```

## Contributing
See [CONTRIBUTING.md](./.github/CONTRIBUTING.md) for contribution guidelines.
