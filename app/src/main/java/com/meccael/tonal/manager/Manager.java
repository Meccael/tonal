package com.meccael.tonal.manager;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Preferences;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.usb.UsbDrive;

/**
 * Builds the music library. Constructing a Manager reads the device's media store (plus any USB
 * drive tracks and saved playlists), sorts everything into the browse categories and hands them to
 * the service. Given a single file it loads just that file instead, for "open with".
 */
public class Manager {
    private static final String TAG = "TONAL MUSIC MANAGER";

    // Category id prefixes: the id is the prefix followed by the song's running number.
    private static final String ALBUM = "2";
    private static final String FOLDER = "3";
    private static final String ARTIST = "4";
    private static final String GENRE = "5";

    /** How long the last build took, in milliseconds (0 if nothing was loaded). */
    public long timePassed;

    Manager() {
        timePassed = 0L;
    }

    /** Column positions for the media-store audio table. */
    private static final class Columns {
        final int duration, id, albumId, dateModified, data, title, artist, album, displayName;

        Columns(Cursor c) {
            duration = c.getColumnIndex("duration");
            id = c.getColumnIndex("_id");
            albumId = c.getColumnIndex("album_id");
            dateModified = c.getColumnIndex("date_modified");
            data = c.getColumnIndex("_data");
            title = c.getColumnIndex("title");
            artist = c.getColumnIndex("artist");
            album = c.getColumnIndex("album");
            displayName = c.getColumnIndex("_display_name");
        }
    }

    public Manager(Context context, Preferences preferences, ServiceMusic service, File file) {
        timePassed = 0L;
        long started = System.currentTimeMillis();
        Cursor songs = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
        if (songs == null) {
            Log.i(TAG, "Manager executed but impossible to fetch the mediastore database.");
            return;
        }
        if (!songs.moveToFirst()) {
            songs.close();
            Log.i(TAG, "Manager executed and no media found on the device.");
            return;
        }
        Log.i(TAG, "Fetching " + songs.getCount() + " files.");
        if (file != null) {
            loadSingleFile(context, service, songs, file);
        } else {
            loadLibrary(context, preferences, service, songs);
        }
        timePassed = System.currentTimeMillis() - started;
        Log.i(TAG, "Manager successfully executed and takes " + timePassed + " ms.");
    }

    private static int categoryId(String prefix, int number) {
        return Integer.parseInt(prefix + number);
    }

    // ---- One file ("open with") -------------------------------------------------------------

    private void loadSingleFile(Context context, ServiceMusic service, Cursor songs, File file) {
        Columns col = new Columns(songs);
        AllMusics only = new AllMusics();
        String wanted = file.toString();
        do {
            String path = songs.getString(col.data);
            if (path.equals(wanted)) {
                String[] tags = tagFinder(context, songs.getString(col.title), songs.getString(col.artist),
                        songs.getString(col.album), songs.getString(col.displayName));
                only.addSong(new Music(songs.getLong(col.id), songs.getLong(col.albumId), songs.getLong(col.duration) / 1000,
                        songs.getLong(col.dateModified), path, tags[0], tags[1], tags[2]));
            }
        } while (songs.moveToNext());
        songs.close();
        Log.i(TAG, "Music loaded.");
        service.cleanDatas();
        service.addCategory(new Category(only.getPlaylistName(context), only.getPlaylistCover(), R.drawable.cover0,
                only.getPlaylistSize(), only.getPlaylistDuration(), only.getPlaylistList(context)));
    }

    // ---- The whole library ------------------------------------------------------------------

    private void loadLibrary(Context context, Preferences preferences, ServiceMusic service, Cursor songs) {
        // Songs this many seconds or shorter are left out ("skip short songs" setting).
        int shortest = preferences.getSettingsPlayerExcludeMusic() - 1;
        Set<String> driveChoice = DriveFilter.chosen(context);
        Cursor genreCursor = context.getContentResolver().query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
                new String[]{"name", "_id"}, null, null, null);

        AllMusics allMusics = new AllMusics();
        AllRecents allRecents = new AllRecents();
        AllAlbums allAlbums = new AllAlbums();
        AllFolders allFolders = new AllFolders();
        AllArtists allArtists = new AllArtists();
        AllGenres allGenres = new AllGenres();
        AllPlaylists allPlaylists = new AllPlaylists();

        int number = readDeviceSongs(context, songs, driveChoice, shortest, allMusics, allRecents, allAlbums, allFolders, allArtists);
        addDriveTracks(context, number, shortest, allMusics, allRecents, allAlbums, allFolders, allArtists);
        readGenres(context, genreCursor, shortest, allGenres);
        readSavedPlaylists(context, allPlaylists);

