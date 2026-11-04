package ma.chouitv.installer;

import org.json.JSONObject;

/** What a saved settings card says: the TV and a short date (never a version number or an ISO date), and the city of a settings file. Pure, tested on the JVM (ProfileLabelTest). */
final class ProfileLabel {
    private ProfileLabel() {}

    private static final String[] FR = {"janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc."};
    private static final String[] EN = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    private static final String[] AR = {"يناير", "فبراير", "مارس", "أبريل", "ماي", "يونيو", "يوليوز", "غشت", "شتنبر", "أكتوبر", "نونبر", "دجنبر"};

    /** «TV · 28 oct.»: the date is the first ten characters «yyyy-MM-dd» of what the store keeps. */
    static String meta(String tv, String date, String lang) {
        String d = shortDate(date, lang);
        String t = tv == null ? "" : tv.trim();
        if (t.isEmpty()) return d;
        return d.isEmpty() ? t : t + " · " + d;
    }

    private static String shortDate(String date, String lang) {
        if (date == null || date.length() < 10 || date.charAt(4) != '-' || date.charAt(7) != '-') return "";
        try {
            int m = Integer.parseInt(date.substring(5, 7)), day = Integer.parseInt(date.substring(8, 10));
            if (m < 1 || m > 12 || day < 1 || day > 31) return "";
            if ("en".equals(lang)) return EN[m - 1] + " " + day;
            if ("ar".equals(lang)) return day + " " + AR[m - 1];
            return day + " " + FR[m - 1];
        } catch (NumberFormatException e) { return ""; }
    }

    static String city(String settingsJson) {
        if (settingsJson == null) return "";
        try {
            JSONObject s = new JSONObject(settingsJson).optJSONObject("settings");
            return s == null ? "" : s.optString("cityName", "");
        } catch (Exception e) { return ""; }
    }
}
