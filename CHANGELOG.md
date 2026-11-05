# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project uses
[Semantic Versioning](https://semver.org/).

## [0.1.0] - 2026-11-05

### Added

- One list from two sources: the Watch Next rows other apps publish (last 90 days, unfinished titles
  only) and a now-playing history recorded from media sessions, for apps such as YouTube that
  publish no rows.
- Opening a row uses the app's own link, restricted to that app; YouTube titles open as a YouTube
  search; anything else opens the app.
- `neverListed` in `local.properties` keeps chosen apps out of the list and the history.
- A CleanYourTV module card.

[0.1.0]: https://github.com/namaserajesh/continue-watching-tv/releases/tag/v0.1.0
