package com.meccael.tonal.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Fragment_Playlist;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.Music;
import com.meccael.tonal.usb.UsbDrive;

/**
 * The queue, drawn as a vertical rail: played tracks are dimmed with filled dots, the playing track is
 * larger and glowing, and upcoming tracks have hollow dots. Rows can be dragged and swiped away.
 */
public class AdapterMusicMoveable extends RecyclerView.Adapter<AdapterMusicMoveable.ViewHolder> {
    public static int lastSelectedPosition = -1;

    private static final int ACCENT = 0xFFEADFC8;
    private static final int FAINT = 0x24EADFC8;

    private final Context mContext;
    private final Fragment_Playlist mFragment;
    private final ServiceMusic mService;
    private final TextView mToolbarSubTitle;
    private List<Music> mSongs;

    class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final View row;
        final View dot;
        final View railTop;
        final View railBottom;
        final TextView title;
        final TextView artist;
        final TextView duration;
        final ImageView drag;
        Music song;

        ViewHolder(View view) {
            super(view);
            root = view;
            drag = (ImageView) view.findViewById(R.id.items_drag);
            artist = (TextView) view.findViewById(R.id.items_artist);
            dot = view.findViewById(R.id.items_dot);
            railTop = view.findViewById(R.id.rail_top);
            railBottom = view.findViewById(R.id.rail_bottom);
            row = view.findViewById(R.id.items_row);
            title = (TextView) view.findViewById(R.id.items_title);
            duration = (TextView) view.findViewById(R.id.items_duration);
        }
    }

    public AdapterMusicMoveable(Context context, List<Music> songs, Fragment_Playlist fragment, ServiceMusic service, TextView subtitle) {
        mContext = context;
        mFragment = fragment;
        mService = service;
        mToolbarSubTitle = subtitle;
        mSongs = songs;
        showWholeQueue();
    }

    private void showWholeQueue() {
        mSongs = mService.playlistPlayed.getPlaylist();
    }

    /** Narrows the queue to songs whose title, artist or album contains the text; empty text shows it all. */
    public void getFilter(String text) {
        if (text == null || text.isEmpty()) {
            showWholeQueue();
        } else {
            String needle = text.toLowerCase();
            List<Music> matches = new ArrayList<>();
            for (Music song : new ArrayList<>(mService.playlistPlayed.getPlaylist())) {
                if (song.getAlbum().toLowerCase().contains(needle)
                        || song.getArtist().toLowerCase().contains(needle)
                        || song.getTitle().toLowerCase().contains(needle)) {
                    matches.add(song.getMusic());
                }
            }
            mSongs = matches;
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_music_moveable, parent, false));
    }

    private int indexOfPlaying() {
        long playingId = mService.musicPlayed.getMusic(mContext).getID();
        for (int i = 0; i < mSongs.size(); i++) {
            if (mSongs.get(i).getID() == playingId) {
                return i;
            }
        }
        return -1;
    }

    private static void resize(View view, int widthPx, int heightPx, int startMarginPx) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = widthPx;
        params.height = heightPx;
        ((ViewGroup.MarginLayoutParams) params).setMarginStart(startMarginPx);
        view.setLayoutParams(params);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int unused) {
        final int pos = holder.getAdapterPosition();
        final Music song = mSongs.get(pos).getMusic();
        holder.song = song;
        final int current = indexOfPlaying();
        float density = mContext.getResources().getDisplayMetrics().density;

        ViewGroup.LayoutParams rowParams = holder.row.getLayoutParams();
        if (pos == current) {
            lastSelectedPosition = pos;
            rowParams.height = (int) (64 * density);
            holder.row.setBackgroundResource(R.drawable.pl_row_now);
            holder.dot.setBackgroundResource(R.drawable.pl_dot_now);
            resize(holder.dot, (int) (22 * density), (int) (22 * density), (int) (24 * density));
            holder.title.setTextSize(15);
            holder.title.setTextColor(ACCENT);
            holder.duration.setTextColor(ACCENT);
            holder.root.setAlpha(1f);
        } else {
            boolean played = current >= 0 && pos < current;
            rowParams.height = (int) (56 * density);
            holder.row.setBackground(null);
            holder.dot.setBackgroundResource(played ? R.drawable.pl_dot_done : R.drawable.pl_dot);
            resize(holder.dot, (int) (10 * density), (int) (10 * density), (int) (30 * density));
            holder.title.setTextSize(14);
            holder.title.setTextColor(ContextCompat.getColor(mContext, R.color.colorWhite));
            holder.duration.setTextColor(ContextCompat.getColor(mContext, R.color.colorLight));
            holder.root.setAlpha(played ? 0.42f : 1f);
        }
        holder.row.setLayoutParams(rowParams);

        holder.railTop.setBackgroundColor(current >= 0 && pos <= current ? ACCENT : FAINT);
        holder.railBottom.setBackgroundColor(current >= 0 && pos < current ? ACCENT : FAINT);
        holder.railTop.setVisibility(pos == 0 ? View.INVISIBLE : View.VISIBLE);
        holder.railBottom.setVisibility(pos == mSongs.size() - 1 ? View.INVISIBLE : View.VISIBLE);

        String file = song.getFile();
        boolean unplugged = file != null && file.startsWith(UsbDrive.PREFIX) && !UsbDrive.get(mContext).isConnected();
        String artist = song.getArtist();
        if (unplugged) {
            artist += " · Unplugged";
        } else if (song.isOnDrive()) {
            artist += " · Drive";
        }
        holder.artist.setText(artist);
        holder.root.setAlpha(unplugged ? 0.4f : 1f);
        holder.title.setText(song.getTitle());
        holder.duration.setText(DateUtils.formatElapsedTime(song.getDuration()));

        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mService.musicPlayed.setPosition(holder.getAdapterPosition());
                boolean differentSong = mService.musicPlayed.getMusic(mContext).getID() != song.getID();
                mService.play(mContext, differentSong);
            }
        });
    }

    // ---- Called by the drag and swipe helper ------------------------------------------------

    void onItemDismiss(int position) {
        CurrentPlaylist queue = mService.playlistPlayed;
        if (queue.getPlaylistSize() <= 1) {
            notifyDataSetChanged();
            return;
        }
        queue.removeSong(position, mFragment);
        mSongs.remove(position);
        notifyItemRemoved(position);
        queue.setPlaylistName(mContext.getString(R.string.playlist_temp), mToolbarSubTitle);
    }

    void refreshNumbers() {
        if (getItemCount() > 0) {
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    boolean onItemMove(int from, int to) {
        if (from < to) {
            for (int i = from; i < to; i++) {
                Collections.swap(mSongs, i, i + 1);
            }
        } else {
            for (int i = from; i > to; i--) {
                Collections.swap(mSongs, i, i - 1);
            }
        }
        notifyItemMoved(from, to);

        // Keep the "now playing" position pointing at the same song.
        int playing = mService.musicPlayed.getPosition();
        if (from == playing) {
            mService.musicPlayed.setPosition(to);
        } else if (from < playing && to >= playing) {
            mService.musicPlayed.setPosition(playing - 1);
        } else if (from > playing && to <= playing) {
            mService.musicPlayed.setPosition(playing + 1);
        }
        mService.playlistPlayed.setPlaylistName(mContext.getString(R.string.playlist_temp), mToolbarSubTitle);
        return true;
    }
}
