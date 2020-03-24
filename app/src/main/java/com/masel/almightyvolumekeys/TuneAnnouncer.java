package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

import androidx.annotation.NonNull;

import com.masel.rec_utils.RecUtils;

class TuneAnnouncer {
    private MyContext myContext;
    private MediaSessionManager mediaSessionManager;

    private Action.ExecutionException failException = new Action.ExecutionException("Failed to announce tune");

    TuneAnnouncer(MyContext myContext) {
        this.myContext = myContext;
        this.mediaSessionManager = (MediaSessionManager)myContext.context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    void announceTitle() throws Action.ExecutionException {
        MediaController player = getPlayingMediaController();
        if (player == null) throw failException;

        String title = getTitle(player);
        if (title.length() == 0) throw failException;

        if (!myContext.voice.speak(title)) throw failException;
    }

    void announceTitleAndArtist() throws Action.ExecutionException {
        MediaController player = getPlayingMediaController();
        if (player == null) throw failException;

        String title = getTitle(player);
        String artist = getArtist(player);
        if (title.length() == 0 && artist.length() == 0) throw failException;

        if (!myContext.voice.speak(String.format("%s, %s", title, artist))) throw failException;
    }

    void announceTitleArtistAndAlbum() throws Action.ExecutionException {
        MediaController player = getPlayingMediaController();
        if (player == null) throw failException;

        String title = getTitle(player);
        String artist = getArtist(player);
        String album = getAlbum(player);
        if (title.length() == 0 && artist.length() == 0 && album.length() == 0) throw failException;

        if (!myContext.voice.speak(String.format("%s, %s, %s", title, artist, album))) throw failException;
    }

    private @NonNull String getTitle(MediaController controller) {
        if (controller.getMetadata() == null) return "";
        String title = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null) title = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
        if (title == null) title = "";
        title = RecUtils.removeExtension(title);
        return optimizeForAnnouncement(title);
    }

    private @NonNull String getArtist(MediaController controller) {
        if (controller.getMetadata() == null) return "";
        String artist = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (artist == null) artist = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ARTIST);
        artist = artist != null ? artist : "";
        return optimizeForAnnouncement(artist);
    }

    private @NonNull String getAlbum(MediaController controller) {
        if (controller.getMetadata() == null) return "";
        String album = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ALBUM);
        album = album != null ? album : "";
        return optimizeForAnnouncement(album);
    }

    private String optimizeForAnnouncement(String text) {
        String initSymbolsRegex = "^[^\\p{IsAlphabetic}\\p{IsDigit}]*";
        text = text.replaceFirst(initSymbolsRegex, "");
        return RecUtils.reverse(RecUtils.reverse(text).replaceFirst(initSymbolsRegex, ""));
    }

    private MediaController getPlayingMediaController() {
        for (MediaController mediaController : mediaSessionManager.getActiveSessions(new ComponentName("com.masel.almightyvolumekeys", "com.masel.almightyvolumekeys.MonitorService"))) {
            if (mediaController.getPlaybackState() == null) continue;
            if (mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) return mediaController;
        }
        return null;
    }
}
