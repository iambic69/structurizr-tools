# Tweaked version of the structurizr.com default styles
# Avoids the need to download the default theme
styles {
    element Element {
        shape RoundedBox
        fontSize 42
    }
    element "Software System" {
        background #1168bd
        colour #ffffff
    }
    element "Container" {
        background #438dd5
        colour #ffffff
    }
    element "Component" {
        background #85bbf0
        colour #000000
    }
    element "Person" {
        background #08427b
        colour #ffffff
        shape Person
    }
    element "Infrastructure Node" {
        background #ffffff
    }
}