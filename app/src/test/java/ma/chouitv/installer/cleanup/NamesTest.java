package ma.chouitv.installer.cleanup;

import static org.junit.Assert.*;

import org.junit.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/** Every app in a checklist has a name a person can read, and the package goes under it. */
public class NamesTest {
    @Test public void aPackageIsMadeReadableAndNeverShownRaw() {
        assertEquals("Tdm Manager", CleanupData.pretty("com.tdm.manager"));
        assertEquals("Maertsno Tv", CleanupData.pretty("com.maertsno.tv"));
        assertEquals("Ycntivi Off", CleanupData.pretty("ver3.ycntivi.off"));
        assertEquals("Media Center", CleanupData.pretty("com.tcl.ui_mediaCenter").replace("Ui ", ""));
        assertEquals("Printspooler", CleanupData.pretty("com.android.printspooler"));
        assertEquals("Newfangled", CleanupData.pretty("org.example.newfangled"));
    }

    @Test public void theBrandNameWinsThenTheLanguageName() throws Exception {
        CleanupData d = CleanupData.parse("{\"schema\":1,\"consensusMin\":20,\"keep\":[],\"homes\":[],\"genericModels\":[],\"genericDevices\":[],\"packages\":[],\"groups\":[],"
            + "\"display\":{\"a.b.c\":\"Brand C\",\"d.e.f\":\"Print service\"},\"names\":{\"ar\":{\"d.e.f\":\"طباعة\"},\"fr\":{\"d.e.f\":\"Service d'impression\"}},\"lists\":[]}");
        assertEquals("Brand C", d.label("a.b.c", "fr"));
        assertEquals("Service d'impression", d.label("d.e.f", "fr"));
        assertEquals("طباعة", d.label("d.e.f", "ar"));
        assertEquals("Print service", d.label("d.e.f", "en"));
        assertEquals("Foo Bar", d.label("x.y.fooBar", "en"));
    }

    @Test public void theShippedListGivesEveryPackageOfAPlanARealName() throws Exception {
        String json;
        try (InputStream in = getClass().getResourceAsStream("/cleanup.json")) {
            java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            json = new String(o.toByteArray(), "UTF-8");
        }
        CleanupData d = CleanupData.parse(json);
        Set<String> seen = new HashSet<>();
        for (CleanupData.Entry e : d.lists) for (String p : e.packages) {
            String name = d.label(p, "fr");
            assertFalse("no raw package: " + p, name.equals(p));
            assertFalse("no dots in a name: " + name, name.contains("."));
            assertFalse(name.isEmpty());
            seen.add(p);
        }
        assertTrue(seen.size() > 500);
        assertEquals("Netflix", d.label("com.netflix.ninja", "fr"));
        assertEquals("Service d'impression", d.label("com.android.printspooler", "fr"));
    }
}
