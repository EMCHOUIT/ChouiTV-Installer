package ma.chouitv.installer;

import java.io.IOException;

/**
 * 3.x to 4.0. A TV that still has the separate screensaver app (ma.chouitv.screensaver) keeps its settings (city, prayer method, radios...)
 * in that app. Before 4.0 goes on, they are exported through the OLD receiver and kept in the phone's own storage; once 4.0 is installed they
 * are imported into the merged app, the dream is set to it, and the person is ASKED whether to remove the old app. Nothing here is silent:
 * the old app is removed only after a "yes", and never when its settings could not be saved or imported (it still holds the only copy).
 * Pure Java over small interfaces, so every path is tested on a fake TV.
 */
final class Migration {
    private Migration() {}

    /** What the plan needs from the TV; Flow implements it over ADB, the tests over a fake. */
    interface Tv {
        boolean installed(String pkg) throws IOException;
        /** The settings JSON of the OLD screensaver app (through its own receiver). */
        String exportOld() throws IOException;
        /** Applies the JSON in the merged app; throws when the app refuses it. */
        void importInto(String json) throws IOException;
        /** Makes the merged app's screensaver the TV's dream. */
        void setDream() throws IOException;
        void uninstall(String pkg) throws IOException;
    }

    /** Where the exported settings wait between the export and the import (a private file of the app). */
    interface Store {
        void save(String json) throws IOException;
        String load() throws IOException;
        void clear();
    }

    enum Outcome {
        /** The TV never had the old app (new install, or 4.0 already): nothing to do. */
        NOTHING,
        /** The old app is there but its settings could not be read or kept: it stays, nothing is imported. */
        NOT_SAVED,
        /** The merged app did not take the settings (or the dream): the old app stays, it still has the only copy. */
        IMPORT_FAILED,
        /** Imported; the person kept the old app (or nobody could be asked). */
        KEPT,
        /** Imported; the person agreed and the old app is gone. */
        REMOVED,
        /** Imported; the person agreed but the TV did not remove it. */
        REMOVE_FAILED
    }

    static final class State {
        final boolean oldPresent, saved;
        State(boolean oldPresent, boolean saved) { this.oldPresent = oldPresent; this.saved = saved; }
    }

    /** Before 4.0 is installed: is the old app there, and are its settings safe? Only reads from the TV. */
    static State before(Tv tv, Store store) throws IOException {
        store.clear();   // never a file left from another TV or another attempt
        if (!tv.installed(Flow.OLD_SAVER)) return new State(false, false);
        try {
            String json = tv.exportOld();
            if (!looksLikeSettings(json)) return new State(true, false);
            store.save(json);
            return new State(true, true);
        } catch (IOException cannot) {
            store.clear();
            return new State(true, false);
        }
    }

    /** What an export must look like to be worth keeping: one JSON object. */
    static boolean looksLikeSettings(String json) {
        if (json == null) return false;
        String t = json.trim();
        return t.length() > 2 && t.charAt(0) == '{' && t.charAt(t.length() - 1) == '}';
    }

    /** After 4.0 is installed: imports, sets the dream, asks, and only then removes. */
    static Outcome after(Tv tv, Store store, State st, Flow.Ask ask) {
        try {
            if (!st.oldPresent) return Outcome.NOTHING;
            if (!st.saved) return Outcome.NOT_SAVED;
            String json;
            try {
                json = store.load();
                if (!looksLikeSettings(json)) return Outcome.NOT_SAVED;
                if (!tv.installed(Flow.HOME)) return Outcome.IMPORT_FAILED;
                tv.importInto(json);
                tv.setDream();
            } catch (IOException | RuntimeException failed) {
                return Outcome.IMPORT_FAILED;
            }
            if (ask == null || !ask.allow(Flow.OLD_SAVER)) return Outcome.KEPT;
            try {
                tv.uninstall(Flow.OLD_SAVER);
                return tv.installed(Flow.OLD_SAVER) ? Outcome.REMOVE_FAILED : Outcome.REMOVED;
            } catch (IOException | RuntimeException failed) {
                return Outcome.REMOVE_FAILED;
            }
        } finally {
            store.clear();
        }
    }

    /** The store in a private file of the app. */
    static Store fileStore(final java.io.File file) {
        return new Store() {
            @Override public void save(String json) throws IOException {
                try (java.io.FileOutputStream o = new java.io.FileOutputStream(file)) { o.write(json.getBytes("UTF-8")); }
            }
            @Override public String load() throws IOException {
                try (java.io.FileInputStream i = new java.io.FileInputStream(file)) {
                    java.io.ByteArrayOutputStream b = new java.io.ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = i.read(buf)) > 0) b.write(buf, 0, n);
                    return new String(b.toByteArray(), "UTF-8");
                }
            }
            @Override public void clear() { file.delete(); }
        };
    }
}
