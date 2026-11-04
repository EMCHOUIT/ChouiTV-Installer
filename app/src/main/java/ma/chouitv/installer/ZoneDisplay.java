package ma.chouitv.installer;

import org.json.JSONObject;

/**
 * What the family reads about a time zone. The TV keeps «Africa/Abidjan» (GMT all year) for Moroccan cities, because its own time-zone data still gives
 * «Africa/Casablanca» one hour more; ChouiTV turns Casablanca into Abidjan by itself. For a settings file of Casablanca the screens say «Africa/Casablanca»,
 * the name people know. Only what is shown changes, never what is sent to the TV. Pure, tested on the JVM (ZoneDisplayTest).
 */
final class ZoneDisplay {
    private ZoneDisplay() {}

    static boolean isCasablanca(String city) {
        if (city == null) return false;
        String c = city.trim();
        return c.equals("الدار البيضاء") || c.equalsIgnoreCase("Casablanca");
    }

    /** {@code text} (the changes a TV announces), with Africa/Abidjan said Africa/Casablanca when the settings file is the one of Casablanca. */
    static String show(String text, String settingsJson) {
        if (text == null) return "";
        if (settingsJson == null) return text;
        try {
            JSONObject settings = new JSONObject(settingsJson).optJSONObject("settings");
            if (settings != null && isCasablanca(settings.optString("cityName", ""))) return text.replace("Africa/Abidjan", "Africa/Casablanca");
        } catch (Exception ignored) { }
        return text;
    }
}
