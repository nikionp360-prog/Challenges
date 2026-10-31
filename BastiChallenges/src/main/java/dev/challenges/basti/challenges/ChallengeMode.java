package dev.challenges.basti.challenges;

import java.util.Locale;

public enum ChallengeMode {
    FORCEITEM("forceitem", "Force Item"),
    FORCEMOB("forcemob", "Force Mob"),
    ARMYBATTLE("armybattle", "Monster Army Battle"),
    FORCESKILL("forceskill", "Force Skill");

    private final String key;
    private final String display;

    ChallengeMode(String key, String display) {
        this.key = key;
        this.display = display;
    }

    public String key() { return key; }
    public String display() { return display; }

    public static ChallengeMode fromKey(String s) {
        if (s == null) return null;
        String k = s.toLowerCase(Locale.ROOT);
        if (k.equals("army")) k = "armybattle";
        for (ChallengeMode m : values()) if (m.key.equals(k)) return m;
        return null;
    }
}
