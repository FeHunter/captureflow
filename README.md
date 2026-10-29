# CaptureFlow

CaptureFlow is an Android-first mobile app that turns screenshots and shared content into organized references and actionable checklists.

## Project status

Early scaffold. The initial focus is a reliable native share flow for URLs, text, and images. Screenshot detection will be explored as a complementary Android capability, with clear permission and platform limitations.

## Stack

- React Native + Expo + TypeScript
- Expo Router
- Android-first development, with room for iOS support
- Native Android modules where platform APIs require them
- EAS Build for cloud builds

## Getting started

Requirements: Node.js LTS, npm, and an Expo account for EAS cloud builds.

1. Clone this repository.
2. Install dependencies with `npm install`.
3. Run `npx expo install --check` to check SDK dependency compatibility.
4. Start the development server with `npx expo start`.
5. Run `npm run typecheck` and `npm run lint`.

For native Android integrations, use a development build rather than relying on Expo Go. Before the first EAS build, sign in with `eas login` and configure the project with `eas build:configure`. The EAS project ID will be added after the project is linked to an Expo account.

## Documentation

- [Engineering guidelines](AGENTS.md)
- [Architecture](docs/architecture.md)
- [Roadmap](docs/roadmap.md)
- [Captures feature](src/features/captures/README.md)
- [Checklists feature](src/features/checklists/README.md)

## Privacy principles

- Process content on-device where practical.
- Never silently upload captured images.
- Make source URLs and inferred metadata distinguishable.
- Ask for explicit consent before enabling cloud AI or external integrations.

## License

To be decided before public release.
