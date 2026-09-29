package com.matteopaciolla.prbe.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.matteopaciolla.portroyal.core.enums.MoveAction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;

public class PortRoyalCli {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_URL = "http://localhost:8080";
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "password";

    public static void main(String[] args) {
        Session session = new Session(DEFAULT_URL, DEFAULT_USERNAME, DEFAULT_PASSWORD, null);
        try (Scanner scanner = new Scanner(System.in)) {
            printBanner();
            while (true) {
                printMainMenu();
                System.out.print("Choose an option: ");
                String option = scanner.nextLine().trim();
                switch (option) {
                    case "1" -> configureConnection(session, scanner);
                    case "2" -> showCurrentUser(session, scanner);
                    case "3" -> registerUser(session, scanner);
                    case "4" -> listMatches(session, scanner);
                    case "5" -> hostMatch(session, scanner);
                    case "6" -> joinMatch(session, scanner);
                    case "7" -> addAiPlayerToMatch(session, scanner);
                    case "8" -> removeAiPlayerFromMatch(session, scanner);
                    case "9" -> startMatch(session, scanner);
                    case "10" -> viewMatch(session, scanner);
                    case "11" -> playInteractiveMatch(session, scanner);
                    case "12" -> apiExplorer(session, scanner);
                    case "13" -> printQuickGuide();
                    case "0", "exit", "quit" -> {
                        System.out.println("Bye.");
                        return;
                    }
                    default -> System.out.println("Unknown option. Use the menu numbers.");
                }
            }
        }
    }

    private static void configureConnection(Session session, Scanner scanner) {
        System.out.print("Base URL [" + session.baseUrl + "]: ");
        String baseUrl = scanner.nextLine().trim();
        if (!baseUrl.isBlank()) {
            session.baseUrl = baseUrl;
        }

        System.out.print("Username [" + session.username + "]: ");
        String username = scanner.nextLine().trim();
        if (!username.isBlank()) {
            session.username = username;
        }

        System.out.print("Password [" + session.password + "]: ");
        String password = scanner.nextLine();
        if (!password.isBlank()) {
            session.password = password;
        }

        System.out.print("tgId for bot flows (blank to disable): ");
        String tgId = scanner.nextLine().trim();
        session.tgId = tgId.isBlank() ? null : tgId;

        System.out.println("Connection configured.");
        System.out.println("Base URL: " + session.baseUrl);
        System.out.println("Username: " + session.username);
        System.out.println("Bot tgId: " + (session.tgId == null ? "disabled" : session.tgId));
    }

    private static void showCurrentUser(Session session, Scanner scanner) {
        ApiClient.Response response = call(session, "GET", "/api/v1/user/me", null, null);
        printResponse(response);
    }

    private static void registerUser(Session session, Scanner scanner) {
        System.out.print("Username: ");
        String username = scanner.nextLine().trim();
        if (username.isBlank()) {
            System.out.println("Username is required.");
            return;
        }
        System.out.print("Password: ");
        String password = scanner.nextLine();
        if (password.isBlank()) {
            System.out.println("Password is required.");
            return;
        }
        System.out.print("Email (optional): ");
        String email = scanner.nextLine().trim();
        System.out.print("Telegram Id (optional): ");
        String telegramId = scanner.nextLine().trim();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        if (!email.isBlank()) {
            payload.put("email", email);
        }
        if (!telegramId.isBlank()) {
            payload.put("telegramId", telegramId);
        }

        ApiClient.Response response = call(session, "POST", "/public/register", payload, null);
        printResponse(response);
    }

    private static void listMatches(Session session, Scanner scanner) {
        String path = "/api/v1/match/retrieve-all";
        System.out.print("Filter by username? [blank = all]: ");
        String username = scanner.nextLine().trim();
        if (!username.isBlank()) {
            path += "?username=" + urlEncode(username);
        }
        System.out.print("Ended filter [blank = all, true/false]: ");
        String ended = scanner.nextLine().trim();
        if (!ended.isBlank()) {
            String separator = path.contains("?") ? "&" : "?";
            path += separator + "ended=" + ended;
        }
        ApiClient.Response response = call(session, "GET", path, null, null);
        printResponse(response);
    }

