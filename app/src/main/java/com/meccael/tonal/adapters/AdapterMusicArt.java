package com.meccael.tonal.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import com.meccael.tonal.R;
import com.meccael.tonal.application.InformationsWindow;
import com.meccael.tonal.application.Preferences;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.artwork.ArtworkPicker;
import com.meccael.tonal.artwork.ArtworkResolver;
import com.meccael.tonal.bottom_menu.BottomDialog;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.manager.Playlist;
import com.meccael.tonal.scrollbar.IndicatorAdapter;
import com.meccael.tonal.soundanimation.BeatView;

/** The song rows of an album, artist, genre, folder or "All Music" list, with the playing song highlighted. */
public class AdapterMusicArt extends RecyclerView.Adapter<AdapterMusicArt.ViewHolder> implements IndicatorAdapter {
    public static int lastSelectedPosition = -1;

    private static final int TEXT_PLAYING = 0xFFEADFC8;
    private static final int TEXT_NORMAL = 0xFFF4ECDC;

    private final Context mContext;
    private final int mCategory;
    private final int mPosition;
    private final ServiceMusic mService;
    private final Preferences mPreferences;
    private final TextView mToolbarTitle;

    private List<Music> mSongs;
    private String mName;
    private long mDuration;
    private long mSize;
    private long mId;
    private long mCover;

    class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final ImageView cover;
        final BeatView beat;
        final TextView title;
        final TextView artist;
        final TextView duration;
        Music song;

