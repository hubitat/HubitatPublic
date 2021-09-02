definition(
	name: "Battery Notifier",
	namespace: "hubitat",
	author: "Bruce Ravenel",
	description: "Battery Notifier",
	installOnOpen: true,
	category: "Convenience",
	iconUrl: "",
	iconX2Url: ""
)

preferences {
	page(name: "mainPage")
}

def mainPage() {
	dynamicPage(name: "mainPage", title: "Battery Notifier", uninstall: true, install: true) {
		section {
			input "devs", "capability.*", title: "Select devices", submitOnChange: true, multiple: true
			input "notice", "capability.notification", title: "Select notification device", submitOnChange: true
		}
	}
}

def updated() {
	unschedule()
	initialize()
}

def installed() {
	initialize()
}

void initialize() {
	schedule("0 0 9 ? * * *", handler)
}

void handler() {
	List noReport = []
	String s = ""
	def rightNow = new Date()
	devs.each {
		def lastTime = it.events(max: 1).date
		if(lastTime) {
			def minutes = ((rightNow.time - lastTime.time) / 60000).toInteger()
			if(minutes < 0) minutes += 1440
			if(minutes > 1440) noReport += it
		} else noReport += it
	}
	if(noReport) {
		noReport.each{s += "$it.displayName, "}
		notice.deviceNotification("${s[0..-3]} did not report")
	} else notice.deviceNotification("All devices reported")
}
