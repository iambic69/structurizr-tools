workspace "ACME finance" {

    model {
        ######
        # These elements are tagged as External
        # When workspaces are merged, their definitive version
        # is expected to be found in another workspace

        backOffice = SoftwareSystem "Back-office system" "" "External"
        ######

        ######
        # Definitive
        # These elements are defined (owned) by this workspace

        finance = SoftwareSystem "Finance system" {
            Container "Web user interface" "Modern web UI for finance system"
            financeDB = Container "Database"
        }

        Group "Admin team" {
            Person "Accountant" {
                -> finance "Uses"
            }
            Person "Administrator" {
                -> backOffice "Uses"
                -> finance "Uses"
            }
        }
        ######

        # The following relationship is considered declarative by the merge tool
        # Its source (backOffice) is declared as External
        backOffice -> financeDB "Queries"
    }

    views {
        systemContext finance "Finance-SystemContext" {
            include *
            autolayout
        }
        container finance "Finance-Container" {
            include *
            autolayout
        }
        !include styles.dsl
    }
}