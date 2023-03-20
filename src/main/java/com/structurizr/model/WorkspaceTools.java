package com.structurizr.model;

import com.structurizr.Workspace;
import com.structurizr.documentation.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Tools for working with Structurizr workspaces.
 */
public class WorkspaceTools {

    public static final String EXTERNAL_TAG = "External";

    private WorkspaceTools() {
    }

    /**
     * Merges the models from a list of workspaces into one model.
     * Elements tagged as 'External' are not merged.  This allows an 'External'
     * declaration in one workspace to be resolved by a definition in another.
     * @throws RuntimeException if any 'External' declarations don't have a definition
     * @throws UnsupportedOperationException for element types not yet handled
     */
    public static void merge(List<Workspace> workspaces, Model model) {

        // An element is declared external if it, or any parent has the External tag
        final Predicate<Element> isDeclaredExternal = element -> {
            for (Element e = element; null != e; e = e.getParent()) {
                if (e.hasTag(EXTERNAL_TAG)) {
                    return true;
                }
            }
            return false;
        };
        final Predicate<Element> isDefinitiveElement = Predicate.not(isDeclaredExternal);

        // Collect all workspace elements not declared External
        final Map<String, List<Element>> elements =
                workspaces.stream()
                        .map(Workspace::getModel)
                        .map(Model::getElements)
                        .flatMap(Collection::stream)
                        .filter(isDefinitiveElement)
                        .collect(Collectors.groupingBy(element -> element.getClass().getSimpleName()));

        // Add each type of element to the merged model
        // There's no generic way to do this, so use the appropriate Model method
        // Also, adding elements in a hierarchical order allows parents to be resolved
        // TODO: Cover remaining element classes.  Currently only StaticStructureElements are copied
        Optional.ofNullable(elements.get("Person")).ifPresent(persons -> persons.stream()
                .map(element -> (Person) element)
                .forEach(person ->
                        copyAttributes(person, model.addPerson(Location.Unspecified, person.getName(), person.getDescription()))
                ));

        Optional.ofNullable(elements.get("SoftwareSystem")).ifPresent(systems -> systems.stream()
                .map(element -> (SoftwareSystem) element)
                .forEach(softwareSystem -> {
                    final SoftwareSystem newSoftwareSystem = model.addSoftwareSystem(Location.Unspecified, softwareSystem.getName(), softwareSystem.getDescription());
                    copyAttributes(softwareSystem, newSoftwareSystem);
                    final Documentation documentation = new Documentation();
                    DocumentationTools.copy(softwareSystem.getDocumentation(), documentation);
                    newSoftwareSystem.setDocumentation(documentation);
                }));

        Optional.ofNullable(elements.get("Container")).ifPresent(containers -> containers.stream()
                .map(element -> (Container) element)
                .forEach(container -> {
                    final SoftwareSystem parent = (SoftwareSystem) model.getElementWithCanonicalName(container.getParent().getCanonicalName());
                    copyAttributes(container, model.addContainer(parent, container.getName(), container.getDescription(), container.getTechnology()));
                }));

        Optional.ofNullable(elements.get("Component")).ifPresent(components -> components.stream()
                .map(element -> (Component) element)
                .forEach(component -> {
                    final Container parent = (Container) model.getElementWithCanonicalName(component.getParent().getCanonicalName());
                    copyAttributes(component, model.addComponent(parent, component.getName(), component.getDescription(), component.getTechnology()));
                }));

        // Throw exception if any unsupported types are present
        final List<String> supported = List.of("Person", "SoftwareSystem", "Container", "Component");
        final List<String> unsupported = elements.keySet().stream()
                .filter(s -> !supported.contains(s))
                .collect(Collectors.toList());

        if (!unsupported.isEmpty()) {
            throw new UnsupportedOperationException("The following element types are currently unsupported: "
                    + String.join(", ", unsupported));
        }

        // Throw exception if any externals are unresolved
        final List<String> unresolvedExternals = workspaces.stream()
                .map(Workspace::getModel)
                .map(Model::getElements)
                .flatMap(Collection::stream)
                .filter(isDeclaredExternal)
                .map(Element::getCanonicalName)
                .filter(name -> null == model.getElementWithCanonicalName(name))
                .sorted()
                .collect(Collectors.toList());

        if (!unresolvedExternals.isEmpty()) {
            throw new RuntimeException("The following elements are declared External, but aren't defined anywhere: "
                    + String.join(", ", unresolvedExternals));
        }

        // A definitive relationship is one that is not tagged External,
        // and whose source is definitive
        final Predicate<Relationship> isDefinitiveRelationship =
                relationship -> !relationship.hasTag(EXTERNAL_TAG)
                        && isDefinitiveElement.test(relationship.getSource())
                ;

        // Copy the relationships from all workspaces
        // Note: this will fail if the source or destination hasn't been merged,
        // although in practice this should be covered by previous checks
        workspaces.stream()
                .map(Workspace::getModel)
                .map(Model::getRelationships)
                .flatMap(Collection::stream)
                .filter(isDefinitiveRelationship)
                .forEach(relationship -> {
                    final String sourceName = relationship.getSource().getCanonicalName();
                    final String destinationName = relationship.getDestination().getCanonicalName();

                    final Element source = Optional.ofNullable(model.getElementWithCanonicalName(sourceName))
                            .orElseThrow(() -> new RuntimeException("Source " + sourceName + " has not been defined"));

                    final Element destination = Optional.ofNullable(model.getElementWithCanonicalName(destinationName))
                            .orElseThrow(() -> new RuntimeException("Destination " + destinationName + " has not been defined"));

                    final String[] tags = relationship.getTagsAsSet().toArray(new String[0]);

                    // Take advantage of the package-private Model::addRelationship
                    model.addRelationship(source, destination, relationship.getDescription(), relationship.getTechnology(), relationship.getInteractionStyle(), tags);
                });
    }

    private static void copyAttributes(StaticStructureElement original, StaticStructureElement copy) {
        copy.setTags(original.getTags());
        copy.setGroup(original.getGroup());
        copy.setUrl(original.getUrl());
        // Note: the getters created a (shallow) copy of the collection
        copy.setProperties(original.getProperties());
        copy.setPerspectives(original.getPerspectives());
    }
}
