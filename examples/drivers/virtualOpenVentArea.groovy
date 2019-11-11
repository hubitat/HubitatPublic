metadata {
    definition (name: "Virtual Open Vent Area", namespace: "hubitat", author: "Bruce Ravenel") {
        capability "Sensor"
        command "setOpenVentArea", ["NUMBER"]
        attribute "openVentArea", "Number"

    }
    preferences {
        input name: "txtEnable", type: "bool", title: "Enable descriptionText logging", defaultValue: true
    }
}

def installed() {
    log.warn "installed..."
    setOpenVentArea(0)
}

def updated() {
    log.info "updated..."
    log.warn "description logging is: ${txtEnable == true}"
}

def parse(String description) {
}

def setOpenVentArea(area) {
    def descriptionText = "${device.displayName} was set to $area"
    if (txtEnable) log.info "${descriptionText}"
    sendEvent(name: "openVentArea", value: area, unit: "sq. m", descriptionText: descriptionText)
}
