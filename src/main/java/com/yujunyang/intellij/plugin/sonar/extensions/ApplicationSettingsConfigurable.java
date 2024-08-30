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

package com.yujunyang.intellij.plugin.sonar.extensions;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.ProjectManager;
import com.yujunyang.intellij.plugin.sonar.config.SonarQubeSettings;
import com.yujunyang.intellij.plugin.sonar.config.WorkspaceSettings;
import com.yujunyang.intellij.plugin.sonar.gui.settings.ApplicationSettingsPanel;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ApplicationSettingsConfigurable implements Configurable {
    NotificationGroup notificationGroup = NotificationGroup.balloonGroup("Sonar Intellij plugin Balloon Notification");
    ApplicationSettingsPanel applicationSettingsPanel;

    @Override
    public @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "SonarAnalyzer";
    }

    @Override
    public @Nullable JComponent createComponent() {
        applicationSettingsPanel = new ApplicationSettingsPanel();
        return applicationSettingsPanel;
    }

    @Override
    public boolean isModified() {
        WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance();
        Set<SonarQubeSettings> existConnections = workspaceSettings.sonarQubeConnections;
        Map<String, String> existProperties = workspaceSettings.sonarProperties;
        List<SonarQubeSettings> connections = applicationSettingsPanel.getConnections();
        Map<String, String> properties = applicationSettingsPanel.getProperties();
        String uiLanguageLocale = applicationSettingsPanel.getUILanguageLocale();

        if (existConnections.size() != connections.size()) {
            return true;
        }

        for (SonarQubeSettings n : existConnections) {
            if (connections.stream().noneMatch(m -> m.name.equals(n.name) && m.url.equals(n.url) && m.token.equals(n.token))) {
                return true;
            }
        }

        if (existProperties.size() != properties.size()) {
            return true;
        }

        for (Map.Entry<String, String> n : existProperties.entrySet()) {
            if (!(properties.containsKey(n.getKey()) && properties.get(n.getKey()).equals(n.getValue()))) {
                return true;
            }
        }

        if (!workspaceSettings.uiLanguageLocale.equals(uiLanguageLocale)) {
            return true;
        }

        return false;

    }

    @Override
    public void apply() throws ConfigurationException {
        WorkspaceSettings workspaceSettings = WorkspaceSettings.getInstance();
        List<SonarQubeSettings> connections = applicationSettingsPanel.getConnections();
        Map<String, String> properties = applicationSettingsPanel.getProperties();
        workspaceSettings.sonarQubeConnections = new HashSet<>(connections);
        workspaceSettings.sonarProperties = properties;

        boolean languageSwitched = !workspaceSettings.uiLanguageLocale.equals(applicationSettingsPanel.getUILanguageLocale());
        workspaceSettings.uiLanguageLocale = applicationSettingsPanel.getUILanguageLocale();
        if (languageSwitched && ProjectManager.getInstance().getOpenProjects().length > 0) {
            notificationGroup.createNotification(
                            "SonarAnalyzer",
                            ResourcesLoader.getString("settings.uiLanguages.switchSuccess"),
                            NotificationType.INFORMATION)
                    .notify(ProjectManager.getInstance().getOpenProjects()[0]);
        }
    }

    @Override
    public void reset() {
        applicationSettingsPanel.reset();
    }
}
