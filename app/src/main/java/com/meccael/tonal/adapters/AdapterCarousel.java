package com.meccael.tonal.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.viewpager.widget.PagerAdapter;
import com.bumptech.glide.Glide;
import com.meccael.tonal.R;
import com.meccael.tonal.application.Fragment_Home_Carousel;
import com.meccael.tonal.application.ServiceMusic;
import com.meccael.tonal.artwork.ArtworkResolver;
import com.meccael.tonal.manager.Category;

/** The swipeable stack of Home category cards. Tapping the front card opens it; tapping a side card turns the stack. */
public class AdapterCarousel extends PagerAdapter {
    private final Context mContext;
    private final Fragment_Home_Carousel mFragment;
    private final LayoutInflater mInflater;
    private final ServiceMusic mService;
    private final int mCoverSize;

    public AdapterCarousel(Context context, Fragment_Home_Carousel fragment, ServiceMusic service, int width) {
        mContext = context;
        mFragment = fragment;
        mInflater = LayoutInflater.from(context);
        mService = service;
        mCoverSize = width / 2;
    }

    @Override
    public int getCount() {
        return mService.getCategoriesSize();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        View card = mInflater.inflate(R.layout.item_caroussel, container, false);
        final ImageView picture = (ImageView) card.findViewById(R.id.picture);
        final TextView title = (TextView) card.findViewById(R.id.title);
        final ImageButton more = (ImageButton) card.findViewById(R.id.more);

        Category category = mService.getCategory(mContext, position);
        Glide.with(mContext)
                .load(ArtworkResolver.model(mContext, category.getCover()))
                .signature(ArtworkResolver.signature(mContext, category.getCover()))
                .override(mCoverSize, mCoverSize)
                .error(category.getDefaultCover())
                .into(picture);
        title.setText(category.getTitle());

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCardTapped(position, picture, title, more);
            }
        });
        container.addView(card);
        return card;
    }

    private void onCardTapped(int position, ImageView picture, TextView title, ImageButton more) {
        int count = getCount();
        int index = position % count;
        int current = mFragment.getCardPosition();
        if (index == current) {
            String name = mService.getCategory(mContext, index).getTitle();
            if (index == 0) {
                mFragment.openMusics(index, name, picture, title);
            } else if (index < count) {
                mFragment.openPlaylists(index, name, picture, title, more);
            }
            return;
        }
        // A side card: turn the stack toward it, the short way round.
        boolean forward = current < index;
        boolean wraps = (current == 0 && position == count - 1) || (current == count - 1 && position == 0);
        mFragment.scrollTo(wraps ? !forward : forward);
    }
}
