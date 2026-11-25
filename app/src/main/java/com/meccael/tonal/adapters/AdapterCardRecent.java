package com.meccael.tonal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Fragment_Home_Carousel;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.artwork.ArtworkResolver;
import com.meccael.tonal.manager.Category;
import com.meccael.tonal.manager.Music;

/** The "Recently added" row on Home. The first cover is always the song that is playing. */
public class AdapterCardRecent extends RecyclerView.Adapter<AdapterCardRecent.ViewHolder> {
    public static int lastSelectedPosition = -1;

    private final Context mContext;
    private final Fragment_Home_Carousel mFragment;
    private final ServiceMusic mService;
    private final List<Music> mSongs;

    class ViewHolder extends RecyclerView.ViewHolder {
        final View root;
        final ImageView cover;
        final ImageView playingMark;
        Music song;

        ViewHolder(View view) {
            super(view);
            root = view;
            cover = (ImageView) view.findViewById(R.id.cover);
            playingMark = (ImageView) view.findViewById(R.id.playing);
        }
    }

    public AdapterCardRecent(Context context, List<Music> recent, ServiceMusic service, Fragment_Home_Carousel fragment) {
        mContext = context;
        mService = service;
        mFragment = fragment;
        mSongs = new ArrayList<>(recent);
        mSongs.add(0, service.musicPlayed.getMusic(context));
    }

    @Override
    public int getItemCount() {
        return mSongs.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list_card_recently_added, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.song = mSongs.get(holder.getAdapterPosition());
        long cover = holder.song.getCover();
        Glide.with(mContext)
                .load(ArtworkResolver.model(mContext, cover))
                .signature(ArtworkResolver.signature(mContext, cover))
                .placeholder(R.drawable.cover0)
                .centerCrop()
                .override(192, 192)
                .into(holder.cover);

        boolean playingElsewhere = holder.song.getID() == mService.musicPlayed.getMusic(mContext).getID() && position != 0;
        if (playingElsewhere) {
            lastSelectedPosition = holder.getAdapterPosition();
            holder.playingMark.setVisibility(View.VISIBLE);
        } else {
            mFragment.setCover(holder.cover);
            holder.playingMark.setVisibility(View.GONE);
        }

        holder.cover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCoverTapped(holder);
            }
        });
    }

    private void onCoverTapped(ViewHolder holder) {
        int position = holder.getAdapterPosition();
        if (position == 0) {
            mFragment.setCover(holder.cover);
            mFragment.openPlayer(holder.cover);
            return;
        }
        Category recent = mService.getRecentPlaylist();
        mService.musicPlayed.setPosition(position - 1);
        mService.playlistPlayed.newPlaylist(recent.getTitle(), 0L, recent.getCover(), recent.getSize(), recent.getDuration(),
                mService.getRecent().getPlaylistMusics());
        mService.play(mContext, true);
    }

    public void refreshRecentList() {
        mSongs.set(0, mService.musicPlayed.getMusic(mContext));
        notifyItemChanged(0);
        mFragment.refreshRecentTitleName();
    }
}
