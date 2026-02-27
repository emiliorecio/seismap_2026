package com.seismap.service.parser;

import com.seismap.model.enums.MagnitudeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for Nordic/SEISAN format data files (.data).
 * <p>
 * Each seismic event is a block of lines. The Type 1 line (character '1' at
 * column 80)
 * contains the main event data: date, location, depth, up to 3 magnitudes.
 * A blank line (all spaces, type '0') terminates the event block.
 * <p>
 * Only Type 1 lines are parsed; all other line types (3, 6, 7, E, I) are
 * skipped.
 * Multiple Type 1 lines per event can contribute additional magnitudes.
 * <p>
 * Column positions are 1-indexed as per the Nordic format specification.
 */
public class NordicSeisanParser {

    private static final Logger log = LoggerFactory.getLogger(NordicSeisanParser.class);

    /**
     * Parsed representation of a single seismic event from the data file.
     */
    public static class ParsedEvent {
        private LocalDateTime date;
        private Float latitude;
        private Float longitude;
        private Float depth;
        private final List<ParsedMagnitude> magnitudes = new ArrayList<>();

        public LocalDateTime getDate() {
            return date;
        }

        public Float getLatitude() {
            return latitude;
        }

        public Float getLongitude() {
            return longitude;
        }

        public Float getDepth() {
            return depth;
        }

        public List<ParsedMagnitude> getMagnitudes() {
            return magnitudes;
        }

        public boolean isComplete() {
            return date != null && latitude != null && longitude != null && depth != null;
        }
    }

    public static class ParsedMagnitude {
        private final MagnitudeType type;
        private final float value;
        private final String reportingAgency;

        public ParsedMagnitude(MagnitudeType type, float value, String reportingAgency) {
            this.type = type;
            this.value = value;
            this.reportingAgency = reportingAgency;
        }

        public MagnitudeType getType() {
            return type;
        }

        public float getValue() {
            return value;
        }

        public String getReportingAgency() {
            return reportingAgency;
        }
    }

    /**
     * Parse a Nordic/SEISAN .data file and return a list of parsed events.
     */
    public List<ParsedEvent> parseFile(File file) throws IOException {
        List<ParsedEvent> events = new ArrayList<>();
        ParsedEvent currentEvent = null;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), "ISO-8859-1"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Pad line to at least 80 characters
                if (line.length() < 80) {
                    line = String.format("%-80s", line);
                }

                char lineType = line.charAt(79); // Column 80 (0-indexed: 79)

                if (lineType == ' ' || line.trim().isEmpty()) {
                    // Blank line = event terminator
                    if (currentEvent != null) {
                        if (currentEvent.isComplete()) {
                            events.add(currentEvent);
                        } else {
                            log.warn("Line {}: Skipping incomplete event at date={}", lineNumber, currentEvent.date);
                        }
                        currentEvent = null;
                    }
                    continue;
                }

