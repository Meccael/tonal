package com.meccael.tonal.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;

import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Fragment_Home_Playlists;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.manager.CurrentPlaylist;
import com.meccael.tonal.manager.Playlist;
import com.meccael.tonal.manager.Playlists;

/* JADX INFO: loaded from: classes.dex */
public class AdapterCardPlaylist extends RecyclerView.Adapter<AdapterCardPlaylist.MyViewHolder> {
    private int mCategoryOpened;
    private Context mContext;
    private Fragment_Home_Playlists mFragment;
    private List<Playlist> mList;
    private ServiceMusic mService;

    class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        View mView;
        ImageButton more;
        TextView title;
        TextView count;

        MyViewHolder(View view) {
            super(view);
            this.mView = view;
            this.cover = (ImageView) view.findViewById(R.id.cover);
            this.title = (TextView) view.findViewById(R.id.title);
            this.more = (ImageButton) view.findViewById(R.id.more);
            this.count = (TextView) view.findViewById(R.id.count);
        }
    }

    public AdapterCardPlaylist(Context context, int i, List<Playlist> list, Fragment_Home_Playlists fragment_Home_Playlists, ServiceMusic serviceMusic) {
        this.mContext = context;
        this.mList = list;
        this.mFragment = fragment_Home_Playlists;
        this.mCategoryOpened = i;
        this.mService = serviceMusic;
    }

    private Uri getAlbumCoverUri(long j) {
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), j);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public int getItemCount() {
        return this.mList.size();
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public void onBindViewHolder(final MyViewHolder myViewHolder, int i) {
        final Playlist playlist = this.mList.get(i);
        Glide.with(this.mContext)
                .load(com.meccael.tonal.artwork.ArtworkResolver.model(this.mContext, playlist.getPlaylistCover()))
                .signature(com.meccael.tonal.artwork.ArtworkResolver.signature(this.mContext, playlist.getPlaylistCover()))
                .transition(DrawableTransitionOptions.withCrossFade(ItemTouchHelper.Callback.DEFAULT_SWIPE_ANIMATION_DURATION))
                .apply(new RequestOptions()
                .centerCrop()
                        .error(R.drawable.cover0)
                        .override(640, 640)
                .transform(this.mCategoryOpened == 3 || this.mCategoryOpened == 4
                        ? new com.bumptech.glide.load.resource.bitmap.CircleCrop()
                        : new RoundedCornersTransformation(this.mContext, 8)))
                .into(myViewHolder.cover);
        if (i == 0 && Build.VERSION.SDK_INT >= 24) {
            myViewHolder.cover.setTransitionName(this.mContext.getString(R.string.transition_card_cover));
            myViewHolder.title.setTransitionName(this.mContext.getString(R.string.transition_card_title));
            myViewHolder.more.setTransitionName(this.mContext.getString(R.string.transition_card_button));
        }
        myViewHolder.title.setText(playlist.getPlaylistName());
        myViewHolder.more.setOnClickListener(new View.OnClickListener() { // from class: com.meccael.tonal.adapters.AdapterCardPlaylist.1
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                if (AdapterCardPlaylist.this.mCategoryOpened == 5) {
                    PopupMenu popupMenu = new PopupMenu(myViewHolder.more.getContext(), myViewHolder.more);
                    popupMenu.getMenuInflater().inflate(R.menu.menu_playlists, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.meccael.tonal.adapters.AdapterCardPlaylist.1.1
                        @Override // androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
                        public boolean onMenuItemClick(MenuItem menuItem) {
                            Playlist playlist2 = AdapterCardPlaylist.this.mService.getCategory(AdapterCardPlaylist.this.mContext, AdapterCardPlaylist.this.mCategoryOpened).getPlaylists().get(myViewHolder.getAdapterPosition());
                            if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_open))) {
                                AdapterCardPlaylist.this.mFragment.openPlaylist(AdapterCardPlaylist.this.mCategoryOpened, myViewHolder.getAdapterPosition(), playlist.getPlaylistName(), myViewHolder.cover, myViewHolder.title);
                            } else if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_play))) {
                                AdapterCardPlaylist.this.mService.playlistPlayed = new CurrentPlaylist();
                                AdapterCardPlaylist.this.mService.playlistPlayed.newPlaylist(playlist2.getPlaylistName(), playlist2.getPlaylistID(), playlist2.getPlaylistCover(), playlist2.getPlaylistSize(), playlist2.getPlaylistDuration(), playlist2.getPlaylistMusics());
                                AdapterCardPlaylist.this.mService.musicPlayed.setPosition(0);
                                AdapterCardPlaylist.this.mService.play(AdapterCardPlaylist.this.mContext, true);
                            } else if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_current_add))) {
                                AdapterCardPlaylist.this.mService.playlistPlayed.addPlaylist(playlist2.getPlaylistMusics());
                            } else if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_delete))) {
                                Playlists.delete(AdapterCardPlaylist.this.mContext, playlist.getPlaylistName(), AdapterCardPlaylist.this.mService);
                                AdapterCardPlaylist.this.mList = AdapterCardPlaylist.this.mService.getCategory(AdapterCardPlaylist.this.mContext, 5).getPlaylists();
                                AdapterCardPlaylist.this.notifyDataSetChanged();
                                AdapterCardPlaylist.this.mFragment.applySort();
                            }
                            return true;
                        }
                    });
                    popupMenu.show();
                    return;
                }
                PopupMenu popupMenu2 = new PopupMenu(myViewHolder.more.getContext(), myViewHolder.more);
                popupMenu2.getMenuInflater().inflate(R.menu.menu_cards, popupMenu2.getMenu());
                popupMenu2.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() { // from class: com.meccael.tonal.adapters.AdapterCardPlaylist.1.2
                    @Override // androidx.appcompat.widget.PopupMenu.OnMenuItemClickListener
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        Playlist playlist2 = AdapterCardPlaylist.this.mService.getCategory(AdapterCardPlaylist.this.mContext, AdapterCardPlaylist.this.mCategoryOpened).getPlaylists().get(myViewHolder.getAdapterPosition());
                        if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_open))) {
                            AdapterCardPlaylist.this.mFragment.openPlaylist(AdapterCardPlaylist.this.mCategoryOpened, myViewHolder.getAdapterPosition(), playlist.getPlaylistName(), myViewHolder.cover, myViewHolder.title);
                        } else if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_play))) {
                            AdapterCardPlaylist.this.mService.playlistPlayed = new CurrentPlaylist();
                            AdapterCardPlaylist.this.mService.playlistPlayed.newPlaylist(playlist2.getPlaylistName(), playlist2.getPlaylistID(), playlist2.getPlaylistCover(), playlist2.getPlaylistSize(), playlist2.getPlaylistDuration(), playlist2.getPlaylistMusics());
                            AdapterCardPlaylist.this.mService.musicPlayed.setPosition(0);
                            AdapterCardPlaylist.this.mService.play(AdapterCardPlaylist.this.mContext, true);
                        } else if (menuItem.getTitle().equals(AdapterCardPlaylist.this.mContext.getString(R.string.playlist_current_add))) {
                            AdapterCardPlaylist.this.mService.playlistPlayed.addPlaylist(playlist2.getPlaylistMusics());
                        } else if (menuItem.getItemId() == R.id.artwork_change) {
                            com.meccael.tonal.artwork.ArtworkPicker.show(AdapterCardPlaylist.this.mContext, playlist.getPlaylistCover(), new Runnable() {
                                @Override
                                public void run() {
                                    AdapterCardPlaylist.this.notifyDataSetChanged();
                                }
                            });
                        }
                        return true;
                    }
                });
                popupMenu2.show();
            }
        });
        myViewHolder.cover.setOnClickListener(new View.OnClickListener() { // from class: com.meccael.tonal.adapters.AdapterCardPlaylist.2
            @Override // android.view.View.OnClickListener
            public void onClick(View view) {
                AdapterCardPlaylist.this.mFragment.openPlaylist(AdapterCardPlaylist.this.mCategoryOpened, myViewHolder.getAdapterPosition(), playlist.getPlaylistName(), myViewHolder.cover, myViewHolder.title);
            }
        });
        if (this.mCategoryOpened == 2) {
            styleFolder(myViewHolder, playlist, i);
        } else if (this.mCategoryOpened == 3) {
            setCount(myViewHolder, playlist);
            setOpenListener(myViewHolder, playlist, 2);
        } else if (this.mCategoryOpened == 4) {
            styleGenre(myViewHolder, playlist);
        } else if (this.mCategoryOpened == 5) {
            setCount(myViewHolder, playlist);
            setOpenListener(myViewHolder, playlist, 0);
        }
    }

    private void setCount(MyViewHolder holder, Playlist playlist) {
        if (holder.count != null) {
            holder.count.setText(this.mContext.getString(R.string.folder_tracks, (int) playlist.getPlaylistSize()));
        }
    }

    /** mode 0: open straight away. mode 2: roll the artist into the spotlight first, then open. */
    private void setOpenListener(final MyViewHolder holder, final Playlist playlist, final int mode) {
        View.OnClickListener open = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mode == 2) {
                    android.view.ViewParent parent = holder.mView.getParent();
                    if (parent instanceof RecyclerView
                            && !com.meccael.tonal.views.ArtistWheelEffect.bringToFront((RecyclerView) parent, holder.mView)) {
                        return;
                    }
                }
                AdapterCardPlaylist.this.mFragment.openPlaylist(AdapterCardPlaylist.this.mCategoryOpened, holder.getAdapterPosition(), playlist.getPlaylistName(), holder.cover, holder.title);
            }
        };
        holder.mView.setOnClickListener(open);
        holder.cover.setOnClickListener(open);
        holder.mView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return holder.more.performClick();
            }
        });
    }

    private long[] mGenreCuts;

    /** Genres: bubble size (small, medium, large) by track count; tap opens, long-press shows the menu. */
    private void styleGenre(MyViewHolder holder, Playlist playlist) {
        if (this.mGenreCuts == null) {
            long[] sizes = new long[this.mList.size()];
            for (int k = 0; k < sizes.length; k++) sizes[k] = this.mList.get(k).getPlaylistSize();
            java.util.Arrays.sort(sizes);
            this.mGenreCuts = sizes.length == 0 ? new long[]{0, 0}
                    : new long[]{sizes[sizes.length / 3], sizes[(sizes.length * 2) / 3]};
        }
        long n = playlist.getPlaylistSize();
        int dp = n >= this.mGenreCuts[1] ? 124 : (n >= this.mGenreCuts[0] ? 108 : 90);
        float density = this.mContext.getResources().getDisplayMetrics().density;
        View bubble = holder.mView.findViewById(R.id.bubble);
        if (bubble != null) {
            android.view.ViewGroup.LayoutParams lp = bubble.getLayoutParams();
            lp.width = (int) (dp * density);
            lp.height = (int) (dp * density);
            bubble.setLayoutParams(lp);
        }
        View ring = holder.mView.findViewById(R.id.bubble_ring);
        if (ring != null) {
            ring.setBackgroundResource(dp >= 124 ? R.drawable.bubble_ring_big : R.drawable.bubble_ring);
        }
        setCount(holder, playlist);
        setOpenListener(holder, playlist, 0);
    }

    /** Folders (category 2): a rail row with the track count; tap opens, long-press shows the menu. */
    private void styleFolder(final MyViewHolder holder, final Playlist playlist, int position) {
        setCount(holder, playlist);
        setOpenListener(holder, playlist, 0);
    }

    @Override // androidx.recyclerview.widget.RecyclerView.Adapter
    public MyViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        int layout;
        switch (this.mCategoryOpened) {
            case 2: layout = R.layout.item_list_card_folder; break;
            case 3: layout = R.layout.item_list_card_artist; break;
            case 4: layout = R.layout.item_list_card_genre; break;
            case 5: layout = R.layout.item_list_card_cassette; break;
            default: layout = R.layout.item_list_card_playlist; break;
        }
        return new MyViewHolder(LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false));
    }
}
