package com.linklens.extractionservice.websiteanalysis.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class RobotsPolicy {

    private final List<Rule> rules;
    private final boolean denied;

    private RobotsPolicy(List<Rule> rules, boolean denied) {
        this.rules = rules;
        this.denied = denied;
    }

    static RobotsPolicy allowAll() {
        return new RobotsPolicy(List.of(), false);
    }

    static RobotsPolicy denyAll() {
        return new RobotsPolicy(List.of(), true);
    }

    static RobotsPolicy parse(String content, String userAgent) {
        List<Group> groups = new ArrayList<>();
        Group current = null;
        for (String line : content.split("\\r?\\n")) {
            String directive = line.split("#", 2)[0].trim();
            if (directive.isEmpty()) {
                if (current != null && current.hasRules()) {
                    groups.add(current);
                    current = null;
                }
                continue;
            }

            int colon = directive.indexOf(':');
            if (colon < 0) {
                continue;
            }
            String name = directive.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = directive.substring(colon + 1).trim();
            if ("user-agent".equals(name)) {
                if (current == null || current.hasRules()) {
                    if (current != null) {
                        groups.add(current);
                    }
                    current = new Group();
                }
                current.agents.add(value.toLowerCase(Locale.ROOT));
            } else if (current != null && current.agents.size() > 0) {
                if ("allow".equals(name) && !value.isEmpty()) {
                    current.rules.add(new Rule(true, value));
                } else if ("disallow".equals(name) && !value.isEmpty()) {
                    current.rules.add(new Rule(false, value));
                }
            }
        }
        if (current != null) {
            groups.add(current);
        }

        String normalizedAgent = userAgent.toLowerCase(Locale.ROOT);
        List<Rule> matching = new ArrayList<>();
        int bestSpecificity = -1;
        for (Group group : groups) {
            int groupSpecificity = group.agents.stream()
                    .filter(agent -> !agent.equals("*") && normalizedAgent.startsWith(agent))
                    .mapToInt(String::length)
                    .max()
                    .orElse(group.agents.contains("*") ? 0 : -1);
            if (groupSpecificity > bestSpecificity) {
                bestSpecificity = groupSpecificity;
                matching.clear();
            }
            if (groupSpecificity >= 0 && groupSpecificity == bestSpecificity) {
                matching.addAll(group.rules);
            }
        }
        return new RobotsPolicy(List.copyOf(matching), false);
    }

    boolean allows(String url) {
        if (denied) {
            return false;
        }
        URI uri = URI.create(url);
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        if (uri.getRawQuery() != null) {
            path += "?" + uri.getRawQuery();
        }

        Rule selected = null;
        int selectedSpecificity = -1;
        for (Rule rule : rules) {
            if (!rule.matches(path)) {
                continue;
            }
            int specificity = rule.specificity();
            if (specificity > selectedSpecificity ||
                    (specificity == selectedSpecificity && rule.allow)) {
                selected = rule;
                selectedSpecificity = specificity;
            }
        }
        return selected == null || selected.allow;
    }

    private static final class Group {
        private final List<String> agents = new ArrayList<>();
        private final List<Rule> rules = new ArrayList<>();

        private boolean hasRules() {
            return !rules.isEmpty();
        }
    }

    private record Rule(boolean allow, String path) {
        private boolean matches(String candidate) {
            StringBuilder regex = new StringBuilder("^");
            for (int i = 0; i < path.length(); i++) {
                char character = path.charAt(i);
                if (character == '*') {
                    regex.append(".*");
                } else if (character == '$' && i == path.length() - 1) {
                    regex.append('$');
                } else {
                    if ("\\.[]{}()+-^$|?".indexOf(character) >= 0) {
                        regex.append('\\');
                    }
                    regex.append(character);
                }
            }
            if (!path.endsWith("$")) {
                regex.append(".*");
            }
            return candidate.matches(regex.toString());
        }

        private int specificity() {
            return (int) path.chars().filter(character -> character != '*' && character != '$').count();
        }
    }
}
