workspace "Acme back-office" "Core bespoke internal systems" {

    model {
        ######
        # These elements are tagged as External
        # When workspaces are merged, their definitive version
        # is expected to be found in another workspace

        finance = SoftwareSystem "Finance system" "" "External"{
            Container "Web user interface"
            financeDB = Container "Database"
        }

        frontLine = SoftwareSystem "Front-line system" "" "External"

        csr = Person "Customer service rep" "" "External"
        ######

        ######
        # Definitive
        # These elements are defined (owned) by this workspace

        backOffice = SoftwareSystem "Back-office system" "Runs Acme's operations" "Tag1,Tag2" {
            !docs back-office-docs
            cds = Container "Customer data store"
            hub = Container "Integration hub" {
                -> financeDB "Queries"
                -> cds "Updates"
            }
            client = Container "Desktop client" {
                -> cds "Queries"
            }
            -> frontLine "Pushes decisions"
        }

        Person "Assessor" {
            -> client "Uses"

            # The following relationship is considered declarative by the merge tool
            # Its source (csr) is declared as External
            csr -> this "Discusses cases (declarative)"
        }
        ######
    }

    views {
        systemLandscape "Backofficesystem-Landscape" {
            include *
            autolayout
        }
        systemContext backOffice "Backofficesystem-SystemContext" {
            include *
            autolayout
            default
        }
        container backOffice "Backofficesystem-Container" {
            include *
            autolayout
        }
        !include styles.dsl
    }
}
