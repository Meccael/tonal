package com.meccael.tonal.bottom_menu;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.view.SupportMenuInflater;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.R;

/**
 * A sheet that slides up from the bottom of the screen with a title and a list of actions.
 * Usage: create, optionally set a title, inflate a menu resource, set a listener, show.
 */
public class BottomDialog {
    private final CustomDialog mDialog;

    public interface OnItemSelectedListener {
        /** Return true to close the sheet. */
        boolean onItemSelected(int itemId);
    }

    public BottomDialog(Context context) {
        mDialog = new CustomDialog(context);
    }

    public void title(int resId) { mDialog.title(resId); }
    public void title(String text) { mDialog.title(text); }
    public void inflateMenu(int menuRes) { mDialog.inflateMenu(menuRes); }
    public void setOnItemSelectedListener(OnItemSelectedListener listener) { mDialog.setListener(listener); }
    public void cancelable(boolean cancelable) { mDialog.setCancelable(cancelable); }
    public void canceledOnTouchOutside(boolean cancel) { mDialog.setCanceledOnTouchOutside(cancel); }
    public void show() { mDialog.show(); }
    public void dismiss() { mDialog.dismiss(); }

    // ---- Helpers ----------------------------------------------------------------------------

    private static int dp(Context context, int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics()));
    }

    /** Height of the navigation bar if it is showing, so the sheet's last row is not hidden behind it. */
    private static int navigationBarHeight(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point usable = new Point();
        Point real = new Point();
        display.getSize(usable);
        display.getRealSize(real);
        if (usable.x >= real.x && usable.y >= real.y) {
            return 0;
        }
        Resources res = context.getResources();
        int id = res.getIdentifier("navigation_bar_height", "dimen", "android");
        return id > 0 ? res.getDimensionPixelSize(id) : 0;
    }

    private class CustomDialog extends Dialog implements View.OnClickListener {
        private final LinearLayout mList;
        private final int mIconSize;
        private final int mPadding;
        private OnItemSelectedListener mListener;

        CustomDialog(Context context) {
            super(context);
            mIconSize = dp(getContext(), 24);
            mPadding = dp(getContext(), 12);

            float corner = dp(getContext(), 28);
            GradientDrawable sheet = new GradientDrawable();
            sheet.setColor(ContextCompat.getColor(getContext(), R.color.md_theme_dark_surface));
            sheet.setCornerRadii(new float[]{corner, corner, corner, corner, 0f, 0f, 0f, 0f});

            mList = new LinearLayout(getContext());
            mList.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            mList.setOrientation(LinearLayout.VERTICAL);
            mList.setBackground(sheet);
            mList.setPadding(0, mPadding, 0, mPadding + navigationBarHeight(context));

            ScrollView scroll = new ScrollView(getContext());
            scroll.addView(mList);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(scroll, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            setCancelable(true);
            setCanceledOnTouchOutside(true);

            Window window = getWindow();
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(0));
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        void setListener(OnItemSelectedListener listener) {
            mListener = listener;
        }

        void title(int resId) {
            title(getContext().getString(resId));
        }

        void title(String text) {
            TextView view = new TextView(getContext());
            view.setText(text);
            view.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_dark_onSurface));
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f);
            view.setPadding(dp(getContext(), 16) + mPadding, mPadding, mPadding, mPadding);
            mList.addView(view);
        }

        void inflateMenu(int menuRes) {
            MenuBuilder menu = new MenuBuilder(getContext());
            new SupportMenuInflater(getContext()).inflate(menuRes, menu);
            List<Item> items = new ArrayList<>();
            for (int i = 0; i < menu.size(); i++) {
                MenuItem entry = menu.getItem(i);
                Item item = new Item();
                item.setId(entry.getItemId());
                item.setIcon(entry.getIcon());
                item.setTitle(entry.getTitle().toString());
                items.add(item);
            }
            for (Item item : items) {
                addRow(item);
            }
        }

        private void addRow(Item item) {
            int rowHeight = mIconSize + mPadding * 2;
            TextView row = new TextView(getContext());
            row.setId(item.getId());
            row.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, rowHeight));
            row.setMaxLines(1);
            row.setEllipsize(TextUtils.TruncateAt.END);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setText(item.getTitle());
            row.setTypeface(Typeface.DEFAULT_BOLD);
            row.setTextColor(ContextCompat.getColor(getContext(), R.color.md_theme_dark_onSurface));
            row.setOnClickListener(this);

            TypedValue ripple = new TypedValue();
            getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
            row.setBackgroundResource(ripple.resourceId);

            if (item.getIcon() != null) {
                row.setCompoundDrawablesWithIntrinsicBounds(tintedIcon(item.getIcon()), null, null, null);
                row.setCompoundDrawablePadding(mPadding);
                row.setPadding(mPadding, mPadding, mPadding, mPadding);
            } else {
                row.setPadding(rowHeight, mPadding, mPadding, mPadding);
            }
            mList.addView(row);
        }

        /** The icon scaled to a standard size and tinted to match the text. */
        private Drawable tintedIcon(Drawable icon) {
            Bitmap source;
            if (icon instanceof BitmapDrawable) {
                source = ((BitmapDrawable) icon).getBitmap();
            } else {
                source = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
                icon.setBounds(0, 0, mIconSize, mIconSize);
                icon.draw(new Canvas(source));
            }
            Drawable scaled = new BitmapDrawable(getContext().getResources(), Bitmap.createScaledBitmap(source, mIconSize, mIconSize, true));
            Drawable.ConstantState state = scaled.getConstantState();
            if (state != null) {
                scaled = state.newDrawable();
            }
            Drawable tinted = DrawableCompat.wrap(scaled).mutate();
            DrawableCompat.setTint(tinted, ContextCompat.getColor(getContext(), R.color.md_theme_dark_onSurface));
            return tinted;
        }

        @Override
        public void onClick(View view) {
            if (mListener != null && mListener.onItemSelected(view.getId())) {
                dismiss();
            }
        }
    }
}
