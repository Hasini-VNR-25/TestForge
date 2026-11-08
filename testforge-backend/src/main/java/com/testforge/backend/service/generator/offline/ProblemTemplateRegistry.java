package com.testforge.backend.service.generator.offline;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * B2: Registry selecting the problem template with the highest classifier score.
 * New templates can be added by implementing ProblemTemplate without modifying existing templates.
 */
@Component
public class ProblemTemplateRegistry {

    private final List<ProblemTemplate> templates;

    public ProblemTemplateRegistry(List<ProblemTemplate> templates) {
        this.templates = (templates != null && !templates.isEmpty())
                ? templates
                : List.of(new ArrayTemplate(), new StringTemplate(), new NumericTemplate());
    }

    /**
     * Selects the template with the highest matching score for the given analysis.
     *
     * @param analysis problem analysis result
     * @return matching ProblemTemplate, or default to ArrayTemplate if none score > 0
     */
    public ProblemTemplate getBestTemplate(AnalysisResult analysis) {
        ProblemTemplate best = null;
        double highestScore = -1.0;

        for (ProblemTemplate template : templates) {
            double score = template.matchScore(analysis);
            if (score > highestScore) {
                highestScore = score;
                best = template;
            }
        }

        if (best == null || highestScore <= 0.0) {
            // Default fallback
            return templates.stream()
                    .filter(t -> t instanceof ArrayTemplate)
                    .findFirst()
                    .orElse(templates.get(0));
        }

        return best;
    }

    public List<ProblemTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }
}
