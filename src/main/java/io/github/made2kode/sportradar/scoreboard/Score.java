package io.github.made2kode.sportradar.scoreboard;

record Score(int home, int away) {

    static final Score ZERO = new Score(0, 0);

    Score {
        if (home < 0 || away < 0) {
            throw new IllegalArgumentException("Scores cannot be negative");
        }
        Math.addExact(home, away);
    }

    int total() {
        return Math.addExact(home, away);
    }
}
