package local.continuewatching;

/** One row of the list, from either source. */
final class Item {
    final String pkg;
    final String title;
    final long time;
    final long positionMs;
    final long durationMs;
    /** The app's own link to the title, from Watch Next; null for a now-playing record. */
    final String intentUri;

    Item(String pkg, String title, long time, long positionMs, long durationMs, String intentUri) {
        this.pkg = pkg;
        this.title = title;
        this.time = time;
        this.positionMs = positionMs;
        this.durationMs = durationMs;
        this.intentUri = intentUri;
    }

    /** Apps never recorded or listed, from either source: `neverListed` in local.properties. */
    private static final java.util.Set<String> NEVER_LISTED = new java.util.HashSet<>();

    static {
        for (String pkg : BuildConfig.NEVER_LISTED.split(",")) {
            if (!pkg.trim().isEmpty()) NEVER_LISTED.add(pkg.trim());
        }
    }

    static boolean neverListed(String pkg) {
        return pkg != null && NEVER_LISTED.contains(pkg);
    }

    String key() {
        return pkg + "|" + title.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