    private static void hostMatch(Session session, Scanner scanner) {
        System.out.print("Optional config JSON (press Enter to use default config): ");
        String body = scanner.nextLine().trim();
        Map<String, Object> config = null;
        if (!body.isBlank()) {
            try {
                config = MAPPER.readValue(body, Map.class);
            } catch (IOException e) {
                System.out.println("Invalid JSON payload: " + e.getMessage());
                return;
            }
        }
        ApiClient.Response response = call(session, "POST", "/api/v1/match/host", config, session.tgId == null ? null : Map.of("tgId", session.tgId));
        printResponse(response);
    }

    private static void joinMatch(Session session, Scanner scanner) {
        System.out.print("Key code of the match to join: ");
        String keyCode = scanner.nextLine().trim();
        if (keyCode.isBlank()) {
            System.out.println("Key code cannot be blank.");
            return;
        }
        String path = "/api/v1/match/join?keyCode=" + urlEncode(keyCode);
        ApiClient.Response response = call(session, "PUT", path, null, session.tgId == null ? null : Map.of("tgId", session.tgId));
        printResponse(response);
    }

    private static void addAiPlayerToMatch(Session session, Scanner scanner) {
        System.out.println("AI difficulty: EASY, MEDIUM, HARD");
        System.out.print("Difficulty [MEDIUM]: ");
        String difficulty = scanner.nextLine().trim();
        if (difficulty.isBlank()) {
            difficulty = "MEDIUM";
        }
        System.out.print("AI player display name (optional): ");
        String name = scanner.nextLine().trim();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("difficulty", difficulty.toUpperCase(Locale.ROOT));
        if (!name.isBlank()) {
            payload.put("name", name);
        }
        ApiClient.Response response = call(session, "POST", "/api/v1/match/ai-player", payload, session.tgId == null ? null : Map.of("tgId", session.tgId));
        printResponse(response);
    }

    private static void removeAiPlayerFromMatch(Session session, Scanner scanner) {
        System.out.print("AI player username to remove (e.g. ABC123-ai-1): ");
        String aiPlayerUsername = scanner.nextLine().trim();
        if (aiPlayerUsername.isBlank()) {
            System.out.println("AI player username cannot be blank.");
            return;
        }
        String path = "/api/v1/match/ai-player?aiPlayerUsername=" + urlEncode(aiPlayerUsername);
        ApiClient.Response response = call(session, "DELETE", path, null, session.tgId == null ? null : Map.of("tgId", session.tgId));
        printResponse(response);
    }

    private static void startMatch(Session session, Scanner scanner) {
        ApiClient.Response response = call(session, "PUT", "/api/v1/match/start", null, session.tgId == null ? null : Map.of("tgId", session.tgId));
        printResponse(response);
    }

    private static void viewMatch(Session session, Scanner scanner) {
        System.out.print("Key code: ");
        String keyCode = scanner.nextLine().trim();
        if (keyCode.isBlank()) {
            System.out.println("Key code cannot be blank.");
            return;
        }
        ApiClient.Response response = call(session, "GET", "/api/v1/match/retrieve?keyCode=" + urlEncode(keyCode), null, null);
        printResponse(response);
    }

