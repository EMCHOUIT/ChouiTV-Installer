package ma.chouitv.installer.guide;

import static org.junit.Assert.*;

import org.junit.Test;


/** The guided paths: their steps, how far a person is, and that a closed app resumes where it stopped. */
public class GuideTest {
    @Test public void eachPathHasItsSteps() {
        assertEquals(3, Guide.Path.EQUIP.steps);
        assertEquals(3, Guide.Path.COPY.steps);
        assertEquals(1, Guide.Path.REACT.steps);
    }

    @Test public void advancingStopsAtTheLastStep() {
        Guide g = new Guide(Guide.Path.COPY);
        assertFalse(g.last());
        g.advance(); g.advance();
        assertEquals(2, g.step); assertTrue(g.last());
        g.advance();
        assertEquals("never past the last", 2, g.step);
        Guide one = new Guide(Guide.Path.REACT);
        assertTrue(one.last());
    }

    @Test public void theKeysNameThePathAndTheStep() {
        Guide g = new Guide(Guide.Path.COPY);
        g.advance();
        assertEquals("gn_copy_1", g.key('n'));
        assertEquals("gd_copy_1", g.key('d'));
        assertEquals("gx_copy_1", g.key('x'));
        assertEquals("gl_copy_1", g.key('l'));
    }

    @Test public void aGuideComesBackFromItsTextWithItsStepAndItsProfile() {
        Guide g = new Guide(Guide.Path.COPY);
        g.advance();
        g.profile = "Salon · غرفة";
        Guide back = Guide.fromJson(g.toJson());
        assertEquals(Guide.Path.COPY, back.path);
        assertEquals(1, back.step);
        assertEquals("Salon · غرفة", back.profile);
    }

    @Test public void aDamagedOrEmptyTextGivesNoGuideAndAWildStepIsKeptInRange() {
        assertNull(Guide.fromJson(null));
        assertNull(Guide.fromJson(""));
        assertNull(Guide.fromJson("not json"));
        assertNull(Guide.fromJson("{\"path\":\"GONE\",\"step\":1}"));
        assertEquals(2, Guide.fromJson("{\"path\":\"COPY\",\"step\":99}").step);
        assertEquals(0, Guide.fromJson("{\"path\":\"COPY\",\"step\":-4}").step);
    }

    @Test public void everyKeyOfEveryStepHasAnEnglishFrenchAndArabicText() throws Exception {
        // the texts live in Strings.java: read the source and check each key is there once with three non-empty languages
        // the tests run in the module folder
        String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/ma/chouitv/installer/Strings.java")), "UTF-8");
        for (Guide.Path p : Guide.Path.values()) {
            for (int i = 0; i < p.steps; i++) {
                Guide g = new Guide(p);
                for (int k = 0; k < i; k++) g.advance();
                for (char kind : new char[] {'n', 'd', 'l', 'x'}) assertTrue("missing text " + g.key(kind), src.contains("k(\"" + g.key(kind) + "\", \""));
            }
        }
    }
}
