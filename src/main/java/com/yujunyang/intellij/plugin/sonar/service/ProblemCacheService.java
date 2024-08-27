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

package com.yujunyang.intellij.plugin.sonar.service;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.yujunyang.intellij.plugin.sonar.core.AbstractIssue;
import com.yujunyang.intellij.plugin.sonar.core.AnalyzeScope;
import com.yujunyang.intellij.plugin.sonar.core.DuplicatedBlocksIssue;
import com.yujunyang.intellij.plugin.sonar.core.Issue;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

public class ProblemCacheService {
    private final Project project;
    private final Set<String> filters;
    private final ConcurrentMap<PsiFile, List<AbstractIssue>> issues;
    private final CopyOnWriteArraySet<String> profileLanguages;
    private final CopyOnWriteArraySet<String> ignoreRules;

    private boolean initialized = false;

    private long bugCount;
    private long codeSmellCount;
    private long vulnerabilityCount;
    private long duplicatedBlocksCount;
    private long securityHotSpotCount;
    private long ignoreIssueCount;

    private AnalyzeScope analyzeScope;

    public ProblemCacheService(Project project) {
        this.project = project;
        issues = new ConcurrentHashMap<>();
        bugCount = 0;
        codeSmellCount = 0;
        vulnerabilityCount = 0;
        duplicatedBlocksCount = 0;
        securityHotSpotCount = 0;

        profileLanguages = new CopyOnWriteArraySet<>();
        ignoreRules = new CopyOnWriteArraySet<>();
        ignoreIssueCount = 0;

        filters = new HashSet<>();
    }

    public ConcurrentMap<PsiFile, List<AbstractIssue>> getIssues() {
        return issues;
    }

    public ConcurrentMap<PsiFile, List<AbstractIssue>> getFilteredIssues() {
        if (filters.isEmpty()) {
            return issues;
        }

        boolean includeCritical = filters.contains("CRITICAL");
        boolean includeBlocked = filters.contains("BLOCKER");
        boolean includeMajor = filters.contains("MAJOR");
        boolean includeMinor = filters.contains("MINOR");
        boolean includeInfo = filters.contains("INFO");
        boolean includeBug = filters.contains("BUG");
        boolean includeCodeSmell = filters.contains("CODE_SMELL");
        boolean includeVulnerability = filters.contains("VULNERABILITY");
        boolean includeSecurityHotspot = filters.contains("SECURITY_HOTSPOT");
        boolean includeDuplication = filters.contains("DUPLICATION");
        boolean filterByType = includeBug || includeCodeSmell || includeVulnerability || includeSecurityHotspot || includeDuplication;
        boolean includeUpdatedFiles = filters.contains("UPDATED_FILES");
        boolean includeNotUpdatedFiles = filters.contains("NOT_UPDATED_FILES");
        boolean filterByScope = includeUpdatedFiles || includeNotUpdatedFiles;
        boolean includeResolved = filters.contains("RESOLVED");
        boolean includeUnresolved = filters.contains("UNRESOLVED");
        boolean filterBySeverity = includeCritical || includeBlocked || includeMajor || includeMinor || includeInfo;
        boolean filterByStatus = includeResolved || includeUnresolved;

        List<PsiFile> changedFiles = GitService.getInstance(project).getChangedFiles();

        ConcurrentMap<PsiFile, List<AbstractIssue>> ret = new ConcurrentHashMap<>();
        issues.forEach((psiFile, issues) -> {
            List<AbstractIssue> retIssues = new ArrayList<>();
            for (AbstractIssue issue : issues) {
                boolean include = true;

                if (filterByType) {
                    include = false;

                    if (includeBug && issue.getType().equals("BUG")) {
                        include = true;
                    }
                    if (includeCodeSmell && issue.getType().equals("CODE_SMELL")) {
                        include = true;
                    }
                    if (includeVulnerability && issue.getType().equals("VULNERABILITY")) {
                        include = true;
                    }
                    if (includeSecurityHotspot && issue.getType().equals("SECURITY_HOTSPOT")) {
                        include = true;
                    }
                    if (includeDuplication && issue instanceof DuplicatedBlocksIssue) {
                        include = true;
                    }

                    if (!include) {
                        continue;
                    }
                }

                if (filterByScope) {
                    include = false;

                    if (includeUpdatedFiles && changedFiles.contains(issue.getPsiFile())) {
                        include = true;
                    }
                    if (includeNotUpdatedFiles && !changedFiles.contains(issue.getPsiFile())) {
                        include = true;
                    }

                    if (!include) {
                        continue;
                    }
                }

                if (filterBySeverity) {
                    include = false;
                    if (includeCritical && issue.getSeverity().equals("CRITICAL")) {
                        include = true;
                    }
                    if (includeBlocked && issue.getSeverity().equals("BLOCKER")) {
                        include = true;
                    }
                    if (includeMajor && issue.getSeverity().equals("MAJOR")) {
                        include = true;
                    }
                    if (includeMinor && issue.getSeverity().equals("MINOR")) {
                        include = true;
                    }
                    if (includeInfo && issue.getSeverity().equals("INFO")) {
                        include = true;
                    }

                    if (!include) {
                        continue;
                    }
                }

                if (filterByStatus) {
                    include = false;

                    if (includeResolved && issue.isFixed()) {
                        include = true;
                    }
                    if (includeUnresolved && !issue.isFixed()) {
                        include = true;
                    }

                    if (!include) {
                        continue;
                    }
                }

                if (include) {
                    retIssues.add(issue);
                }

            }
            if (retIssues.size() > 0) {
                ret.put(psiFile, retIssues);
            }
        });

        return ret;
    }