    private static void playInteractiveMatch(Session session, Scanner scanner) {
        System.out.print("Key code of the match to play (press Enter to use active match): ");
        String keyCode = scanner.nextLine().trim();
        if (keyCode.isBlank()) {
            System.out.println("Fetching current match status...");
            ApiClient.Response current = call(session, "GET", "/api/v1/match/status", null, null);
            printResponse(current);
            try {
                JsonNode root = MAPPER.readTree(current.body());
                JsonNode matchNode = root.get("data");
                if (matchNode == null || matchNode.isNull()) {
                    System.out.println("No active match found. Host or join a match first.");
                    return;
                }
                keyCode = matchNode.get("keyCode") == null ? null : matchNode.get("keyCode").asText();
            } catch (IOException e) {
                System.out.println("Unable to parse status response.");
                return;
            }
        }
        if (keyCode == null || keyCode.isBlank()) {
            System.out.println("Cannot identify which match to play.");
            return;
        }

        while (true) {
            ApiClient.Response response = call(session, "GET", "/api/v1/match/retrieve?keyCode=" + urlEncode(keyCode), null, null);
            try {
                JsonNode root = MAPPER.readTree(response.body());
                JsonNode match = root.get("data");
                if (match == null || match.isNull()) {
                    System.out.println("Match not found or no data returned.");
                    return;
                }
                printMatchSnapshot(match);
                System.out.println("--------------------------------------------------");
                System.out.println("Choose the move to send:");
                MoveAction[] actions = MoveAction.values();
                for (int i = 0; i < actions.length; i++) {
                    System.out.println(i + ". " + actions[i]);
                }
                System.out.println(actions.length + ". Back to main menu");
                System.out.print("Your move: ");
                String raw = scanner.nextLine().trim();
                int choice;
                try {
                    choice = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid selection.");
                    continue;
                }
                if (choice == actions.length) {
                    return;
                }
                if (choice < 0 || choice >= actions.length) {
                    System.out.println("Selection out of range.");
                    continue;
                }
                MoveAction action = actions[choice];
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("move", action.name());
                payload.put("parameterIndex", askInt(scanner, "parameterIndex (-1 = not used): ", -1, Integer.MAX_VALUE));
                payload.put("pickPlayerIndex", askInt(scanner, "pickPlayerIndex (-1 = not used): ", -1, Integer.MAX_VALUE));

                if (action == MoveAction.COMMIT_EXPEDITION) {
                    System.out.print("Expedition employee list as comma-separated names, e.g. CAPTAIN,CAPTAIN,SETTLER (blank = empty): ");
                    String empInput = scanner.nextLine().trim();
                    if (!empInput.isBlank()) {
                        List<String> values = new ArrayList<>();
                        for (String part : empInput.split(",")) {
                            values.add(part.trim().toUpperCase(Locale.ROOT));
                        }
                        payload.put("expeditionEmployeesList", values);
                    } else {
                        payload.put("expeditionEmployeesList", Collections.emptyList());
                    }
                } else {
                    payload.put("expeditionEmployeesList", Collections.emptyList());
                }

                ApiClient.Response moveResponse = call(session, "POST", "/api/v1/game/move", payload, session.tgId == null ? null : Map.of("tgId", session.tgId));
                printResponse(moveResponse);
                if (moveResponse.statusCode() >= 400) {
                    continue;
                }
                System.out.print("Play another move? [Y/n]: ");
                String repeat = scanner.nextLine().trim();
                if (!repeat.isBlank() && !repeat.equalsIgnoreCase("y") && !repeat.equalsIgnoreCase("yes")) {
                    return;
                }
            } catch (IOException e) {
                System.out.println("Issue while reading match state: " + e.getMessage());
                return;
            }
        }
    }

    private static void apiExplorer(Session session, Scanner scanner) {
        System.out.print("HTTP method [GET]: ");
        String method = scanner.nextLine().trim();
        if (method.isBlank()) {
            method = "GET";
        }
        System.out.print("Path, e.g. /api/v1/card or /api/v1/match/retrieve?keyCode=ABC123: ");
        String path = scanner.nextLine().trim();
        if (path.isBlank()) {
            path = "/api/v1/user/me";
        }
        System.out.print("JSON body (optional; leave blank for none): ");
        String rawBody = scanner.nextLine();
        String body = rawBody == null || rawBody.isBlank() ? null : rawBody;
        Map<String, String> headers = new HashMap<>();
        if (session.tgId != null && !session.tgId.isBlank()) {
            headers.put("tgId", session.tgId);
        }
        boolean basicAuthRequired = !ApiClient.isPublicEndpoint(path);
        try {
            ApiClient.Response response = ApiClient.execute(session.baseUrl, session.username, session.password, method, path, body, headers, basicAuthRequired);
            printResponse(response);
        } catch (IOException | InterruptedException e) {
            System.out.println("Request failed: " + e.getMessage());
        }
    }

    private static void printQuickGuide() {
        System.out.println("Quick guide:");
        System.out.println("- Default credentials: admin / password");
        System.out.println("- Default base URL: http://localhost:8080");
        System.out.println("- The app calls the running Spring Boot instance via HTTP Basic Auth");
        System.out.println("- For Bot flows, set a tgId in the connection config and the CLI will include the tgId header");
        System.out.println("- To play a match, host it, join it, start it, then use the interactive move menu");
        System.out.println("- To mix in autonomous players, host a match, use 'Add AI player' (EASY/MEDIUM/HARD) before starting it");
        System.out.println("- AI turns are auto-played by the backend; poll 'View a match' to see their moves as they happen");
    }

