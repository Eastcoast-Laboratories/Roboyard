# Contributing to Roboyard

Thank you for your interest in contributing to Roboyard! This document provides guidelines and information for contributors.

## Development Setup

### Prerequisites
- Android Studio (latest version recommended)
- JDK 11 or higher
- Android SDK (minimum API level 21)
- Git

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/rubo77/Roboyard.git
   ```
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Build the project

## Project Structure

### Main Components
- `app/src/main/java/roboyard/eclabs/` - Main source code
- `app/src/main/res/` - Resources (layouts, drawables, etc.)
- `app/src/main/assets/` - Game assets
- `docs/` - Project documentation
- `fastlane/` - Fastlane configuration for automated deployment (F-Droid)

### Key Files
- `GridGameScreen.java` - Main game screen implementation
- `GameManager.java` - Game state management
- `Constants.java` - Game constants and screen IDs
- `MainActivity.java` - Main activity


## Coding Guidelines

### Code Style
- Follow Java coding conventions
- Use meaningful variable and method names
- Add comments for complex logic
- Keep methods focused and concise

### Documentation
- Document public methods and classes
- Update relevant documentation when making changes
- Add comments explaining complex algorithms

### Testing
- Test your changes thoroughly
- Ensure no regressions in existing functionality
- Test on different Android versions and screen sizes

## Pull Request Process

1. Create a feature branch from `master`
2. Make your changes
3. Update documentation if needed
4. Test your changes
5. Submit a pull request
6. Wait for review and address any feedback

## Bug Reports and Feature Requests

- Use GitHub Issues for bug reports and feature requests
- Provide detailed information for bug reports:
  - Steps to reproduce
  - Expected behavior
  - Actual behavior
  - Android version and device
  - Screenshots if applicable

## Game Mechanics

When contributing new features or modifications to game mechanics, consider:

### Level Design
- Levels should be solvable
- Difficulty should match the intended category
- Minimum moves should be reasonable

### UI/UX
- Follow Android Material Design guidelines
- Maintain consistent look and feel
- Consider accessibility

### Performance
- Optimize resource usage
- Minimize memory allocations
- Consider battery impact

## Release Process

1. Update `versionCode` and `versionName` in `app/build.gradle`
2. Update changelog in
    - `CHANGELOG.md`
    - `CHANGELOG_de.md`
    - `fastlane/metadata/android/de/changelogs/<versionName>.txt`
    - `fastlane/metadata/android/en-US/changelogs/<versionName>.txt`
3. Test thoroughly
4. Create an annotated tag (e.g. `v17.2`)
5. Build a Signed APK on the commit of the tag
6. store the APK in `/download/` (e.g. as `download/Roboyard_v17.2.apk`)
7. create another commit with the APK and push it
8. Submit the APK to Play Store
9. Tag release on GitHub

## Questions and Support

- Use GitHub Discussions for questions
- Check existing issues before creating new ones
- In the future: Join our community channels for real-time discussion

## License

By contributing to Roboyard, you agree that your contributions will be licensed under the project's license.
