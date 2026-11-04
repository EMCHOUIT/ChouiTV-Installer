package ma.chouitv.installer.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** What the remote shows when it opens: the demo without any TV, the banner for each situation, the short search, and the keys that need a TV. */
public class HomeStateTest {
    @Test public void withoutAnyTvTheBannerFollowsTheSearch() {
        assertEquals(HomeState.Banner.SEARCHING, HomeState.banner(false, HomeState.Link.ONLINE, true, 0));
        assertEquals("a TV was found: say how many", HomeState.Banner.FOUND, HomeState.banner(false, HomeState.Link.ONLINE, false, 2));
        assertEquals("found while it still looks: show what was found", HomeState.Banner.FOUND, HomeState.banner(false, HomeState.Link.ONLINE, true, 1));
        assertEquals("nothing found: the way to look again", HomeState.Banner.NONE_FOUND, HomeState.banner(false, HomeState.Link.ONLINE, false, 0));
    }

    @Test public void withAKnownTvOnlyAnUnansweredTvNeedsABanner() {
        assertEquals(HomeState.Banner.OFFLINE, HomeState.banner(true, HomeState.Link.OFFLINE, false, 0));
        assertEquals("connecting is shown under the name, not in a banner", HomeState.Banner.HIDDEN, HomeState.banner(true, HomeState.Link.RECONNECTING, false, 0));
        assertEquals(HomeState.Banner.HIDDEN, HomeState.banner(true, HomeState.Link.ONLINE, false, 0));
        assertEquals("a search result never hides the state of the known TV", HomeState.Banner.OFFLINE, HomeState.banner(true, HomeState.Link.OFFLINE, true, 3));
    }

    @Test public void theShortSearchRunsAtEveryOpeningWithoutATvAndStaysShort() {
        assertTrue(HomeState.searchesOnOpen(false));
        assertFalse("a known TV is reconnected instead", HomeState.searchesOnOpen(true));
        assertEquals(4000, HomeState.SEARCH_BUDGET_MS);
    }

    @Test public void theKeysThatActOnTheTvNeedOne() {
        assertTrue(HomeState.keysNeedTv(false));
        assertFalse(HomeState.keysNeedTv(true));
    }

    @Test public void theRemoteShowsTheStateOfTheLinkItReallyHas() {
        assertEquals(HomeState.Link.ONLINE, HomeState.linkOf(true, true));
        assertEquals("a known TV without an open link is offline", HomeState.Link.OFFLINE, HomeState.linkOf(true, false));
        assertEquals("no TV: nothing to be offline from", HomeState.Link.ONLINE, HomeState.linkOf(false, false));
    }
}
