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
			input "check", "button", title: "Check Now", state: "check"
			if(state.check) {
				paragraph handler()
				state.check = false
			}
		}
	}
}

def updated() {
	unschedule()
	unsubscribe()
	initialize()
}

def installed() {
	initialize()
}

void initialize() {
	schedule("0 30 9 ? * * *", handlerX)
}

String handler(note=false) {
	String s = ""
	def rightNow = new Date()
	devs.each {
		def lastTime = it.events(max: 1).date
		if(lastTime) {
			def minutes = ((rightNow.time - lastTime.time) / 60000).toInteger()
			if(minutes < 0) minutes += 1440
			if(minutes > 1440) s += (note ? "" : "<a href='/device/edit/$it.deviceId' target='_blank'>") + "$it.displayName, "
			
		} else s += (note ? "" : "<a href='/device/edit/$it.deviceId' target='_blank'>") + "$it.displayName, " + (note ? "" : "</a>")
	}
	if(note) notice.deviceNotification(s ? "[H]${s[0..-3]} did not report" : "All devices reported")
	else return s ? "${s[0..-3]} did not report" : "All devices reported"
}

void handlerX() {
	handler(true)
}
