definition(
    name: "Energy Sum",
    namespace: "hubitat",
    author: "Bruce Ravenel",
    description: "Add up some energy reports",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "")

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: " ", install: true, uninstall: true) {
		section {
			input "thisName", "text", title: "Name this energy sum", submitOnChange: true
			if(thisName) app.updateLabel("$thisName")
			input "energymeters", "capability.energyMeter", title: "Select Energy Meters", submitOnChange: true, required: true, multiple: true
			if(energymeters) paragraph "Current sum is ${sumEnergy()}"
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
	def energySum = getChildDevice("EnergySum_${app.id}")
	if(!energySum) energySum = addChildDevice("hubitat", "Virtual Omni Sensor", "EnergySum_${app.id}", null, [label: thisName, name: thisName])
	energySum.setEnergy(sumEnergy())
	subscribe(energymeters, "energy", handler)
}

def sumEnergy() {
	def total = 0
	energymeters.each {total += it.currentEnergy}
	return total
}

def handler(evt) {
	def energySum = getChildDevice("EnergySum_${app.id}")
	def sum = sumEnergy()
	energySum.setEnergy(sum)
	log.info "Energy sum = $sum"
}
