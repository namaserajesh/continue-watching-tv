package local.continuewatching;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;

import java.util.HashSet;
import java.util.Set;

/**
 * Saves what is playing, for the apps that publish no Watch Next rows (YouTube among them). A title must be
 * playing on two checks in a row before it is saved, which keeps out previews on the apps' home screens.
 * It reads media sessions only; no notification callback is overridden.
 */
public class NowPlayingListener extends NotificationListenerService {
    private static final long CHECK_MS = 30_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable check = this::check;
    private Set<String> playingLastCheck = new HashSet<>();

    @Override
    public void onListenerConnected() {
        handler.removeCallbacks(check);
        handler.post(check);
    }

    @Override
    public void onListenerDisconnected() {
        handler.removeCallbacks(check);
    }

    private void check() {
        Set<String> playing = new HashSet<>();
        try {
            MediaSessionManager sessions = getSystemService(MediaSessionManager.class);
            for (MediaController controller : sessions.getActiveSessions(
                    new ComponentName(this, NowPlayingListener.class))) {
                Item item = itemOf(controller);
                if (item == null) continue;
                playing.add(item.key());
                if (playingLastCheck.contains(item.key())) History.record(this, item);
            }
        } catch (SecurityException accessWithdrawn) {
            return;
        }
        playingLastCheck = playing;
        handler.postDelayed(check, CHECK_MS);
    }

    private Item itemOf(MediaController controller) {
        if (Item.neverListed(controller.getPackageName())) return null;
        PlaybackState state = controller.getPlaybackState();
        MediaMetadata metadata = controller.getMetadata();
        if (state == null || metadata == null || state.getState() != PlaybackState.STATE_PLAYING) return null;
        String title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
        if (title == null) title = metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null || title.trim().isEmpty()) return null;
        return new Item(controller.getPackageName(), title.trim(), System.currentTimeMillis(),
                Math.max(0, state.getPosition()), Math.max(0, metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)),
                null);
    }
}
