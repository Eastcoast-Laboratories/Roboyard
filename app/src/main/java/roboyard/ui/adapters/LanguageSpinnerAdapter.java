package roboyard.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

import roboyard.eclabs.R;

/**
 * Custom spinner adapter for language selection with flags
 */
public class LanguageSpinnerAdapter extends ArrayAdapter<String> {
    
    private final LayoutInflater inflater;
    private final Map<String, Integer> languageFlags;
    
    public LanguageSpinnerAdapter(@NonNull Context context, @NonNull List<String> languages) {
        super(context, android.R.layout.simple_spinner_dropdown_item, languages);
        this.inflater = LayoutInflater.from(context);
        
        // Map language names to flag drawables using native language names
        languageFlags = new HashMap<>();
        languageFlags.put("English", R.drawable.ic_flag_us);
        languageFlags.put("Deutsch", R.drawable.ic_flag_de);
        languageFlags.put("Français", R.drawable.ic_flag_fr);
        languageFlags.put("Español", R.drawable.ic_flag_es);
        languageFlags.put("中文", R.drawable.ic_flag_cn);
        languageFlags.put("한국어", R.drawable.ic_flag_kr);
        languageFlags.put("日本語", R.drawable.ic_flag_jp);
        languageFlags.put("Português (Brasil)", R.drawable.ic_flag_br);
        languageFlags.put("Same as app language", R.drawable.icon_10_gear); // Use gear icon for "Same as app"
    }
    
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }
    
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return createView(position, convertView, parent);
    }
    
    private View createView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = inflater.inflate(R.layout.language_spinner_item, parent, false);
        }
        
        TextView languageText = view.findViewById(R.id.language_text);
        ImageView flagIcon = view.findViewById(R.id.flag_icon);
        
        String language = getItem(position);
        languageText.setText(language);
        
        // Set flag icon
        Integer flagRes = languageFlags.get(language);
        if (flagRes != null) {
            flagIcon.setImageResource(flagRes);
            flagIcon.setVisibility(View.VISIBLE);
        } else {
            flagIcon.setVisibility(View.GONE);
        }
        
        return view;
    }
}
