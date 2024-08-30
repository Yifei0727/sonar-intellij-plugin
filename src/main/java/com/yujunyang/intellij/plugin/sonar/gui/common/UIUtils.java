/*
 * Copyright 2021 Yu Junyang
 * https://github.com/lowkeyfish
 *
 * This file is part of Sonar Intellij plugin.
 *
 * Sonar Intellij plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Sonar Intellij plugin is distributed in the hope that it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Sonar Intellij plugin.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.yujunyang.intellij.plugin.sonar.gui.common;


import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.UIUtil;
import com.yujunyang.intellij.plugin.sonar.core.SeverityType;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;
import icons.PluginIcons;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;


public final class UIUtils {
    private static final Map<String, String> UI_LANGUAGES = new HashMap<>();

    static {
        UI_LANGUAGES.put("zh", "中文");
        UI_LANGUAGES.put("en", "English");
    }

    public static void setBackgroundRecursively(@NotNull Component component) {
        setBackgroundRecursively(component, UIUtils.backgroundColor());
    }

    public static void setBackgroundRecursively(@NotNull Component component, Color color) {
        if (component instanceof JBPanel || component instanceof JBScrollPane || component instanceof JBTextArea) {
            component.setBackground(color);
        }

        if (component instanceof Container) {
            for (Component c : ((Container) component).getComponents()) {
                setBackgroundRecursively(c, color);
            }
        }
    }

    public static Color borderColor() {
        return UIUtil.getBoundsColor();
    }

    public static Color backgroundColor() {
        return UIUtil.getPanelBackground();
    }

    public static Color highlightBackgroundColor() {
//        return UIUtil.isUnderDarcula() ? new Color(54, 57, 59) : new Color(245, 249, 255);
        return new JBColor(new Color(255, 234, 234), new Color(105, 36, 36));
    }

    public static Color highlightBorderColor() {
        return new JBColor(new Color(221, 64, 64), new Color(221, 64, 64));
    }


    public static <T extends JBPanel<T>> JBPanel<T> createBorderLayoutPanel() {
        return new JBPanel<>(new BorderLayout());
    }


    public static JBPanel wrappedInBorderLayoutPanel(JComponent component, Object constraints) {
        JBPanel panel = createBorderLayoutPanel();
        panel.add(component, constraints);
        return panel;
    }


    public static JBLabel createHorizontalAlignmentCenterLabel(String text) {
        JBLabel ret = new JBLabel(text);
        ret.setHorizontalAlignment(SwingConstants.CENTER);
        return ret;
    }

    public static JBLabel createHorizontalAlignmentCenterLabel(String text, Font font) {
        JBLabel ret = createHorizontalAlignmentCenterLabel(text);
        ret.setFont(font);
        return ret;
    }

    @NotNull
    public static <T extends JBPanel<T>> JBPanel<T> createBoxLayoutPanel(int axis) {
        JBPanel<T> panel = new JBPanel<>();
        BoxLayout layout = new BoxLayout(panel, axis);
        panel.setLayout(layout);
        return panel;
    }

    @NotNull
    public static Pair<String, Icon> typeInfo(String type) {
        return switch (type) {
            case "BUG" -> new Pair<>(ResourcesLoader.getString("issueType.bug"), PluginIcons.BUGS);
            case "VULNERABILITY" ->
                    new Pair<>(ResourcesLoader.getString("issueType.vulnerability"), PluginIcons.VULNERABILITY);
            case "CODE_SMELL" -> new Pair<>(ResourcesLoader.getString("issueType.codeSmell"), PluginIcons.CODE_SMELL);
            case "SECURITY_HOTSPOT" ->
                    new Pair<>(ResourcesLoader.getString("issueType.securityHotspot"), PluginIcons.SECURITY_HOTSPOT);
            default -> new Pair<>("", null);
        };
    }

    @NotNull
    public static Pair<String, Icon> severityInfo(String severity) {
        return switch (severity) {
            case "BLOCKER" -> new Pair<>(ResourcesLoader.getString("severityType.blocker"), PluginIcons.BLOCKER);
            case "CRITICAL" -> new Pair<>(ResourcesLoader.getString("severityType.critical"), PluginIcons.CRITICAL);
            case "MAJOR" -> new Pair<>(ResourcesLoader.getString("severityType.major"), PluginIcons.MAJOR);
            case "MINOR" -> new Pair<>(ResourcesLoader.getString("severityType.minor"), PluginIcons.MINOR);
            case "INFO" -> new Pair<>(ResourcesLoader.getString("severityType.info"), PluginIcons.INFO);
            default -> new Pair<>("", null);
        };
    }

    @NotNull
    public static Icon getSeverityIcon(List<SeverityType> severities) {
        int severity = severities.stream().max(Comparator.comparingInt(SeverityType::severity)).orElse(SeverityType.ANY).severity();

        if (severity >= 10) {
            return PluginIcons.BLOCKER;
        }
        if (severity >= 7) {
            return PluginIcons.CRITICAL;
        }
        if (severity >= 5) {
            return PluginIcons.MAJOR;
        }
        if (severity >= 3) {
            return PluginIcons.MINOR;
        }
        if (severity >= 1) {
            return PluginIcons.INFO;
        }
        return PluginIcons.ISSUE;
    }

    public static JBTextArea createWrapLabelLikedTextArea(String text) {
        JBTextArea textArea = new JBTextArea(text);
        textArea.setFont(UIUtil.getLabelFont());
        textArea.setEditable(false);
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);
//        textArea.setOpaque(true);
//        textArea.setBackground(UIUtils.backgroundColor());
        return textArea;
    }

    public static void navigateToOffset(PsiFile psiFile, int offset) {
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(psiFile.getProject(), psiFile.getVirtualFile(), offset);
        openFileDescriptor.navigate(true);
    }

    public static void navigateToLine(PsiFile psiFile, int line) {
        OpenFileDescriptor openFileDescriptor = new OpenFileDescriptor(psiFile.getProject(), psiFile.getVirtualFile(), line, 0);
        openFileDescriptor.navigate(true);
    }

    public static Collection<String> getAllUILanguagesDesc() {
        return UI_LANGUAGES.values();
    }

    public static String getLocaleByLanguageDesc(@Nonnull String languageDesc) {
        for (Map.Entry<String, String> item : UI_LANGUAGES.entrySet()) {
            if (item.getValue().equals(languageDesc)) {
                return item.getKey();
            }
        }
        return "";
    }

    public static String getLanguageDescByLocale(@Nonnull String locale) {
        for (Map.Entry<String, String> item : UI_LANGUAGES.entrySet()) {
            if (item.getKey().equals(locale)) {
                return item.getValue();
            }
        }
        return "";
    }

}
