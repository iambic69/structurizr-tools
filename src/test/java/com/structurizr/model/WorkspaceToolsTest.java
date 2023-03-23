package com.structurizr.model;

import com.structurizr.Workspace;
import com.structurizr.documentation.Documentation;
import com.structurizr.documentation.Section;
import com.structurizr.dsl.StructurizrDslParser;
import com.structurizr.dsl.StructurizrDslParserException;
import com.structurizr.util.WorkspaceUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class WorkspaceToolsTest {

    private Workspace workspace;

    @BeforeEach
    public void beforeEach() {
        workspace = new Workspace("Merged", "A combination of several workspaces");
    }

    @Test
    public void givenModelWithUnresolvedExternals_throwsExceptionDetailingThem() throws StructurizrDslParserException {
        final List<Workspace> workspaces = Collections.singletonList(
                parseDsl("""
                        workspace {
                            model {
                                SoftwareSystem "Foo" "" "External"
                                SoftwareSystem "Bar" "" "External"
                            }
                        }
                        """)
        );
        final RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () ->
                WorkspaceTools.merge(workspaces, workspace.getModel())
        );
        Assertions.assertEquals(
                "The following elements are declared External, but aren't defined anywhere: SoftwareSystem://Bar, SoftwareSystem://Foo",
                exception.getMessage()
        );
    }

    @Test
    public void givenSelfContainedModel_MergesWithoutException() throws StructurizrDslParserException, IOException {
        final Model model = workspace.getModel();
        WorkspaceTools.merge(Collections.singletonList(parseDsl("""
                workspace "Simple" {
                    model {
                        ss = SoftwareSystem "System"
                        Person "User" {
                            -> ss "Uses"
                        }
                    }
                }
                """)), model);

        // Report content of 'merged' model
        try (PrintWriter writer = new PrintWriter(new FileWriter("target/acme.txt"))) {
            model.getElements().forEach(writer::println);
            model.getRelationships().forEach(writer::println);
        }
    }

    @Test
    public void givenSystemWithSameName_ReportsDuplication() throws StructurizrDslParserException {
        final List<Workspace> workspaces = new ArrayList<>(parseAcmeWorkspaces());
        workspaces.add(parseDsl("""
                workspace {
                    model {
                        SoftwareSystem "Back-office system"
                    }
                }
                """));
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () ->
                WorkspaceTools.merge(workspaces, workspace.getModel())
        );
        Assertions.assertEquals("A top-level element named 'Back-office system' already exists.", exception.getMessage());
    }

    @Test
    public void givenWorkspacesWithExternalRef_ResolvesTheExternal() throws Exception {
        final Model model = workspace.getModel();
        WorkspaceTools.merge(parseAcmeWorkspaces(), model);

        final SoftwareSystem backOffice = (SoftwareSystem) model.getElementWithCanonicalName("SoftwareSystem://Back-office system");
        Assertions.assertEquals("Runs Acme's operations", backOffice.getDescription());

        // Report content of 'merged' model
        try (PrintWriter writer = new PrintWriter(new FileWriter("target/acme.txt"))) {
            model.getElements().forEach(writer::println);
            model.getRelationships().forEach(writer::println);
        }

        // Add views
        workspace.getViews().createDefaultViews();

        // Save the workspace as JSON
        WorkspaceUtils.saveWorkspaceToJson(workspace, new File("target/acme.json"));
    }

    @Test
    public void givenGroupedSystems_mergeRetainsGrouping() throws StructurizrDslParserException {
        final List<Workspace> workspaces = Collections.singletonList(parseDsl("""
                workspace {
                    model {
                        group "Group of two" {
                            SoftwareSystem "One"
                            SoftwareSystem "Two"
                        }
                    }
                }
                """));

        WorkspaceTools.merge(workspaces, workspace.getModel());

        final SoftwareSystem two = (SoftwareSystem) workspace.getModel().getElementWithCanonicalName("SoftwareSystem://Two");
        Assertions.assertEquals("Group of two", two.getGroup());
    }

    @Test
    public void givenModelWithTechnologyAndTags_copiesThem() throws StructurizrDslParserException {
        final Model model = workspace.getModel();
        WorkspaceTools.merge(parseAcmeWorkspaces(), model);

        final SoftwareSystem backOffice = (SoftwareSystem) model.getElementWithCanonicalName("SoftwareSystem://Back-office system");

        Assertions.assertEquals(
                new LinkedHashSet<>(List.of("Element", "Software System", "Tag1", "Tag2")),
                backOffice.getTagsAsSet()
        );

        final Element csr = model.getElementWithCanonicalName("Person://Customer service rep");
        final Element assessor = model.getElementWithCanonicalName("Person://Assessor");
        final Relationship relationship = csr.getEfferentRelationshipWith(assessor);
        Assertions.assertEquals("Phone call", relationship.getTechnology());
        Assertions.assertEquals(
                new LinkedHashSet<>(List.of("Relationship", "Important Tag")),
                relationship.getTagsAsSet()
        );
    }

    @Test
    public void givenSystemDocumentation_mergeCopiesIt() throws StructurizrDslParserException {
        WorkspaceTools.merge(parseAcmeWorkspaces(), workspace.getModel());

        final SoftwareSystem backOffice = (SoftwareSystem) workspace.getModel().getElementWithCanonicalName("SoftwareSystem://Back-office system");

        final Documentation documentation = backOffice.getDocumentation();
        final List<Section> sections = documentation.getSections().stream()
                .sorted(Comparator.comparing(Section::getOrder))
                .toList();
        Assertions.assertEquals(3, sections.size());
        assertSection(sections.get(0), "00 Overview.adoc");
        assertSection(sections.get(1), "01 System Context.adoc");
        assertSection(sections.get(2), "02 Design.adoc");
    }

    @Test
    public void givenExternalSystem_DefinitiveContainerIsMerged() throws StructurizrDslParserException {
        WorkspaceTools.merge(parseAcmeWorkspaces(), workspace.getModel());

        // The Finance UI Container with the definitive description should have been copied
        final Element webUI = workspace.getModel().getElementWithCanonicalName("Container://Finance system.Web user interface");
        Assertions.assertEquals("Modern web UI for finance system", webUI.getDescription());
    }

    @Test
    public void givenSuccessfulMerge_workspaceDetailsCaptured() throws StructurizrDslParserException {
        WorkspaceTools.merge(parseAcmeWorkspaces(), workspace.getModel());

        // Collect all elements in the merged workspace, grouped by workspace name
        final Function<Element, String> workspaceName = element -> element.getProperties().getOrDefault("workspace-name", "Missing");
        final Map<String, List<Element>> elementsByWorkspace = workspace.getModel().getElements().stream()
                .sorted(Comparator.comparing(Element::getCanonicalName))
                .collect(Collectors.groupingBy(workspaceName));

        if (false) {
            // Print
            elementsByWorkspace.forEach((name, elements) -> {
                System.out.println("name = " + name);
                elements.stream()
                        .map(element -> "  " + element.getCanonicalName())
                        .map(s -> "  " + s)
                        .forEach(System.out::println);
            });
        }

        // Assert that the elements merged from one workspace are as expected
        final List<String> backOfficeElementNames = elementsByWorkspace.get("Acme back-office").stream()
                .map(Element::getCanonicalName).toList();
        Assertions.assertEquals(List.of(
                "Container://Back-office system.Customer data store",
                "Container://Back-office system.Desktop client",
                "Container://Back-office system.Integration hub",
                "Person://Assessor",
                "SoftwareSystem://Back-office system"
        ), backOfficeElementNames);
    }

    @Test
    public void givenEfferentRelationshipFromExternal_DefinitiveRelationshipIsMerged() throws StructurizrDslParserException {
        final Model model = workspace.getModel();
        WorkspaceTools.merge(parseAcmeWorkspaces(), model);

        // After the merge, the only relationship between the CSR and Assessor should be the definitive one
        final Element csr = model.getElementWithCanonicalName("Person://Customer service rep");
        final Element assessor = model.getElementWithCanonicalName("Person://Assessor");
        final Set<Relationship> relationships = csr.getEfferentRelationshipsWith(assessor);
        final Set<String> descriptions = relationships.stream().map(Relationship::getDescription).collect(Collectors.toSet());
        Assertions.assertEquals(
                new LinkedHashSet<>(List.of("Discusses cases")),
                descriptions
        );
    }

    private static List<Workspace> parseAcmeWorkspaces() throws StructurizrDslParserException {
        return List.of(
                parseDslFile("workspaces/back-office.dsl"),
                parseDslFile("workspaces/frontline.dsl"),
                parseDslFile("workspaces/finance.dsl")
        );
    }

    private static void assertSection(Section section, String filename) {
        Assertions.assertEquals(filename, section.getFilename());
    }

    private static Workspace parseDsl(String dsl) throws StructurizrDslParserException {
        final StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(dsl);
        return parser.getWorkspace();
    }

    private static Workspace parseDslFile(String pathname) throws StructurizrDslParserException {
        final StructurizrDslParser parser = new StructurizrDslParser();
        parser.parse(new File(pathname));
        return parser.getWorkspace();
    }
}