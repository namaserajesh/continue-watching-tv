package local.continuewatching;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import java.util.List;

public class MainActivity extends Activity {
    private static final int GROUND = Color.rgb(0x16, 0x13, 0x0F);
    private static final int RAISED = Color.rgb(0x2A, 0x24, 0x1C);
    private static final int TEXT = Color.rgb(0xF4, 0xED, 0xE2);
    private static final int MUTED = Color.rgb(0xB5, 0xA9, 0x98);
    private static final int ACCENT = Color.rgb(0xE8, 0xA3, 0x3D);
    private static final int TRACK = Color.rgb(0x3A, 0x30, 0x24);
    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private static final String YOUTUBE_SEARCH = "https://www.youtube.com/results?search_query=";

    private LinearLayout list;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(GROUND);
        scroll.setClipToPadding(false);
        scroll.setPadding(dp(56), dp(32), dp(56), dp(32));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setClipChildren(false);
        scroll.setClipChildren(false);
        scroll.addView(list);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        list.removeAllViews();
        list.addView(text(getString(R.string.app_name), 34, TEXT), margins(0, 0, 0, 20));
        long now = System.currentTimeMillis();
        List<Item> items = WatchNext.merge(WatchNext.read(this, now), History.read(this));
        if (!listenerAllowed()) list.addView(text(getString(R.string.no_access), 16, ACCENT), margins(0, 0, 0, 16));
        if (items.isEmpty()) list.addView(text(getString(R.string.empty), 20, MUTED));
        for (Item item : items) list.addView(row(item, now), margins(0, 0, 0, 10));
        if (!items.isEmpty()) list.getChildAt(list.getChildCount() - items.size()).requestFocus();
    }

    private boolean listenerAllowed() {
        String allowed = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return allowed != null
                && allowed.contains(new ComponentName(this, NowPlayingListener.class).flattenToString());
    }

    private View row(Item item, long now) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(20), dp(12));
        row.setFocusable(true);
        row.setBackground(rowBackground());
        row.setOnClickListener(view -> open(item));
        row.setOnFocusChangeListener((view, focused) -> {
            float scale = focused ? 1.015f : 1f;
            view.animate().scaleX(scale).scaleY(scale).setDuration(120).start();
        });

        ImageView banner = new ImageView(this);
        banner.setScaleType(ImageView.ScaleType.FIT_CENTER);
        banner.setImageDrawable(artFor(item.pkg));
        row.addView(banner, new LinearLayout.LayoutParams(dp(112), dp(63)));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView title = text(item.title, 22, TEXT);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        column.addView(title);
        column.addView(text(meta(item, now), 15, MUTED), margins(0, 3, 0, 0));
        if (item.durationMs > 0 && item.positionMs > 0) column.addView(progress(item), margins(0, 8, 0, 0));
        LinearLayout.LayoutParams grow = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        grow.leftMargin = dp(20);
        row.addView(column, grow);
        return row;
    }

    private String meta(Item item, long now) {
        String when = DateUtils.getRelativeTimeSpanString(item.time, now, DateUtils.MINUTE_IN_MILLIS).toString();
        String meta = labelFor(item.pkg) + " · " + when;
        if (item.durationMs > 0 && item.positionMs > 0) {
            meta += " · " + getString(R.string.progress, (int) (item.positionMs / 60000), (int) (item.durationMs / 60000));
        }
        return meta;
    }

    private View progress(Item item) {
        FrameLayout track = new FrameLayout(this);
        track.setBackgroundColor(TRACK);
        track.setMinimumHeight(dp(4));
        View fill = new View(this);
        fill.setBackgroundColor(ACCENT);
        float done = Math.min(1f, (float) item.positionMs / item.durationMs);
        LinearLayout bar = new LinearLayout(this);
        bar.addView(fill, new LinearLayout.LayoutParams(0, dp(4), done));
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, dp(4), 1f - done));
        track.addView(bar);
        return track;
    }

    private void open(Item item) {
        Intent intent = item.intentUri != null ? linkOf(item) : null;
        if (intent == null && YOUTUBE.equals(item.pkg)) {
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_SEARCH + Uri.encode(item.title)))
                    .setPackage(YOUTUBE);
        }
        if (intent == null) intent = appOf(item.pkg);
        if (intent == null) {
            Toast.makeText(this, labelFor(item.pkg), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException linkFailed) {
            Intent app = appOf(item.pkg);
            if (app != null) startActivity(app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    /** Another app wrote this link, so it may only ever open that same app. */
    private Intent linkOf(Item item) {
        try {
            Intent link = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME);
            link.setSelector(null);
            link.setComponent(null);
            link.setPackage(item.pkg);
            return link;
        } catch (URISyntaxException badLink) {
            return null;
        }
    }

    private Intent appOf(String pkg) {
        PackageManager pm = getPackageManager();
        Intent app = pm.getLeanbackLaunchIntentForPackage(pkg);
        return app != null ? app : pm.getLaunchIntentForPackage(pkg);
    }

    private String labelFor(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException gone) {
            return pkg;
        }
    }

    private Drawable artFor(String pkg) {
        PackageManager pm = getPackageManager();
        try {
            Drawable banner = pm.getApplicationBanner(pkg);
            return banner != null ? banner : pm.getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException gone) {
            return null;
        }
    }

    private Drawable rowBackground() {
        GradientDrawable focused = new GradientDrawable();
        focused.setCornerRadius(dp(12));
        focused.setColor(RAISED);
        focused.setStroke(dp(2), TEXT);
        GradientDrawable normal = new GradientDrawable();
        normal.setCornerRadius(dp(12));
        normal.setColor(Color.TRANSPARENT);
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused}, focused);
        states.addState(new int[]{}, normal);
        return states;
    }

    private TextView text(String value, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
