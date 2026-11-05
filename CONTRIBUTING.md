# Contributing

Thanks for looking. This is a small app built for one TV, so the bar for changes is simple:

- **Reports from other TVs are the most useful thing.** Open an issue with the TV model, Android
  version, and which apps showed up (or didn't).
- **Keep it offline.** Pull requests that add the `INTERNET` permission, a boot receiver or an
  accessibility service won't be merged; fork for those.
- Build with `./gradlew assembleRelease` and test on a real TV before opening a pull request. Say which
  TV in the description.
