package local.continuewatching;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The now-playing records, newest first, kept only in this app's private storage. */
final class History {
    private static final String PREFS = "history";
    private static final String KEY = "rows";
    private static final int MAX = 50;

    private History() {
    }

    static List<Item> read(Context context) {
        List<Item> items = new ArrayList<>();
        String json = prefs(context).getString(KEY, "[]");
        try {
            JSONArray rows = new JSONArray(json);
            for (int i = 0; i < rows.length(); i++) {
                JSONObject row = rows.getJSONObject(i);
                if (Item.neverListed(row.getString("p"))) continue;
                items.add(new Item(row.getString("p"), row.getString("t"), row.getLong("w"),
                        row.getLong("o"), row.getLong("d"), null));
            }
        } catch (JSONException corrupt) {
            return new ArrayList<>();
        }
        return items;
    }

    static void record(Context context, Item item) {
        List<Item> items = read(context);
        items.removeIf(old -> old.key().equals(item.key()));
        items.add(0, item);
        JSONArray rows = new JSONArray();
        try {
            for (int i = 0; i < Math.min(items.size(), MAX); i++) {
                Item row = items.get(i);
                rows.put(new JSONObject().put("p", row.pkg).put("t", row.title).put("w", row.time)
                        .put("o", row.positionMs).put("d", row.durationMs));
            }
        } catch (JSONException cannotHappenWithStringsAndLongs) {
            return;
        }
        prefs(context).edit().putString(KEY, rows.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
