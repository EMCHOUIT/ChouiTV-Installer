package ma.chouitv.installer.remote;

import java.util.List;

/**
 * Which TV on the network is the one the family had at another address (the box gave it a new one). The serial number says it for sure; without one, the
 * only TV of the same model does. Two of the same model are never mixed up. Pure, tested on the JVM (TvMatchTest).
 */
public final class TvMatch {
    private TvMatch() {}

    public static final class Candidate {
        public final String ip, serial, model;
        public Candidate(String ip, String serial, String model) {
            this.ip = ip;
            this.serial = serial == null ? "" : serial;
            this.model = model == null ? "" : model;
        }
    }

    /** The index of the candidate that is the saved TV, or -1. */
    public static int pick(String savedSerial, String savedModel, List<Candidate> candidates) {
        if (candidates == null || candidates.isEmpty()) return -1;
        String serial = savedSerial == null ? "" : savedSerial.trim();
        String model = savedModel == null ? "" : savedModel.trim();
        if (!serial.isEmpty()) {
            for (int i = 0; i < candidates.size(); i++) if (serial.equals(candidates.get(i).serial)) return i;
            return -1;   // a saved serial that is on no candidate: another TV, or the same one not answering
        }
        if (model.isEmpty()) return -1;
        int found = -1;
        for (int i = 0; i < candidates.size(); i++) {
            if (model.equals(candidates.get(i).model.trim())) {
                if (found >= 0) return -1;   // two of that model: no guess
                found = i;
            }
        }
        return found;
    }
}
