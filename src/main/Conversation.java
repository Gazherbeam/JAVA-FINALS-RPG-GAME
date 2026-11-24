package main;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A small, reusable sequence of speaker names and dialogue lines.
 * Create additional conversations with new Conversation(...).
 */
public class Conversation {

    public static final class Line {
        public final String speaker;
        public final String text;

        public Line(String speaker, String text) {
            this.speaker = speaker == null ? "" : speaker;
            this.text = text == null ? "" : text;
        }
    }

    private final List<Line> lines;

    public Conversation(List<Line> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("A conversation must contain at least one line.");
        }
        this.lines = Collections.unmodifiableList(lines);
    }

    public Conversation(Line... lines) {
        this(Arrays.asList(lines));
    }

    public int size() {
        return lines.size();
    }

    public Line getLine(int index) {
        return lines.get(index);
    }

    /** Temporary sign dialogue used to verify the textbox system. */
    public static Conversation testSign() {
        return new Conversation(
            new Line("Weathered Sign", "Welcome, traveler."),
            new Line("Weathered Sign", "If your reading this, that means it works!"),
            new Line("Weathered Sign", "Press E to continue. Enjoy your adventure!")
        );
    }
}
