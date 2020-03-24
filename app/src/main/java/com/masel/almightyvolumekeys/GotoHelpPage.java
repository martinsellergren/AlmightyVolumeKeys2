package com.masel.almightyvolumekeys;

import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

import com.masel.rec_utils.RecUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

class GotoHelpPage {

    private static Map<String, String> sectionToUrlMap = new HashMap<>();
    static {
        sectionToUrlMap.put("why activate", "https://sites.google.com/view/almightyvolumekeys-help#h.ak1tvc342tsx");
        sectionToUrlMap.put("how long press?", "https://sites.google.com/view/almightyvolumekeys-help#h.q0r3knihzfor");
        sectionToUrlMap.put("battery usage", "https://sites.google.com/view/almightyvolumekeys-help#h.yi56pdvw22gu");
    }

    static void gotoHelp(AppCompatActivity activity, String section) {
        String defaultUrl = "https://sites.google.com/view/almightyvolumekeys-help";
        if (section != null && sectionToUrlMap.containsKey(section)) defaultUrl = sectionToUrlMap.get(section);

        String lang = Locale.getDefault().getLanguage();
        String translatedUrl = String.format("https://translate.google.com/translate?js=n&sl=en&tl=%s&u=%s", lang, defaultUrl);

        Intent engIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(defaultUrl));
        engIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        Intent translatedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(translatedUrl));
        translatedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);

        if (lang.equals("en")) {
            activity.startActivity(engIntent);
        }
        else {
            RecUtils.showDialog(activity,
                    null,
                    String.format("Auto-translate to: <b>%s</b>?", Locale.getDefault().getDisplayLanguage()),
                    Locale.getDefault().getDisplayLanguage(),
                    () -> activity.startActivity(translatedIntent),
                    "English",
                    () -> activity.startActivity(engIntent));
        }
    }

    static void gotoHelp(AppCompatActivity activity) {
        gotoHelp(activity, null);
    }
}