                if (lineType == '1') {
                    // Type 1 line — main event data
                    if (currentEvent == null) {
                        currentEvent = new ParsedEvent();
                    }
                    parseType1Line(line, currentEvent, lineNumber);
                }
                // All other line types (3, 6, 7, E, F, I) are silently skipped
            }
        }

        // Handle last event if file doesn't end with a blank line
        if (currentEvent != null && currentEvent.isComplete()) {
            events.add(currentEvent);
        }

        return events;
    }

    /**
     * Parse a Type 1 line. The first Type 1 line sets date/location/depth.
     * All Type 1 lines contribute magnitudes (up to 3 per line).
     * <p>
     * Column layout (1-indexed):
     * 2-5: Year
     * 7-8: Month
     * 9-10: Day
     * 12-13: Hour
     * 14-15: Minutes
     * 17-20: Seconds (F4.1)
     * 24-30: Latitude (F7.3)
     * 31-38: Longitude (F8.3)
     * 39-43: Depth (F5.1)
     * 56-59: Magnitude 1 value (F4.1)
     * 60: Magnitude 1 type (L=ML, B=MB, S=MS, W=MW, G=MBLG, C=MC)
     * 61-63: Magnitude 1 reporting agency
     * 64-67: Magnitude 2 value (F4.1)
     * 68: Magnitude 2 type
     * 69-71: Magnitude 2 reporting agency
     * 72-75: Magnitude 3 value (F4.1)
     * 76: Magnitude 3 type
     * 77-79: Magnitude 3 reporting agency
     */
    private void parseType1Line(String line, ParsedEvent event, int lineNumber) {
        try {
            // Only set date/location from the first Type 1 line
            if (event.date == null) {
                Integer year = parseInteger(line, 1, 5);
                Integer month = parseInteger(line, 6, 8);
                Integer day = parseInteger(line, 8, 10);
                Integer hour = parseInteger(line, 11, 13);
                Integer minute = parseInteger(line, 13, 15);
                Float seconds = parseFloat(line, 16, 20);

                if (year != null && month != null && day != null && hour != null && minute != null) {
                    int sec = seconds != null ? (int) seconds.floatValue() : 0;
                    try {
                        event.date = LocalDateTime.of(year, month, day, hour, minute, sec);
                    } catch (Exception e) {
                        log.warn("Line {}: Invalid date values: {}/{}/{} {}:{}:{}", lineNumber, year, month, day, hour,
                                minute, sec);
                    }
                }

                event.latitude = parseFloat(line, 23, 30);
                event.longitude = parseFloat(line, 30, 38);
                event.depth = parseFloat(line, 38, 43);
            }

            // Parse up to 3 magnitudes from every Type 1 line
            addMagnitude(event, line, 55, 59, 59, 60, 63, lineNumber);
            addMagnitude(event, line, 63, 67, 67, 68, 71, lineNumber);
            addMagnitude(event, line, 71, 75, 75, 76, 79, lineNumber);

        } catch (Exception e) {
            log.warn("Line {}: Error parsing Type 1 entry: {}", lineNumber, e.getMessage());
        }
    }

    private void addMagnitude(ParsedEvent event, String line,
            int valStart, int valEnd, int typePos, int agStart, int agEnd,
            int lineNumber) {
        Float value = parseFloat(line, valStart, valEnd);
        if (value == null)
            return;

        char typeChar = safeCharAt(line, typePos);
        MagnitudeType type = parseMagnitudeType(typeChar);
        if (type == null)
            return;

        String agency = safeSubstring(line, agStart, agEnd).trim();
        if (agency.isEmpty())
            agency = "UNK";

        event.magnitudes.add(new ParsedMagnitude(type, value, agency));
    }

    private MagnitudeType parseMagnitudeType(char c) {
        return switch (c) {
            case 'L' -> MagnitudeType.ML;
            case 'B', 'b' -> MagnitudeType.MB;
            case 'S', 's' -> MagnitudeType.MS;
            case 'W' -> MagnitudeType.MW;
            case 'G' -> MagnitudeType.MBLG;
            case 'C' -> MagnitudeType.MC;
            default -> null; // BLANK or unknown
        };
    }

    // --- Utility methods for fixed-column parsing (0-indexed internally) ---

    private Integer parseInteger(String line, int startCol1, int endCol1) {
        String s = safeSubstring(line, startCol1, endCol1).trim();
        if (s.isEmpty())
            return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Float parseFloat(String line, int startCol0, int endCol0) {
        String s = safeSubstring(line, startCol0, endCol0).trim();
        if (s.isEmpty())
            return null;
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Safe substring using 0-indexed positions.
     */
    private String safeSubstring(String line, int start, int end) {
        if (start >= line.length())
            return "";
        end = Math.min(end, line.length());
        return line.substring(start, end);
    }

    private char safeCharAt(String line, int pos) {
        return pos < line.length() ? line.charAt(pos) : ' ';
    }
}
