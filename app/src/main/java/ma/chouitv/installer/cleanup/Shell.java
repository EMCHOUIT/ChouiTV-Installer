package ma.chouitv.installer.cleanup;

import java.io.IOException;

/** One shell command on the TV and its output. The real one is the ADB connection; tests use a fake TV. */
public interface Shell {
    String run(String command) throws IOException;
}
