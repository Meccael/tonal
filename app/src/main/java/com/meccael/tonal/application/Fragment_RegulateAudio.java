package com.meccael.tonal.application;

import android.content.Context;
import android.graphics.Typeface;
import android.media.audiofx.BassBoost;
import android.media.audiofx.Equalizer;
import android.media.audiofx.LoudnessEnhancer;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import java.util.ArrayList;
import java.util.List;
import com.meccael.tonal.R;
import com.meccael.tonal.audio.EqualizerStore;
import com.meccael.tonal.views.EqKnobView;
import com.meccael.tonal.views.EqSliderView;

/** The equalizer: a bass knob, preset chips, one fader per band and a preamp fader. */
public class Fragment_RegulateAudio extends Fragment {
    private CallbacksInterface mActivity;
    private Equalizer equalizer;
    private BassBoost bassBoost;
    private LoudnessEnhancer loudness;

    private TextView power;
    private EqKnobView knob;
    private TextView knobLabel;
    private TextView knobValue;
    private LinearLayout presetsRow;
    private LinearLayout layoutBands;
    private TextView txtNoAudio;

    private final List<TextView> presetChips = new ArrayList<>();
    private final List<EqSliderView> bandSliders = new ArrayList<>();
    private final List<TextView> bandValues = new ArrayList<>();
    private short minLevel;
    private short maxLevel;

    /* JADX WARN: Multi-variable type inference failed */
    @Override // androidx.fragment.app.Fragment
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mActivity = (CallbacksInterface) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        View view = layoutInflater.inflate(R.layout.fragment_equalizer, viewGroup, false);
        this.mActivity.setFragment(40);
        this.mActivity.hideSearchIcon(true);
        this.mActivity.setToolbarText(getString(R.string.menu_equalizer), "Audio Equalizer");
        this.mActivity.refreshNavigation(3);

        power = view.findViewById(R.id.power);
        knob = view.findViewById(R.id.knob);
        knobLabel = view.findViewById(R.id.knob_label);
        knobValue = view.findViewById(R.id.knob_value);
        presetsRow = view.findViewById(R.id.presets_row);
        layoutBands = view.findViewById(R.id.layout_bands);
        txtNoAudio = view.findViewById(R.id.txt_no_audio);

