# Contributing to Downloader Bot

Thanks for your interest in improving `Downloader Bot`!  
Any contribution - bug reports, ideas, or pull requests - is appreciated.

## Development Setup

1. Clone the repository:
    ```bash
    git clone https://github.com/furaizi/downloader-bot
    cd downloader-bot
    ```

2. Set up environment variables:
    ```bash
    cat <<EOF > .env
    TELEGRAM_BOT_TOKEN=<your_bot_token>
    TELEGRAM_BOT_USERNAME=<your_bot_username>
    EOF
    ```
You have to create a bot via [BotFather](https://t.me/BotFather) to get a token.

> `.env` file is included in `.gitignore`, so it won't be committed.

3. Choose how to run the application:  
  **Using IntelliJ IDEA (recommended):**
    - Start Redis:
      ```bash
      docker compose up -d redis
      ```
     - Run the application from the IDE.  

   **Using Docker Compose:**
     - Go to `compose.yml`. 
     - Comment out the `image` section in the `app` service and uncomment the `build` section.
     - Then run:
       ```bash
       docker compose up -d
       ```
       
> Why is a manual launch via IntelliJ IDEA recommended?

Because you can use:
- Hot reload via Spring Boot DevTools
- Debugger
- Hot swap

## How to Contribute

### Reporting Bugs

If you found a bug:
1. Make sure the issue doesn’t already exist.
2. Open a new issue using the Bug Report template.
3. Include:
  - The problematic link (e.g., TikTok, Instagram Reels)
  - Expected vs actual behavior
  - Any additional context

### Requesting Features

If you’d like a new feature (e.g., "add YouTube Shorts support"):
1. Open a Feature Request issue.
2. Clearly describe what the bot should do.

### Submitting Code
1. Fork the repository.
2. Create a branch:
    ```bash
    git checkout -b feature/your-feature-name
    ```
3. Follow Conventional Commits naming:
    ```bash
    feat(bot): add YouTube Shorts support
    fix(core): handle invalid TikTok short URLs
    chore(ci): update release workflow
    ```
4. Run checks before pushing:
    ```bash
    ./gradlew format
    ./gradlew build
    ```
5. Open a Pull Request to the `main` branch.  
Make sure the PR title also follows Conventional Commits - CI will validate it.

## Code Quality
The project uses automated tools:
- Detekt - static analysis
- Ktlint - formatting
- Kover - code coverage (temporarily off)
- JUnit 5, Mockk - testing
- Release-Please - automatic versioning and changelog
- semantic-pr - PR title validation

## Docker & Deployment
For testing production behavior locally:
```bash
docker compose up -d
```
Make sure you have your own `.env` file with the bot token.  
Production deployment runs automatically via GitHub Actions and AWS SSM when a release tag (v*) is created.

## Tips
- Keep PRs small and focused.
- If unsure - open a Draft PR and ask for feedback.
- Always describe _why_ a change is needed, not only _what_ it does.

## License
By contributing, you agree that your contributions will be licensed under the same [MIT License](../LICENSE) as the project.