    public void setIssues(ConcurrentMap<PsiFile, List<AbstractIssue>> issues) {
        issues.forEach(((psiFile, issueList) -> {
            if (issueList.size() > 0) {
                this.issues.put(psiFile, issueList);
            }
        }));
    }

    public long getBugCount() {
        return bugCount;
    }

    public long getCodeSmellCount() {
        return codeSmellCount;
    }

    public long getVulnerabilityCount() {
        return vulnerabilityCount;
    }

    public long getDuplicatedBlocksCount() {
        return duplicatedBlocksCount;
    }

    public long getSecurityHotSpotCount() {
        return securityHotSpotCount;
    }

    public CopyOnWriteArraySet<String> getProfileLanguages() {
        return profileLanguages;
    }

    public CopyOnWriteArraySet<String> getIgnoreRules() {
        return ignoreRules;
    }

    public long getIgnoreIssueCount() {
        return ignoreIssueCount;
    }

    public void setStats(long bugCount, long codeSmellCount, long vulnerabilityCount, long duplicatedBlocksCount, long securityHotSpotCount) {
        initialized = true;
        this.bugCount = bugCount;
        this.codeSmellCount = codeSmellCount;
        this.vulnerabilityCount = vulnerabilityCount;
        this.duplicatedBlocksCount = duplicatedBlocksCount;
        this.securityHotSpotCount = securityHotSpotCount;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setAnalyzeScope(AnalyzeScope analyzeScope) {
        this.analyzeScope = analyzeScope;
    }

    public AnalyzeScope getAnalyzeScope() {
        return analyzeScope;
    }

    public void reset() {
        initialized = false;
        issues.clear();
        bugCount = 0;
        codeSmellCount = 0;
        vulnerabilityCount = 0;
        duplicatedBlocksCount = 0;
        securityHotSpotCount = 0;

        profileLanguages.clear();
        ignoreRules.clear();
        ignoreIssueCount = 0;

        filters.clear();
    }

    public long getUpdatedFilesIssueCount() {
        List<PsiFile> changedFiles = GitService.getInstance(project).getChangedFiles();
        long count = 0;
        for (Map.Entry<PsiFile, List<AbstractIssue>> entry : issues.entrySet()) {
            PsiFile psiFile = entry.getKey();
            List<AbstractIssue> issueList = entry.getValue();
            if (changedFiles.contains(psiFile)) {
                count += issueList.stream().filter(n -> n instanceof Issue).count();
                count += issueList.stream().anyMatch(n -> n instanceof DuplicatedBlocksIssue) ? 1 : 0;
            }
        }
        return count;
    }

    public long getNotUpdatedFilesIssueCount() {
        return issueTotalCount() - getUpdatedFilesIssueCount();
    }

    public long getFixedIssueCount() {
        long count = 0;
        for (Map.Entry<PsiFile, List<AbstractIssue>> entry : issues.entrySet()) {
            List<AbstractIssue> issueList = entry.getValue();
            long normalIssueFixedCount = issueList.stream().filter(n -> n instanceof Issue && n.isFixed()).count();
            boolean duplicationIssueFixed = issueList.stream().filter(n -> n instanceof DuplicatedBlocksIssue && n.isFixed()).count() > 0;
            count += normalIssueFixedCount + (duplicationIssueFixed ? 1 : 0);
        }
        return count;
    }

    public long getUnresolvedIssueCount() {
        return issueTotalCount() - getFixedIssueCount();
    }

    public long issueTotalCount() {
        return bugCount + codeSmellCount + vulnerabilityCount + securityHotSpotCount;
    }

    public Set<String> getFilters() {
        return filters;
    }

    public static ProblemCacheService getInstance(@NotNull Project project) {
        return project.getService(ProblemCacheService.class);
    }

    public long getBlockerCount() {
        return issues.values().stream().flatMap(Collection::stream).filter(n -> n.getSeverity().equals("BLOCKER")).count();
    }

    public long getCriticalCount() {
        return issues.values().stream().flatMap(Collection::stream).filter(n -> n.getSeverity().equals("CRITICAL")).count();
    }

    public long getMajorCount() {
        return issues.values().stream().flatMap(Collection::stream).filter(n -> n.getSeverity().equals("MAJOR")).count();
    }

    public long getMinorCount() {
        return issues.values().stream().flatMap(Collection::stream).filter(n -> n.getSeverity().equals("MINOR")).count();
    }

    public long getInfoCount() {
        return issues.values().stream().flatMap(Collection::stream).filter(n -> n.getSeverity().equals("INFO")).count();
    }
}
