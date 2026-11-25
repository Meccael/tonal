package com.meccael.tonal.application;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaDataSource;
import android.media.MediaMetadataRetriever;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import java.io.File;
import java.util.Calendar;
import com.meccael.tonal.R;
import com.meccael.tonal.artwork.AlbumInfoStore;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.usb.UsbDrive;

/** The "song details" dialog: tags, size, format and a blurred cover backdrop. */
public class InformationsWindow {
    private static final String TAG = "TonalUsb";
    private static final int BLUR_SOURCE_SIZE = 92;
    private static final int COVER_SIZE = 356;

    // ---- Small formatting helpers -----------------------------------------------------------

    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private String orUnknown(Context context, String text) {
        return isBlank(text) ? context.getString(R.string.unknown) : text;
    }

    /** "256000" becomes "256 Kbps"; anything too short to carry a rate is shown as is. */
    private String formatBitrate(Context context, String raw) {
        String value = orUnknown(context, raw);
        if (value.equals(context.getString(R.string.unknown)) || value.length() <= 3) {
            return value;
        }
        return value.substring(0, 3) + " Kbps";
    }

    private String formatDate(long seconds) {
        Calendar day = Calendar.getInstance();
        day.setTimeInMillis(seconds * 1000L);
        return day.get(Calendar.DAY_OF_MONTH) + "/" + day.get(Calendar.MONTH) + "/" + day.get(Calendar.YEAR);
    }

    private String extensionOf(String path) {
        return path.substring(path.lastIndexOf('.') + 1).toUpperCase();
    }

    private String fileNameOf(String path) {
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private long sizeOnDisk(File file) {
        if (!file.isDirectory()) {
            return file.length();
        }
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                total += sizeOnDisk(child);
            }
        }
        return total;
    }

    private static String kilobytesToText(long kilobytes) {
        return kilobytes >= 1024 ? (kilobytes / 1024) + "MB" : kilobytes + "KB";
    }

    // ---- Public helpers (also used by other screens) ----------------------------------------

    public String getDuration(Context context, long seconds) {
        long minutes = (seconds % 3600) / 60;
        if (seconds / 60 < 60) {
            return context.getString(R.string.time_minute, minutes, seconds % 60);
        }
        return context.getString(R.string.time_hour, seconds / 3600, minutes);
    }

    public String getSize(String path) {
        return kilobytesToText(sizeOnDisk(new File(path)) / 1024);
    }

    // ---- The dialog -------------------------------------------------------------------------

    public void show(Context context, Music song) {
        String path = song.getFile();
        boolean onDrive = path != null && path.startsWith(UsbDrive.PREFIX);

        MediaMetadataRetriever tags = new MediaMetadataRetriever();
        String driveSize = null;
        if (onDrive) {
            MediaDataSource source = UsbDrive.get(context).openSource(path);
            try {
                if (source != null) {
                    tags.setDataSource(source);
                    driveSize = kilobytesToText(source.getSize() / 1024);
                }
            } catch (Exception e) {
                Log.w(TAG, "Details: " + e);
            }
        } else {
            tags.setDataSource(path);
        }

        String genre = tags.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE);
        String year = tags.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR);
        // Fall back to the downloaded album info when the file itself has no genre or year.
        String[] online = AlbumInfoStore.get(context, song.getCover());
        if (online != null) {
            if (isBlank(genre) && !online[1].isEmpty()) {
                genre = online[1];
            }
            if (isBlank(year) && !online[0].isEmpty()) {
                year = online[0];
            }
        }
        String bitrate = formatBitrate(context, tags.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE));
        String size = driveSize != null ? driveSize : (onDrive ? "" : getSize(path));

        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(1);
        dialog.setContentView(R.layout.window_informations);
        dialog.setCancelable(true);

        setText(dialog, R.id.details_name, fileNameOf(path));
        setText(dialog, R.id.details_title, context.getString(R.string.song_title) + " : " + song.getTitle());
        setText(dialog, R.id.details_artist, context.getString(R.string.song_artist) + " : " + song.getArtist());
        setText(dialog, R.id.details_album, context.getString(R.string.song_album) + " : " + song.getAlbum());
        setText(dialog, R.id.details_genre, context.getString(R.string.song_genre) + " : " + orUnknown(context, genre));
        setText(dialog, R.id.details_year, context.getString(R.string.song_year) + " : " + orUnknown(context, year));
        setText(dialog, R.id.details_duration, context.getString(R.string.song_duration) + " : " + getDuration(context, song.getDuration()));
        setText(dialog, R.id.details_bitrate, context.getString(R.string.song_bitrate) + " : " + bitrate);
        setText(dialog, R.id.details_size, size);
        setText(dialog, R.id.details_format, extensionOf(path));
        setText(dialog, R.id.details_date, context.getString(R.string.song_modified) + " : " + formatDate(song.getDate()));
        dialog.findViewById(R.id.details_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });

        int blurRadius = new Preferences(context).getSettingsUIBlurIntensity() * 2;
        Bitmap cover = Utils.getCoverBitamp(context, song.getCover(), R.drawable.cover0, COVER_SIZE);
        Bitmap small = Bitmap.createScaledBitmap(cover, BLUR_SOURCE_SIZE, BLUR_SOURCE_SIZE, false);
        ((android.widget.ImageView) dialog.findViewById(R.id.details_cover_blur)).setImageBitmap(Utils.blur(small, blurRadius));
        ((android.widget.ImageView) dialog.findViewById(R.id.details_cover)).setImageBitmap(cover);
        dialog.show();
    }

    private static void setText(Dialog dialog, int id, String text) {
        ((TextView) dialog.findViewById(id)).setText(text);
    }
}
