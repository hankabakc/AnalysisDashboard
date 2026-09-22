package com.sistek.sos.analysis_dashboard.repositories;

/**
 * LIKE / ILIKE aramalarında kullanıcı metnindeki joker karakterleri etkisizleştirir.
 * Sorgular bu sınıfın kaçış karakteriyle (`ESCAPE '!'`) yazılmalıdır; aksi hâlde
 * "%" araması bütün kayıtları, "a_b" araması "axb" kayıtlarını getirir.
 */
public final class LikeEscape {

    /** Sorgulardaki ESCAPE bildirimiyle aynı olmak zorundadır. */
    public static final String ESCAPE_CHAR = "!";

    private LikeEscape() {
    }

    /** Kaçış karakterinin kendisi önce kaçırılır; sırası değişirse "!%" girdisi bozulur. */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace(ESCAPE_CHAR, ESCAPE_CHAR + ESCAPE_CHAR)
                .replace("%", ESCAPE_CHAR + "%")
                .replace("_", ESCAPE_CHAR + "_");
    }
}