        ViewHolder(View view) {
            super(view);
            root = view;
            cover = (ImageView) view.findViewById(R.id.cover);
            beat = (BeatView) view.findViewById(R.id.beat);
            title = (TextView) view.findViewById(R.id.title);
            artist = (TextView) view.findViewById(R.id.artist);
            duration = (TextView) view.findViewById(R.id.duration);
        }
    }

    public AdapterMusicArt(Context context, List<Music> songs, int category, int position, ServiceMusic service, TextView toolbarTitle) {
        mContext = context;
        mSongs = songs;
        mCategory = category;
        mPosition = position;
        mService = service;
        mPreferences = new Preferences(context);
        mToolbarTitle = toolbarTitle;
        showWholePlaylist();
    }

    private Playlist source() {
        return mService.Categories.get(mCategory).getPlaylists().get(mPosition);
    }

    private void showWholePlaylist() {
        Playlist playlist = source();
        mSongs = new ArrayList<>(playlist.getPlaylistMusics());
        mDuration = playlist.getPlaylistDuration();
        mSize = playlist.getPlaylistSize();
        mId = playlist.getPlaylistID();
        mCover = playlist.getPlaylistCover();
        mName = playlist.getPlaylistName();
    }

    /** Narrows the list to songs whose title, artist or album contains the text; empty text shows everything. */
    public void getFilter(String text) {
        if (text.isEmpty()) {
            if (mSongs != null) {
                mSongs.clear();
            }
            showWholePlaylist();
        } else {
            String needle = text.toLowerCase();
            List<Music> matches = new ArrayList<>();
            long duration = 0;
            for (Music song : new ArrayList<>(source().getPlaylistMusics())) {
                if (song.getAlbum().toLowerCase().contains(needle)
                        || song.getArtist().toLowerCase().contains(needle)
                        || song.getTitle().toLowerCase().contains(needle)) {
                    matches.add(song.getMusic());
                    duration += song.getDuration();
                }
            }
            mDuration = duration;
            mSize = matches.size();
            mId = -1L;
            mName = mContext.getString(R.string.playlist_temp);
            mSongs.clear();
            mSongs.addAll(matches);
        }
        notifyDataSetChanged();
    }

    public List<Music> getMusics() {
        return mSongs;
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    // ---- Fast-scroll indicator --------------------------------------------------------------

    @Override
    public Character getCharacterForElement(int index) {
        try {
            Music song = mSongs.get(index);
            String text = "artist".equals(mPreferences.getItemsSortBy()) ? song.getArtist() : song.getTitle();
            char first = text.charAt(0);
            return Character.isDigit(first) || first == '+' || first == '-' ? '#' : first;
        } catch (Exception e) {
            return '#';
        }
    }

    @Override
    public long getYearForElement(int index) {
        if (!"date_modified".equals(mPreferences.getItemsSortBy())) {
            return 2016L;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mSongs.get(index).getDate() * 1000);
        return calendar.get(Calendar.YEAR);
    }

    // ---- Rows -------------------------------------------------------------------------------

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_music_cover, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Music song = mSongs.get(holder.getAdapterPosition());
        holder.song = song;
        bindPlayingState(holder, song);

        Glide.with(mContext)
                .load(ArtworkResolver.model(mContext, song.getCover()))
                .signature(ArtworkResolver.signature(mContext, song.getCover()))
                .placeholder(R.drawable.xml_cover)
                .override(128, 128)
                .transform(new RoundedCornersTransformation(mContext, 8))
                .into(holder.cover);
        holder.title.setText(song.getTitle());
        holder.artist.setText(song.isOnDrive() ? song.getArtist() + " · Drive" : song.getArtist());
        holder.duration.setText(DateUtils.formatElapsedTime(song.getDuration()));

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playFrom(holder.getAdapterPosition());
            }
        });
        holder.root.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showMenu(holder);
                return true;
            }
        });
    }

    private void bindPlayingState(ViewHolder holder, Music song) {
        boolean playing = song.getID() == mService.musicPlayed.getMusic(mContext).getID();
        if (!playing) {
            holder.root.setBackground(null);
            holder.title.setTextColor(TEXT_NORMAL);
            holder.beat.stop(true);
            holder.beat.setVisibility(View.GONE);
            holder.duration.setVisibility(View.VISIBLE);
            return;
        }
        lastSelectedPosition = holder.getAdapterPosition();
        holder.root.setBackgroundResource(R.drawable.pl_row_now);
        holder.title.setTextColor(TEXT_PLAYING);
        if (mService.isPlaying()) {
            holder.beat.resume(true);
        } else {
            holder.beat.pause();
        }
        if (mService.musicLaunched()) {
            holder.beat.setVisibility(View.VISIBLE);
            holder.duration.setVisibility(View.GONE);
        }
    }

    private void playFrom(int position) {
        mService.musicPlayed.setPosition(position);
        mService.playlistPlayed = new CurrentPlaylist();
        mService.playlistPlayed.newPlaylist(mName, mId, mCover, mSize, mDuration, mSongs);
        mService.play(mContext, true);
    }

    private void showMenu(final ViewHolder holder) {
        BottomDialog dialog = new BottomDialog(mContext);
        dialog.title(holder.song.getTitle());
        dialog.canceledOnTouchOutside(true);
        dialog.cancelable(true);
        dialog.inflateMenu(R.menu.menu_bottom_explorer);
        dialog.setOnItemSelectedListener(new BottomDialog.OnItemSelectedListener() {
            @Override
            public boolean onItemSelected(int itemId) {
                Music target = mSongs.get(holder.getAdapterPosition()).getMusic();
                if (itemId == R.id.current_playlist_add_next) {
                    mService.playlistPlayed.setPlaylistName(mContext.getString(R.string.playlist_temp), mToolbarTitle);
                    mService.playlistPlayed.addSongToNext(target, mService);
                } else if (itemId == R.id.current_playlist_add_end) {
                    mService.playlistPlayed.setPlaylistName(mContext.getString(R.string.playlist_temp), mToolbarTitle);
                    mService.playlistPlayed.addSongToEnd(target);
                } else if (itemId == R.id.artwork_change) {
                    ArtworkPicker.show(mContext, target.getCover(), new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                } else if (itemId == R.id.current_song_details) {
                    new InformationsWindow().show(mContext, target);
                }
                return true;
            }
        });
        dialog.show();
    }
}
