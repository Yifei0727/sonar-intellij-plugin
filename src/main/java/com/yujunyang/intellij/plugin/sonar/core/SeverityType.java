package com.yujunyang.intellij.plugin.sonar.core;

public enum SeverityType {
    BLOCKER(10),
    CRITICAL(7),
    MAJOR(5),
    MINOR(3),
    INFO(1),
    ANY(0);
    private final int value;

    SeverityType(int n) {
        value = n;
    }

    public int severity() {
        return value;
    }

    public static SeverityType fromName(String name) {
        if (BLOCKER.name().equalsIgnoreCase(name)) {
            return BLOCKER;
        } else if (CRITICAL.name().equalsIgnoreCase(name)) {
            return CRITICAL;
        } else if (MAJOR.name().equalsIgnoreCase(name)) {
            return MAJOR;
        } else if (MINOR.name().equalsIgnoreCase(name)) {
            return MINOR;
        } else if (INFO.name().equalsIgnoreCase(name)) {
            return INFO;
        } else {
            return ANY;
        }
    }

}
