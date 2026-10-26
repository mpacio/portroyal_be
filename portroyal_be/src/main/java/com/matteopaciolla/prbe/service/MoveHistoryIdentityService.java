package com.matteopaciolla.prbe.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MoveHistoryIdentityService {

    private final JdbcTemplate jdbcTemplate;

    public void renamePlayer(String previousUsername, String newUsername) {
        jdbcTemplate.update(
                "UPDATE moves SET active_player_username = ? WHERE active_player_username = ?",
                newUsername, previousUsername);
        jdbcTemplate.update(
                "UPDATE moves SET running_player_username = ? WHERE running_player_username = ?",
                newUsername, previousUsername);

        Pattern usernamePattern = usernamePattern(previousUsername);
        List<MoveNote> matchingNotes = jdbcTemplate.query(
                "SELECT id, notes FROM moves WHERE notes IS NOT NULL AND strpos(notes, ?) > 0",
                (resultSet, rowNumber) -> new MoveNote(resultSet.getLong("id"), resultSet.getString("notes")),
                previousUsername);
        for (MoveNote moveNote : matchingNotes) {
            String updatedNotes = usernamePattern.matcher(moveNote.notes())
                    .replaceAll(Matcher.quoteReplacement(newUsername));
            if (!updatedNotes.equals(moveNote.notes())) {
                jdbcTemplate.update("UPDATE moves SET notes = ? WHERE id = ?", updatedNotes, moveNote.id());
            }
        }
    }

    static Pattern usernamePattern(String username) {
        return Pattern.compile("(?<![\\p{L}\\p{N}_-])" + Pattern.quote(username) + "(?![\\p{L}\\p{N}_-])");
    }

    private record MoveNote(Long id, String notes) {
    }
}
