package com.meccael.tonal.navigation;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import com.meccael.tonal.R;

/** The rows of the navigation drawer; the selected row gets an arrow and a highlight. */
public class NavigationDrawerAdapter extends RecyclerView.Adapter<NavigationDrawerAdapter.MyViewHolder> {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private final List<NavigationItem> mItems;
    public int selected_item = 0;

    class MyViewHolder extends RecyclerView.ViewHolder {
        final LinearLayout row;
        final TextView title;
        final ImageView icon;
        final ImageView arrow;

        MyViewHolder(View view) {
            super(view);
            row = (LinearLayout) view.findViewById(R.id.item);
            title = (TextView) view.findViewById(R.id.navigation_title);
            icon = (ImageView) view.findViewById(R.id.navigation_icon);
            arrow = (ImageView) view.findViewById(R.id.navigation_arrow);
        }
    }

    NavigationDrawerAdapter(Context context, List<NavigationItem> items) {
        mContext = context;
        mItems = items;
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(mInflater.inflate(R.layout.navigation_items, parent, false));
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        NavigationItem item = mItems.get(position);
        int color = ContextCompat.getColor(mContext, R.color.colorWhite);
        holder.title.setText(item.getTitle());
        holder.title.setTextColor(color);
        holder.icon.setImageResource(item.getIcon());
        holder.icon.setColorFilter(color);

        boolean selected = position == selected_item;
        float alpha = selected ? 1f : 0.85f;
        holder.title.setAlpha(alpha);
        holder.icon.setAlpha(alpha);
        holder.arrow.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        holder.row.setBackgroundResource(selected ? R.drawable.xml_navigation_select : 0);
    }
}