    private static ApiClient.Response call(Session session, String method, String path, Object payload, Map<String, String> headers) {
        String body = null;
        if (payload != null) {
            try {
                body = MAPPER.writeValueAsString(payload);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to serialize payload for request: " + e.getMessage(), e);
            }
        }
        try {
            return ApiClient.execute(session.baseUrl, session.username, session.password, method, path, body, headers, !ApiClient.isPublicEndpoint(path));
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Request failed: " + e.getMessage(), e);
        }
    }

    private static void printResponse(ApiClient.Response response) {
        System.out.println("--------------------------------------------------");
        System.out.println("HTTP status: " + response.statusCode() + (response.reason() == null || response.reason().isBlank() ? "" : " (" + response.reason() + ")"));
        System.out.println(ApiClient.prettyPrint(response.body()));
    }

    private static void printBanner() {
        System.out.println("==================================================");
        System.out.println("Port Royal CLI");
        System.out.println("Test the API and play a match without starting the Spring app inside the same process.");
        System.out.println("==================================================");
    }

    private static void printMainMenu() {
        System.out.println();
        System.out.println("Main menu");
        System.out.println("1. Configure connection");
        System.out.println("2. Show current user");
        System.out.println("3. Register new user");
        System.out.println("4. List matches");
        System.out.println("5. Host match");
        System.out.println("6. Join match");
        System.out.println("7. Add AI player to hosted match");
        System.out.println("8. Remove AI player from hosted match");
        System.out.println("9. Start match");
        System.out.println("10. View a match by key code");
        System.out.println("11. Play a match interactively");
        System.out.println("12. API explorer / generic endpoint tester");
        System.out.println("13. Quick guide");
        System.out.println("0. Exit");
    }

    private static int askInt(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            String raw = scanner.nextLine().trim();
            if (raw.isBlank()) {
                return minimum;
            }
            try {
                int value = Integer.parseInt(raw);
                if (value < minimum || value > maximum) {
                    System.out.println("Value must be between " + minimum + " and " + maximum + ".");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("This value must be an integer.");
            }
        }
    }

    private static String urlEncode(String input) {
        try {
            return java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return input;
        }
    }

    public static void printMatchSnapshot(JsonNode match) {
        System.out.println("Match snapshot");
        System.out.println("keyCode: " + text(match, "keyCode", "n/a"));
        System.out.println("phase: " + text(match, "currentPhase", "n/a"));
        System.out.println("runningPlayerIndex: " + text(match, "runningPlayerIndex", "n/a"));
        System.out.println("activePlayerIndex: " + text(match, "activePlayerIndex", "n/a"));
        System.out.println("started: " + bool(match, "started"));
        System.out.println("matchEnded: " + bool(match, "matchEnded"));
        JsonNode players = match.get("players");
        if (players != null && players.isArray()) {
            System.out.println("Players:");
            for (JsonNode player : players) {
                String name = text(player, "username", "unknown");
                String coins = text(player, "coins", "-");
                String points = text(player, "points", "-");
                JsonNode user = player.get("user");
                String aiTag = "";
                if (user != null && user.get("aiDifficulty") != null && !user.get("aiDifficulty").isNull()) {
                    aiTag = " [AI/" + user.get("aiDifficulty").asText() + "]";
                }
                System.out.println(" - " + name + aiTag + " (coins=" + coins + ", points=" + points + ")");
            }
        }
        JsonNode table = match.get("table");
        if (table != null) {
            JsonNode harbor = table.get("harbor");
            if (harbor != null && harbor.isArray()) {
                System.out.println("Harbor cards:");
                for (JsonNode card : harbor) {
                    System.out.println(" - " + card);
                }
            }
            JsonNode expeditions = table.get("expeditions");
            if (expeditions != null && expeditions.isArray()) {
                System.out.println("Expedition cards:");
                for (JsonNode card : expeditions) {
                    System.out.println(" - " + card);
                }
            }
        }
    }

    private static String text(JsonNode node, String field, String fallback) {
        if (node == null || node.isNull()) {
            return fallback;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private static boolean bool(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value != null && !value.isNull() && value.asBoolean();
    }

    private static final class Session {
        private String baseUrl;
        private String username;
        private String password;
        private String tgId;

        private Session(String baseUrl, String username, String password, String tgId) {
            this.baseUrl = baseUrl;
            this.username = username;
            this.password = password;
            this.tgId = tgId;
        }
    }
}
