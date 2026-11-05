package local.continuewatching;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Joins the Watch Next rows other apps publish with the now-playing records. */
final class WatchNext {
    private static final long CUTOFF_MS = 90L * 24 * 60 * 60 * 1000;
    private static final double FINISHED = 0.95;
    private static final int TYPE_CONTINUE = 0;
    private static final int TYPE_NEXT = 1;
    private static final Uri URI = Uri.parse("content://android.media.tv/watch_next_program");
    private static final String[] COLUMNS = {
            "package_name", "title", "watch_next_type", "last_engagement_time_utc_millis",
            "last_playback_position_millis", "duration_millis", "intent_uri",
            "season_display_number", "episode_display_number", "episode_title"
    };

    private WatchNext() {
    }

    /** Without ACCESS_ALL_EPG_DATA a selection is refused, so the whole table is read and filtered here. */
    static List<Item> read(Context context, long now) {
        List<Item> items = new ArrayList<>();
        try (Cursor c = context.getContentResolver().query(URI, COLUMNS, null, null, null)) {
            while (c != null && c.moveToNext()) {
                int type = c.isNull(2) ? -1 : c.getInt(2);
                long time = c.getLong(3);
                long position = c.getLong(4);
                long duration = c.getLong(5);
                String title = c.getString(1);
                String link = c.getString(6);
                if (type != TYPE_CONTINUE && type != TYPE_NEXT) continue;
                if (Item.neverListed(c.getString(0))) continue;
                if (title == null || link == null || now - time > CUTOFF_MS) continue;
                if (duration > 0 && (double) position / duration >= FINISHED) continue;
                items.add(new Item(c.getString(0), title + episode(c), time, position, duration, link));
            }
        } catch (SecurityException | IllegalArgumentException notAvailable) {
            return new ArrayList<>();
        }
        return items;
    }

    private static String episode(Cursor c) {
        String season = c.getString(7);
        String number = c.getString(8);
        String name = c.getString(9);
        String label = season != null && number != null ? "S" + season + " E" + number : "";
        if (name != null && !name.isEmpty()) label = label.isEmpty() ? name : label + " " + name;
        return label.isEmpty() ? "" : " · " + label;
    }

    /** One row per title, newest first. The app's own link wins, with the latest time from either source. */
    static List<Item> merge(List<Item> watchNext, List<Item> history) {
        Map<String, Item> merged = new HashMap<>();
        for (Item item : history) merged.put(item.key(), item);
        for (Item item : watchNext) {
            Item other = merged.get(item.key());
            if (other == null || other.time <= item.time) {
                merged.put(item.key(), item);
            } else {
                merged.put(item.key(), new Item(other.pkg, other.title, other.time, other.positionMs,
                        other.durationMs, item.intentUri));
            }
        }
        List<Item> items = new ArrayList<>(merged.values());
        items.sort((a, b) -> Long.compare(b.time, a.time));
        return items;
    }
}
