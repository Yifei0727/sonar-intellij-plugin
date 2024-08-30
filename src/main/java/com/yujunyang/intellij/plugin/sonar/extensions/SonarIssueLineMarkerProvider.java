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

import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.yujunyang.intellij.plugin.sonar.config.ProjectSettings;
import com.yujunyang.intellij.plugin.sonar.core.AbstractIssue;
import com.yujunyang.intellij.plugin.sonar.core.AnalyzeState;
import com.yujunyang.intellij.plugin.sonar.core.DuplicatedBlocksIssue;
import com.yujunyang.intellij.plugin.sonar.core.SeverityType;
import com.yujunyang.intellij.plugin.sonar.gui.common.UIUtils;
import com.yujunyang.intellij.plugin.sonar.gui.popup.LineMarkerProviderPopupPanel;
import com.yujunyang.intellij.plugin.sonar.resources.ResourcesLoader;
import com.yujunyang.intellij.plugin.sonar.service.ProblemCacheService;
import org.jetbrains.annotations.NotNull;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SonarIssueLineMarkerProvider implements LineMarkerProvider {

    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        if (element.getFirstChild() != null) {
            return null;
        }

        final Project project = element.getProject();
        if (!AnalyzeState.get(project).isIdle()) {
            return null;
        }

        final ProblemCacheService problemCacheService = ProblemCacheService.getInstance(project);
        final Map<PsiFile, List<AbstractIssue>> issues = problemCacheService.getIssues();
        final PsiFile psiFile = element.getContainingFile();
        if (!issues.containsKey(psiFile)) {
            return null;
        }

        final List<AbstractIssue> currentFileIssues = issues.get(psiFile);
        final List<AbstractIssue> matchedIssues = new ArrayList<>();
        for (AbstractIssue item : currentFileIssues) {
            if (element == firstLeafOrNull(item.getTextRangePsiElement())) {
                matchedIssues.add(item);
            }
        }

        if (matchedIssues.isEmpty()) {
            return null;
        }

        final SeverityType severityType = ProjectSettings.getInstance(project).getSeverityType();
        List<SeverityType> severityTypes = matchedIssues
                .stream()
                .map(e -> SeverityType.fromName(e.getSeverity()))
                .filter(s -> s.severity() >= severityType.severity())
                .toList();
        final GutterIconNavigationHandler<PsiElement> navigationHandler = new IssueGutterIconNavigationHandler(matchedIssues, element);
        final TooltipProvider tooltipProvider = new TooltipProvider(matchedIssues);
        if (!severityTypes.isEmpty()) {
            return new LineMarkerInfo<>(element, element.getTextRange(),
                    UIUtils.getSeverityIcon(severityTypes),
                    tooltipProvider,
                    navigationHandler,
                    GutterIconRenderer.Alignment.CENTER,
                    () -> tooltipProvider.fun(element));
        }
        return null;
    }

    private static PsiElement firstLeafOrNull(@NotNull PsiElement element) {
        LeafElement firstLeaf = TreeUtil.findFirstLeaf(element.getNode());
        return firstLeaf != null ? firstLeaf.getPsi() : null;
    }

    private record IssueGutterIconNavigationHandler(List<AbstractIssue> issues,
                                                    PsiElement psiElement) implements GutterIconNavigationHandler<PsiElement> {


        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            JBPopupFactory jbPopupFactory = JBPopupFactory.getInstance();
            LineMarkerProviderPopupPanel contentPanel = new LineMarkerProviderPopupPanel(elt.getProject(), issues);
            JBPopup popup = jbPopupFactory.createComponentPopupBuilder(contentPanel, null).createPopup();
            popup.show(new RelativePoint(e));
            contentPanel.setPopup(popup);
        }
    }

    private record TooltipProvider(List<AbstractIssue> issues) implements Function<PsiElement, String> {

        @Override
        public String fun(final PsiElement psiElement) {
            return getTooltipText();
        }

        private String getTooltipText() {
            final StringBuilder buffer = new StringBuilder();
            buffer.append("<!DOCTYPE html><html lang=\"en\"><head><style>h3{margin:0;}</style></head><body>");
            buffer.append("<h3>");
            buffer.append(ResourcesLoader.getString("report.fileSummary", ResourcesLoader.getString("analysis.report.tips.prefix"), issues.size()));
            buffer.append("</h3>");
            for (AbstractIssue issue : issues) {
                buffer.append("<p>");
                buffer.append(issue.getName());
                buffer.append("</p>");
                if (issue instanceof DuplicatedBlocksIssue duplicatedBlocksIssue) {
                    for (DuplicatedBlocksIssue.Duplicate duplicate : duplicatedBlocksIssue.getDuplicates()) {
                        buffer.append("<p>");
                        buffer.append("<span>");
                        buffer.append(duplicateInfo(duplicate));
                        buffer.append("</span>");
                        buffer.append("</p>");
                    }
                }
            }

            buffer.append("</body></html>");
            return buffer.toString();
        }

        private String duplicateInfo(DuplicatedBlocksIssue.Duplicate duplicate) {
            return String.format("<span>%s</span><span>%s-%s</span>", duplicate.path(), duplicate.startLine(), duplicate.endLine());
        }
    }
}
