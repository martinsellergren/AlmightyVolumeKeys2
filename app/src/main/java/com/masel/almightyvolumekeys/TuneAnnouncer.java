package com.masel.almightyvolumekeys;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;

class TuneAnnouncer {
    private Voice voice;
    private MediaSessionManager mediaSessionManager;

    TuneAnnouncer(MyContext myContext) {
        this.voice = myContext.voice;
        this.mediaSessionManager = (MediaSessionManager)myContext.context.getSystemService(Context.MEDIA_SESSION_SERVICE);
    }

    boolean announceTitle() {
        MediaController player = getPlayingMediaController();
        if (player == null) return false;

        String title = getTitle(player);
        if (title == null) return false;

        return voice.speak(title);
    }

    boolean announceTitleAndArtist() {
        MediaController player = getPlayingMediaController();
        if (player == null) return false;
        if (player.getMetadata() == null) return false;

        String title = getTitle(player);
        String artist = getArtist(player);
        if (title == null || artist == null) return false;

        return voice.speak(String.format("%s, %s", title, artist));
    }

    boolean announceTitleArtistAndAlbum() {
        MediaController player = getPlayingMediaController();
        if (player == null) return false;
        if (player.getMetadata() == null) return false;

        String title = getTitle(player);
        String artist = getArtist(player);
        String album = getAlbum(player);
        if (title == null || artist == null || album == null) return false;

        return voice.speak(String.format("%s, %s, %s", title, artist, album));
    }

    private String getTitle(MediaController controller) {
        if (controller.getMetadata() == null) return null;
        String title = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
        if (title == null) title = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_TITLE);
        return title;
    }

    private String getArtist(MediaController controller) {
        if (controller.getMetadata() == null) return null;
        String artist = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        if (artist == null) artist = controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ARTIST);
        return artist;
    }

    private String getAlbum(MediaController controller) {
        if (controller.getMetadata() == null) return null;
        return controller.getMetadata().getString(MediaMetadata.METADATA_KEY_ALBUM);
    }

    private MediaController getPlayingMediaController() {
        for (MediaController mediaController : mediaSessionManager.getActiveSessions(new ComponentName("com.masel.almightyvolumekeys", "com.masel.almightyvolumekeys.MonitorService"))) {
            if (mediaController.getPlaybackState() == null) continue;
            if (mediaController.getPlaybackState().getState() == PlaybackState.STATE_PLAYING) return mediaController;
        }
        return null;
    }

    static boolean isAvailable(Context context) {
        return Voice.isAvailable(context);
    }
}
