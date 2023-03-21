workspace "Acme front-line systems" {
    model {

        ######
        # These elements are tagged as External
        # When workspaces are merged, their definitive version
        # is expected to be found in another workspace
        backOffice = SoftwareSystem "Back-office system" "" "External"
        assessor   = Person "Assessor" "" "External" {
            -> backOffice "Uses"
        }
        ######

        ######
        # Definitive
        # These elements are defined (owned) by this workspace
        frontLine = SoftwareSystem "Front-line system" {
            -> backOffice "Queries"
        }
        csr = Person "Customer service rep" {
            -> frontLine "Uses"
            -> assessor "Discusses cases" "Phone call" "Important Tag"
        }
        ######
    }

    views {
        systemLandscape "Frontlinesystem-Landscape" {
            include *
            autolayout
        }
        systemContext frontLine "Frontlinesystem-SystemContext" {
            include *
            autolayout
        }
        !include styles.dsl
    }
}
