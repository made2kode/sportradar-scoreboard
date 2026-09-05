package io.github.made2kode.sportradar.scoreboard;

import java.util.Objects;

record TeamName(String value) {

    TeamName {
        Objects.requireNonNull(value, "Team name must not be null");
        value = value.strip();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Team name must not be blank");
        }
    }
}
