package com.structurizr.documentation;

import java.util.Comparator;

public class DocumentationTools {
    /**
     * Copy Structurizr documentation.
     * @throws UnsupportedOperationException if there are any decisions, as these aren't yet supported
     */
    public static void copy(Documentation original, Documentation copy) {
        original.getSections().stream()
                .sorted(Comparator.comparing(Section::getOrder))
                .forEach(section -> {
                    final Section newSection = new Section(section.getFormat(), section.getContent());
                    newSection.setFilename(section.getFilename());
                    copy.addSection(newSection);
                });
        original.getImages().forEach(image ->
                copy.addImage(new Image(image.getName(), image.getType(), image.getContent()))
        );

        // TODO: add support for architecture decisions
        if (!original.getDecisions().isEmpty()) {
            throw new UnsupportedOperationException(DocumentationTools.class.getSimpleName() + ".copy does not yet support copying decisions");
        }
        original.getDecisions().forEach(decision -> {
            final Decision newDecision = new Decision();
            newDecision.setDate(decision.getDate());
            newDecision.setFormat(decision.getFormat());
            newDecision.setStatus(decision.getStatus());
            newDecision.setTitle(decision.getTitle());
            newDecision.setLinks(decision.getLinks()); // Have to deep-copy decision links, resolving their IDs
            copy.addDecision(newDecision);
        });
    }
}
