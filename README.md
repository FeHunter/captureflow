# CaptureFlow

CaptureFlow is an Android-first mobile app that turns screenshots and shared content into organized references and actionable checklists.

## Project status

Early scaffold. The initial focus is a reliable native share flow for URLs, text, and images. Screenshot detection will be explored as a complementary Android capability, with clear permission and platform limitations.

## Planned stack

- React Native + Expo + TypeScript
- Expo Router
- Android-first development, with room for iOS support
- Native Android modules where platform APIs require them
- EAS Build for cloud builds

## Getting started

The initial application scaffold and setup instructions are being prepared. See [docs/architecture.md](docs/architecture.md) and [docs/roadmap.md](docs/roadmap.md).

## Privacy principles

- Process content on-device where practical.
- Never silently upload captured images.
- Make source URLs and inferred metadata distinguishable.
- Ask for explicit consent before enabling cloud AI or external integrations.

## License

To be decided before public release.