        Log.i(TAG, "All musics have been loaded.");
        allRecents.manage();
        allMusics.manage(preferences);
        allAlbums.manage(context);
        allFolders.manage();
        allArtists.manage();
        allGenres.manage();
        allPlaylists.manage();

        service.cleanDatas();
        service.addRecent(new Category(allRecents.getPlaylistName(context), allRecents.getPlaylistCover(), R.drawable.cover0,
                allRecents.getPlaylistSize(), allRecents.getPlaylistDuration(), allRecents.getPlaylistList(context)));
        service.addCategory(new Category(allMusics.getPlaylistName(context), allMusics.getPlaylistCover(), R.drawable.cover0,
                allMusics.getPlaylistSize(), allMusics.getPlaylistDuration(), allMusics.getPlaylistList(context)));
        service.addCategory(new Category(allAlbums.getPlaylistName(context), allAlbums.getPlaylistCover(), R.drawable.cover3,
                allAlbums.getPlaylistSize(), allAlbums.getPlaylistDuration(), allAlbums.getPlaylistList()));
        service.addCategory(new Category(allFolders.getPlaylistName(context), allFolders.getPlaylistCover(), R.drawable.cover4,
                allFolders.getPlaylistSize(), allFolders.getPlaylistDuration(), allFolders.getPlaylistList()));
        service.addCategory(new Category(allArtists.getPlaylistName(context), allArtists.getPlaylistCover(), R.drawable.cover6,
                allArtists.getPlaylistSize(), allArtists.getPlaylistDuration(), allArtists.getPlaylistList()));
        service.addCategory(new Category(allGenres.getPlaylistName(context), allGenres.getPlaylistCover(), R.drawable.cover2,
                allGenres.getPlaylistSize(), allGenres.getPlaylistDuration(), allGenres.getPlaylistList()));
        service.addCategory(new Category(allPlaylists.getPlaylistName(context), allPlaylists.getPlaylistCover(), R.drawable.cover1,
                allPlaylists.getPlaylistSize(), allPlaylists.getPlaylistDuration(), allPlaylists.getPlaylistList()));
        service.saveCategories(context);
    }

    /** Reads every song the device reports; returns how many were kept (the next running number). */
    private int readDeviceSongs(Context context, Cursor songs, Set<String> driveChoice, int shortest,
                                AllMusics allMusics, AllRecents allRecents, AllAlbums allAlbums,
                                AllFolders allFolders, AllArtists allArtists) {
        Columns col = new Columns(songs);
        int number = 0;
        int driveSongs = 0;
        do {
            long seconds = songs.getLong(col.duration) / 1000;
            String path = songs.getString(col.data);
            if (seconds <= shortest || DriveFilter.isHidden(context, path, driveChoice)) {
                continue;
            }
            long albumId = songs.getLong(col.albumId);
            String[] tags = tagFinder(context, songs.getString(col.title), songs.getString(col.artist),
                    songs.getString(col.album), songs.getString(col.displayName));
            Music music = new Music(songs.getLong(col.id), albumId, seconds, songs.getLong(col.dateModified),
                    path, tags[0], tags[1], tags[2]);
            allMusics.addSong(music);
            if (music.isOnDrive()) {
                driveSongs++;
            }
            allRecents.addSong(music);
            allAlbums.addPlaylist(tags[2], categoryId(ALBUM, number), albumId, seconds, music);
            allFolders.addPlaylist(path, categoryId(FOLDER, number), albumId, seconds, music);
            allArtists.addPlaylist(tags[1], categoryId(ARTIST, number), albumId, seconds, music);
            number++;
        } while (songs.moveToNext());
        songs.close();
        DriveWatcher.sLoadedDriveSongs = driveSongs;
        return number;
    }

    /** Songs read straight off the USB drive (not mounted by Android) join the same categories. */
    private void addDriveTracks(Context context, int number, int shortest, AllMusics allMusics, AllRecents allRecents,
                                AllAlbums allAlbums, AllFolders allFolders, AllArtists allArtists) {
        try {
            List<UsbDrive.Track> tracks = DrivePrefs.showSongs(context)
                    ? UsbDrive.get(context).activeTracks() : new ArrayList<UsbDrive.Track>();
            for (UsbDrive.Track track : tracks) {
                if (track.duration <= shortest) {
                    continue;
                }
                Music music = new Music(track.id(), track.coverId, track.duration, track.date,
                        UsbDrive.PREFIX + track.path, track.title, track.artist, track.album);
                allMusics.addSong(music);
                allRecents.addSong(music);
                allAlbums.addPlaylist(track.album, categoryId(ALBUM, number), track.coverId, track.duration, music);
                allFolders.addPlaylist(music.getFile(), categoryId(FOLDER, number), track.coverId, track.duration, music);
                allArtists.addPlaylist(track.artist, categoryId(ARTIST, number), track.coverId, track.duration, music);
                number++;
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not add the USB drive's songs.", e);
        }
    }

    /**
     * Genres come from their own table. A genre's position counts every genre seen, including ones
     * left empty by the short-song filter, because the genre list is addressed by that position.
     */
    private void readGenres(Context context, Cursor genres, int shortest, AllGenres allGenres) {
        if (genres == null || !genres.moveToFirst()) {
            return;
        }
        int position = -1;
        do {
            position++;
            String name = genres.getString(genres.getColumnIndexOrThrow("name"));
            String rawId = genres.getString(genres.getColumnIndexOrThrow("_id"));
            Cursor members = context.getContentResolver().query(
                    MediaStore.Audio.Genres.Members.getContentUri("external", Long.parseLong(rawId == null ? "0" : rawId)),
                    null, null, null, null);
            if (members != null && members.moveToFirst()) {
                boolean first = true;
                do {
                    try {
                        long seconds = members.getLong(members.getColumnIndexOrThrow("duration")) / 1000;
                        if (seconds > shortest) {
                            Music music = new Music(
                                    members.getLong(members.getColumnIndexOrThrow("_id")),
                                    members.getLong(members.getColumnIndexOrThrow("album_id")),
                                    seconds,
                                    members.getLong(members.getColumnIndexOrThrow("date_modified")),
                                    members.getString(members.getColumnIndexOrThrow("_data")),
                                    members.getString(members.getColumnIndexOrThrow("title")),
                                    members.getString(members.getColumnIndexOrThrow("artist")),
                                    members.getString(members.getColumnIndexOrThrow("album")));
                            if (first) {
                                first = false;
                                allGenres.addPlaylist(name, categoryId(GENRE, position), music.getCover(), music.getDuration(), music);
                            } else {
                                allGenres.completePlaylist(position, music.getDuration(), music);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } while (members.moveToNext());
            }
            if (members != null) {
                members.close();
            }
        } while (genres.moveToNext());
        genres.close();
    }

    /** Saved playlists are files in the app's private folder. */
    private void readSavedPlaylists(Context context, AllPlaylists allPlaylists) {
        int index = 0;
        for (String name : context.getFilesDir().list()) {
            Playlist playlist = Playlists.load(context, name, index);
            if (playlist != null) {
                allPlaylists.addPlaylist(playlist);
                index++;
            }
        }
    }

    // ---- Tags -------------------------------------------------------------------------------

    private static boolean missing(String tag) {
        return tag == null || tag.isEmpty() || tag.equals("<unknown>");
    }

    /**
     * Returns {title, artist, album}. Untagged files fall back to the file name, and a name shaped
     * like "Artist - Title.ext" is split into artist and title.
     */
    private String[] tagFinder(Context context, String title, String artist, String album, String fileName) {
        String unknownArtist = context.getString(R.string.unknown_artist);
        String unknown = context.getString(R.string.unknown);
        String name = fileName == null ? unknown : fileName;
        String guessedArtist = unknownArtist;

        if (missing(title)) {
            String guessedTitle = name;
            if (name.contains(" - ")) {
                String[] parts = name.split(" - ");
                guessedArtist = parts[0];
                // Drop the extension (".mp3" and the like) from the title half.
                String withoutExtension = parts[1].length() > 4 ? parts[1].substring(0, parts[1].length() - 4) : parts[1];
                if (withoutExtension.length() > 1) {
                    guessedTitle = withoutExtension;
                }
                if (guessedArtist.length() <= 1) {
                    guessedArtist = unknownArtist;
                }
            }
            title = guessedTitle;
        }
        if (missing(artist)) {
            if (guessedArtist.equals(unknownArtist) && name.contains(" - ")) {
                guessedArtist = name.split(" - ")[0];
                if (guessedArtist.length() <= 1) {
                    guessedArtist = unknownArtist;
                }
            }
            artist = guessedArtist;
        }
        if (missing(album)) {
            album = unknown;
        }
        return new String[]{title, artist, album};
    }

    // ---- Saved playlists only ---------------------------------------------------------------

    /** Rebuilds just the Playlists category after one was saved or deleted. */
    void updatePlaylists(Context context, ServiceMusic service) {
        AllPlaylists allPlaylists = new AllPlaylists();
        readSavedPlaylists(context, allPlaylists);
        allPlaylists.manage();
        service.deleteLastCategory();
        service.addCategory(new Category(allPlaylists.getPlaylistName(context), allPlaylists.getPlaylistCover(), R.drawable.cover1,
                allPlaylists.getPlaylistSize(), allPlaylists.getPlaylistDuration(), allPlaylists.getPlaylistList()));
        service.saveCategories(context);
    }
}
