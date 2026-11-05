# Security

Please report a vulnerability privately through
[GitHub's private vulnerability reporting](../../security/advisories/new), not in a public issue.

The app holds two grants worth knowing about: `READ_TV_LISTINGS` and notification access, which it uses
only to read media sessions. It has no network permission. A report that it can leak data, or that
another app can use it to open something it shouldn't, is in scope.