        setupEqualizer();
        return view;
    }

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private int color(int res) {
        return ContextCompat.getColor(requireContext(), res);
    }

    /** Called when the music service connects after this screen was already built (for example after a rotation). */
    void refreshForService() {
        if (getView() != null) {
            setupEqualizer();
        }
    }

    private void setupEqualizer() {
        ServiceMusic serviceMusic = mActivity != null ? mActivity.getServiceMusic() : null;
        if (serviceMusic != null) {
            serviceMusic.ensureEffects();
            equalizer = serviceMusic.getEqualizer();
            bassBoost = serviceMusic.getBassBoost();
            loudness = serviceMusic.getLoudness();
        }

        if (equalizer == null) {
            power.setVisibility(View.GONE);
            knob.setVisibility(View.GONE);
            knobLabel.setVisibility(View.GONE);
            knobValue.setVisibility(View.GONE);
            presetsRow.setVisibility(View.GONE);
            layoutBands.setVisibility(View.GONE);
            txtNoAudio.setVisibility(View.VISIBLE);
            return;
        }
        txtNoAudio.setVisibility(View.GONE);
        power.setVisibility(View.VISIBLE);
        knob.setVisibility(View.VISIBLE);
        knobLabel.setVisibility(View.VISIBLE);
        knobValue.setVisibility(View.VISIBLE);
        presetsRow.setVisibility(View.VISIBLE);
        layoutBands.setVisibility(View.VISIBLE);

        setupPower();
        setupKnob();
        setupPresets();
        setupBands();
        refreshEnabledLook();
    }

    // ---- power ----
    private void setupPower() {
        power.setOnClickListener(v -> {
            boolean on = !EqualizerStore.enabled(requireContext());
            EqualizerStore.setEnabled(requireContext(), on);
            try { if (equalizer != null) equalizer.setEnabled(on); } catch (Exception e) { e.printStackTrace(); }
            try { if (bassBoost != null) bassBoost.setEnabled(on); } catch (Exception e) { e.printStackTrace(); }
            try { if (loudness != null) loudness.setEnabled(on); } catch (Exception e) { e.printStackTrace(); }
            refreshEnabledLook();
        });
    }

    private void refreshEnabledLook() {
        boolean on = EqualizerStore.enabled(requireContext());
        power.setText(on ? "ON" : "OFF");
        power.setAlpha(on ? 1f : 0.6f);
        power.setBackgroundResource(on ? R.drawable.pl_pill_active : R.drawable.pl_pill);
        float a = on ? 1f : 0.45f;
        knob.setAlpha(a);
        layoutBands.setAlpha(a);
        presetsRow.setAlpha(a);
    }

    // ---- bass knob ----
    private void setupKnob() {
        boolean supported = false;
        try { supported = bassBoost != null && bassBoost.getStrengthSupported(); } catch (Exception ignored) {}
        if (!supported) {
            knob.setEnabled(false);
            knob.setAlpha(0.4f);
            knobLabel.setText("BASS (not supported on this device)");
            knobValue.setText("");
            return;
        }
        int strength = EqualizerStore.bass(requireContext());
        knob.setValue(strength / 1000f);
        knobValue.setText(Math.round(strength / 10f) + "%");
        knob.setListener((value, fromUser) -> {
            int s = Math.round(value * 1000f);
            try { bassBoost.setStrength((short) s); } catch (Exception e) { e.printStackTrace(); }
            EqualizerStore.setBass(requireContext(), s);
            knobValue.setText(Math.round(s / 10f) + "%");
        });
    }

    // ---- presets ----
    private void setupPresets() {
        presetsRow.removeAllViews();
        presetChips.clear();
        short n = equalizer.getNumberOfPresets();
        for (short p = 0; p < n; p++) {
            final short preset = p;
            TextView chip = new TextView(requireContext());
            chip.setText(equalizer.getPresetName(p));
            chip.setTextSize(14);
            chip.setPadding(dp(14), dp(8), dp(14), dp(11));
            chip.setGravity(android.view.Gravity.CENTER);
            chip.setOnClickListener(v -> {
                try {
                    equalizer.usePreset(preset);
                    saveLevelsFromEqualizer();
                    EqualizerStore.setPreset(requireContext(), preset);
                    refreshBandSliders();
                    refreshPresetChips();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            presetsRow.addView(chip);
            presetChips.add(chip);
        }
        refreshPresetChips();
    }

    private void refreshPresetChips() {
        int selected = EqualizerStore.preset(requireContext());
        for (int i = 0; i < presetChips.size(); i++) {
            TextView c = presetChips.get(i);
            boolean on = i == selected;
            c.setTextColor(color(on ? R.color.colorWhite : R.color.colorLight));
            c.setTypeface(null, on ? Typeface.BOLD : Typeface.NORMAL);
            c.setBackgroundResource(on ? R.drawable.pl_pill_active : 0);
        }
    }

    private void saveLevelsFromEqualizer() {
        short bands = equalizer.getNumberOfBands();
        short[] levels = new short[bands];
        for (short i = 0; i < bands; i++) levels[i] = equalizer.getBandLevel(i);
        EqualizerStore.saveLevels(requireContext(), levels);
    }

    // ---- band faders ----
    private void setupBands() {
        layoutBands.removeAllViews();
        bandSliders.clear();
        bandValues.clear();
        short[] range = equalizer.getBandLevelRange();
        minLevel = range[0];
        maxLevel = range[1];
        short bands = equalizer.getNumberOfBands();
        for (short i = 0; i < bands; i++) {
            final short band = i;
            int freq = equalizer.getCenterFreq(band) / 1000;
            String name = freq >= 1000 ? String.format("%.1f kHz", freq / 1000.0) : freq + " Hz";
            final EqSliderView slider = new EqSliderView(requireContext());
            slider.setBipolar(true);
            final TextView value = addColumn(name, slider);
            slider.setListener((v, fromUser) -> {
                short level = (short) Math.round(minLevel + v * (maxLevel - minLevel));
                try { equalizer.setBandLevel(band, level); } catch (Exception e) { e.printStackTrace(); }
                value.setText(db(level));
                try { saveLevelsFromEqualizer(); } catch (Exception e) { e.printStackTrace(); }
                EqualizerStore.setPreset(requireContext(), -1);
                refreshPresetChips();
            });
            bandSliders.add(slider);
            bandValues.add(value);
        }
        // preamp
        final EqSliderView preamp = new EqSliderView(requireContext());
        preamp.setBipolar(false);
        final TextView preampValue = addColumn("PREAMP", preamp);
        boolean ok = loudness != null;
        int gain = EqualizerStore.preamp(requireContext());
        preamp.setValue(gain / (float) EqualizerStore.PREAMP_MAX_MB);
        preampValue.setText(db(gain));
        if (!ok) {
            preamp.setEnabled(false);
            preamp.setAlpha(0.4f);
        }
        preamp.setListener((v, fromUser) -> {
            int g = Math.round(v * EqualizerStore.PREAMP_MAX_MB);
            try { if (loudness != null) loudness.setTargetGain(g); } catch (Exception e) { e.printStackTrace(); }
            EqualizerStore.setPreamp(requireContext(), g);
            preampValue.setText(db(g));
        });
        refreshBandSliders();
    }

    private void refreshBandSliders() {
        for (short i = 0; i < bandSliders.size(); i++) {
            short level = equalizer.getBandLevel(i);
            bandSliders.get(i).setValue((level - minLevel) / (float) (maxLevel - minLevel));
            bandValues.get(i).setText(db(level));
        }
    }

    private String db(int millibels) {
        return String.format("%+.1f dB", millibels / 100.0f);
    }

    /** One column: name on top, fader in the middle, value underneath. Returns the value label. */
    private TextView addColumn(String name, EqSliderView slider) {
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView top = new TextView(requireContext());
        top.setText(name);
        top.setTextSize(11);
        top.setGravity(Gravity.CENTER);
        top.setTextColor(color(R.color.colorLight));
        col.addView(top, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        col.addView(slider, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(190)));

        TextView bottom = new TextView(requireContext());
        bottom.setTextSize(11);
        bottom.setGravity(Gravity.CENTER);
        bottom.setTextColor(color(R.color.colorWhite));
        col.addView(bottom, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        layoutBands.addView(col);
        return bottom;
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Application_Extend.getInstance().trackScreenView("Equalizer");
        setupEqualizer();
    }
}
