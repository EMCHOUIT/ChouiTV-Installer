package ma.chouitv.installer.profiles;

/** The settings of one TV under a name the person chose, kept on the phone to be sent to another TV later. */
public final class Profile {
    public final String id, name, date, tv, version, settingsJson;

    Profile(String id, String name, String date, String tv, String version, String settingsJson) {
        this.id = id; this.name = name; this.date = date; this.tv = tv; this.version = version; this.settingsJson = settingsJson;
    }
}
