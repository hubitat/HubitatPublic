definition(
    name: "Switch-Lock",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Use Switch to Lock/Unlock",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this Switch-Lock", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "lock", "capability.lock", title: "Select Lock", submitOnChange: true, required: true
		}
	}
}

def installed() {
	initialize()
}

def updated() {
	unsubscribe()
	initialize()
}

def initialize() {
	def switchDev = getChildDevice("SwitchLock_${app.id}")
	if(!switchDev) switchDev = addChildDevice("hubitat", "Virtual Switch", "SwitchLock_${app.id}", null, [label: lock.label, name: lock.name])
	subscribe(switchDev, "switch", handler)
}

def handler(evt) {
	if(evt.value == "on") lock.lock() else lock.unlock()
	log.info "$lock ${evt.value == "on" ? "locked" : "unlocked"}"
}
