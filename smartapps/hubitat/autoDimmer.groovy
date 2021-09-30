/**
 *  Auto Dimmer V2.2
 *
 *  Author: Mike Maxwell 
 	
    2015-09-30	maxwell added dimmer specific level option (off)
	  2017-04-15	maxwell mods for Hubitat
    2021-09-30  maxwell cleanup for publish

	This software if free for Private Use. You may use and modify the software without distributing it.
 
	This software and derivatives may not be used for commercial purposes.
	You may not distribute or sublicense this software.
	You may not grant a sublicense to modify and distribute this software to third parties not included in the license.

	Software is provided without warranty and the software author/license owner cannot be held liable for damages.        
        
 */
import com.hubitat.app.DeviceWrapper
import groovy.transform.Field

@Field static Map luxDarkOpts = [
    options:[10:"10 Lux",25:"25 Lux",50:"50 Lux",75:"75 Lux",100:"100 Lux"]
]
@Field static Map luxDuskOpts = [
    options:[100:"100 Lux",125:"125 Lux",150:"150 Lux",175:"175 Lux",200:"200 Lux",300:"300 Lux",400:"400 Lux",500:"500 Lux",600:"600 Lux"]
]
@Field static Map luxBrightOpts = [
    options:[500:"500 Lux",1000:"1000 Lux",2000:"2000 Lux",3000:"3000 Lux",4000:"4000 Lux"]
]
@Field static Map dimDarkOpts = [
    options:[10:"10%",20:"20%",30:"30%",40:"40%",50:"50%",60:"60%",70:"70%",80:"80%",90:"90%",100:"100%"]
]
@Field static Map overrideDarkOpts = [
    options:[0:"Off",5:"5%",10:"10%",20:"20%",25:"25%",30:"30%",35:"35%",40:"40%",45:"45%",50:"50%",60:"60%",70:"70%",80:"80%",90:"90%",100:"100%"]
]

definition(
    name			: "Auto Dimmer",
    namespace		: "hubitat",
    author			: "Mike Maxwell",
    description		: "This add on smartApp automatically adjusts dimmer levels based on a lux sensor, fires from the switch on event.",
    category		: "My Apps",
    iconUrl			: "",
    iconX2Url		: "",
    iconX3Url		: ""
)

preferences {
	page(name: "main")
    page(name: "aboutMe", nextPage	: "main")
    page(name: "luxPage")
    page(name: "dimmersPage", nextPage	: "main")
    page(name: "overridePage")
    page(name: "dimmerOptions")
}

void installed() {
	state.anyOptionsSet = false
   	init()
}

void updated() {
	unsubscribe()
    init()
}

void init(){
   	subscribe(dimmers, "switch.on", dimHandler)
   	subscribe(luxOmatic, "illuminance", luxHandler)
}

void dimHandler(evt) {
    setDimmer(evt.device,false)
}

void luxHandler(evt = "ramp"){
    Boolean isAutoLux = false
    Boolean isPrestage = false
    dimmers.each {
        isAutoLux = settings."${it.deviceId}_autoLux" == true
        isPrestage = settings."${it.deviceId}_usePrestage" == true
        Boolean isOn = it.currentValue("switch") == "on"
        if (isAutoLux && isOn) {
            setDimmer(it,true)
        } else if (isPrestage && !isOn) {
            setDimmer(it,false)
        }
    }
}

String getCurrentLUXmode(){
	Integer crntLux = luxOmatic?.currentValue("illuminance")?.toInteger() ?: 0
  	state.luxValue = crntLux
  	String lMode = ""
  	if (crntLux == -1) lMode = "${crntLux}"
  	else {
   		if (crntLux < luxDark.toInteger()) {
    		lMode = "Dark"
    	} else if (crntLux < luxDusk.toInteger()) {
          	lMode = "Dusk"
  		} else if (crntLux < luxBright.toInteger()) {
          	lMode = "Overcast"
    	} else {
          	lMode = "Bright"
    	}      
    }
  	state.luxMode = lMode
  	return lMode
}

void setDimmer(DeviceWrapper dimmer,Boolean isRamp){
    	Integer newLevel = 0

        //get its current dim level
    	Integer crntDimmerLevel = dimmer.currentValue("level").toInteger()
      	if (crntDimmerLevel >= 99) crntDimmerLevel = 100
    
    	//get currentLux reading
    	Integer crntLux = luxOmatic.currentValue("illuminance").toInteger()
      
        String prefVar = dimmer.deviceId.toString()
    	String dimVar
    	if (crntLux < luxDark.toInteger()) {
        	prefVar = prefVar + "_dark"
        	dimVar = dimDark
    	} else if (crntLux < luxDusk.toInteger()) {
            prefVar = prefVar + "_dusk"
            dimVar = dimDusk
  		} else if (crntLux < luxBright.toInteger()) {
            prefVar = prefVar + "_day"
            dimVar = dimDay
    	} else {
    		prefVar = prefVar + "_bright"
        	dimVar = dimBright
    	}
     
    	Integer newDimmerLevel = (this."${prefVar}" ?: dimVar).toInteger()
		if (newDimmerLevel >= 99) newDimmerLevel = 100		
      
    	if ( newDimmerLevel == crntDimmerLevel ){
        	//log.info "${dimmer.displayName}, changeRequired: False"
        } else {
            if (isRamp) {
            	if (newDimmerLevel == 0){
                    log.info "${dimmer.displayName}, currentLevel:${crntDimmerLevel}%, requestedLevel:${newDimmerLevel}%, currentLux:${crntLux}"
	        		dimmer.off()
                } else {
            		String rampRate  = dimmer.deviceId.toString()
                	rampRate = rampRate + "_ramp"
                	Integer rampInt = (this."${rampRate}" ?: 2).toInteger()
                  	Integer rampLevel 
                	if (crntDimmerLevel < newDimmerLevel){
                		rampLevel = Math.min(crntDimmerLevel + rampInt, newDimmerLevel)
                      	//on the way up if ramp level >= 99 push it to 100
                    	if (rampLevel >= 99) rampLevel = 100
                	} else {	
                      	//we could be at 100, with a request to go down, so we need a minimum ramp of 2 to get around the 99 issue
                      	if (crntDimmerLevel == 100) rampInt = Math.max(rampInt,2)
                		rampLevel = Math.max(crntDimmerLevel - rampInt,newDimmerLevel)
                	}
                  	dimmer.setLevel(rampLevel)
                	if (rampLevel != newDimmerLevel){
                    	runIn(60,luxHandler)
                    } //else { log.debug "bailed on run in"  }
                }
            } else {
				if (newDimmerLevel == 0){
	        		dimmer.off()
                } else {
	        		dimmer.setLevel(newDimmerLevel)
                }

            }
        }
}

void setCurrentOverride(Integer did, String darkValue, String duskValue, String dayValue, String brightValue){
  	//get current mode, and the input value from above
  	String crntModeValue
  	switch (state.luxMode){
      case "Dark":
        if (darkValue != null) crntModeValue = darkValue
      	break
      case "Dusk":
        if (duskValue != null) crntModeValue = duskValue
      	break
      case "Overcast":
        if (dayValue != null) crntModeValue = dayValue
      	break
      case "Bright":
        if (brightValue != null) crntModeValue = brightValue
      	break
  	}
    if (crntModeValue == null) return
    
   	DeviceWrapper dimmer = dimmers.find{ it.deviceId == did.toInteger() }
   	//see if we need to do anything
   	String dState = dimmer.currentValue("switch")
   	Integer dValue = dimmer.currentValue("level")
   	if (dState  == "on"){
       	if (dValue != crntModeValue.toInteger()) {
            dimmer.setLevel(crntModeValue.toInteger())
        }
   	}
}

/* page methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
Map main(){
    Boolean isLuxComplete = luxPageComplete() == "complete"
    Boolean isDimComplete = dimmersPageComplete() == "complete"
	Boolean allComplete = isLuxComplete && isDimComplete  
    String toDo
  	String pageTitle = "Setup..."
    if (!isLuxComplete){
    	toDo = "luxPage"
    } else if (!isDimComplete){
    	toDo = "dimmersPage"
    } else {
      pageTitle = "Current LUX Mode: ${getCurrentLUXmode()}"
    }

	return dynamicPage(
    	name		: "main"
        ,title		: pageTitle
        ,nextPage	: toDo
        ,install	: allComplete
        ,uninstall	: true
        ){
            section(){
                     href(
                        name		: "ab"
                        ,title		: "About Me..." 
                        ,required	: false
                        ,page		: "aboutMe"
                        ,description: ""
                     )
                     if (isLuxComplete){
                         href(
                            name		: "lp"
                            ,title		: "Illuminance settings..." 
                            ,required	: false
                            ,page		: "luxPage"
                            ,description: ""
                            ,state		: luxPageComplete()
                        )
                    }
					if (isDimComplete){
                        href(
                            name		: "dp"
                            ,title		: "Dimmers and defaults..." 
                            ,required	: false
                            ,page		: "dimmersPage"
                            ,description: ""
                            ,state		: dimmersPageComplete()
                        )
                    }
                    if (allComplete){
                        href(
                            name		: "op"
                            ,title		: "Specific dimmer settings..." 
                            ,required	: false
                            ,page		: "overridePage"
                            ,description: ""
                            ,state		: anyOptionsSet()
                        )
					}
            }
	}
}

Map aboutMe(){
	return dynamicPage(name: "aboutMe"){
		section ("About Me"){
             paragraph 	"This add on smartApp automatically adjusts dimmer levels when dimmer(s) are turned on from physical switches or other smartApps.\n" +
						"Levels are set based on lux (illuminance) sensor readings and the dimmer levels that you specify." + 
						"This smartApp does not turn on dimmers directly, this allows you to retain all your existing on/off smartApps.\n"+
						"autoDimmer provides intelligent level management to your existing automations."
        }
    }
}

Map luxPage(){
    Boolean isDimComplete = dimmersPageComplete() == "complete"
    String toDo
    if (!isDimComplete){
    	toDo = "dimmersPage"
    } else if (!isDimComplete){
    	toDo = "main"
    }
  	String info = ""
  	if (state.luxMode && state.luxValue) info = "Current Mode: ${state.luxMode}, LUX: ${state.luxValue}"
    return dynamicPage(name: "luxPage",nextPage: toDo){
		section ("Illuminance settings"){
            input(
            	name		: "luxOmatic"
                ,title		: "Use this illuminance Sensor..."
                ,multiple	: false
                ,required	: true
                ,type		: "capability.illuminanceMeasurement"
				,submitOnChange : true
            )
			input(name: "test",title: "test",type: "button")
        }
      section("Select Lux levels"){
          	paragraph "${info}"
            input(
            	name		: "luxDark"
                ,title		: "It's Dark below this level..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: luxDarkOpts.options
            )
            input(
            	name		: "luxDusk"
                ,title		: "Dusk/Dawn is between Dark and here..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: luxDuskOpts.options
            )
            input(
            	name		: "luxBright"
                ,title		: "Overcast is between Dusk/Dawn and here, above this level it's considered Sunny."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: luxBrightOpts.options
            )
        }
	}
}

Map dimmersPage(){
  	String info = ""
  	if (state.luxMode && state.luxValue) info = "Current Mode: ${state.luxMode}, LUX: ${state.luxValue}"  
	return dynamicPage(name: "dimmersPage",title: "Dimmers and defaults"){
    	section ("Default dim levels for each brigtness range"){
          	paragraph "${info}"
            input(
                name		: "dimDark"
                ,title		: "When it's Dark out..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: dimDarkOpts.options
            )
             input(
                name		: "dimDusk"
                ,title		: "For Dusk/Dawn use this..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum",
                ,options	: dimDarkOpts.options
            )
            input(
                name		: "dimDay" 
                ,title		: "When it's Overcast..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: dimDarkOpts.options
            )
			input(
                name		: "dimBright" 
                ,title		: "When it's Bright..."
                ,multiple	: false
                ,required	: true
                ,type		: "enum"
                ,options	: dimDarkOpts.options
            )        
        }
		section (){
			input(
            	name			: "dimmers"
                ,multiple		: true
                ,required		: true
              	,submitOnChange	: true
                ,type			: "capability.switchLevel"
              	,title			: "Dimmers to manage"
            )
        }
	}
}

Map overridePage(){
	state.anyOptionsSet = false
	return dynamicPage(name: "overridePage"){
    	section("Specific dimmer settings"){
        	List<DeviceWrapper> sortedDimmers = dimmers.sort{it.displayName}
            sortedDimmers.each() { dimmer ->
                def safeName = dimmer.id
                def name = dimmer.displayName
				 
                 href(
                    name		: safeName + "_pg"
                    ,title		: name
                    ,required	: false
                    ,page		: "dimmerOptions"
                    ,params		: [id:safeName,displayName:name]
                    ,description: ""
                    ,state		: dimmerOptionsSelected(safeName)
                )
            }
		}
	}
}

Map dimmerOptions(params){
	Integer safeName
    String displayName
    if (params.id) {
    	safeName = 	params.id.toInteger()
        displayName = params.displayName
    } else if (params.params) {
    	safeName = 	params.params.id.toInteger()
        displayName = params.params.displayName
    } 
  	String pageTitle = "${displayName} Options, Current LUX Mode: ${state.luxMode}"
  
    setCurrentOverride(safeName,this."${safeName}_dark",this."${safeName}_dusk",this."${safeName}_day",this."${safeName}_bright") //132,30,null,null,null
  
  	String titleDark = "Dark level [default: ${dimDark}]"
  	String titleDusk = "Dusk/Dawn level [default: ${dimDusk}]"
  	String titleDay = "Overcast level [default: ${dimDay}]"
  	String titleBright = "Bright level [default: ${dimBright}]"
    return dynamicPage(name: "dimmerOptions") {
            section("${pageTitle}") {
				input(
                	name					: safeName + "_autoLux"
                    ,title					: "Auto adjust levels during LUX changes when dimmer is on"
                    ,required				: false
                    ,type					: "bool"
                )
				input(
                	name					: safeName + "_usePrestage"
                    ,title					: "Use level prestage when dimmer is off (must be enabled on device)"
                    ,required				: false
                    ,type					: "bool"
                )                
                input(
                	name					: safeName + "_ramp"
                    ,title					: "Percent rate of change for Auto adjust"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: [["1":"1%"],["2":"2%"],["5":"5%"]]
                    ,defaultValue			: "2"
                )
            }
            section("Set these to override the default settings."){
				input(
                    name					: safeName + "_dark"
                    ,title					: titleDark //"Dark level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: overrideDarkOpts.options
                  	,submitOnChange			: true
                )
                input(
                    name					: safeName + "_dusk" 
                    ,title					: titleDusk //"Dusk/Dawn level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: overrideDarkOpts.options
                  	,submitOnChange			: true
                )
                input(
                    name					: safeName + "_day" 
                    ,title					: titleDay //"Overcast level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: overrideDarkOpts.options
                  	,submitOnChange			: true
                )
                input(
                    name					: safeName + "_bright" 
                    ,title					: titleBright //"Bright level"
                    ,multiple				: false
                    ,required				: false
                    ,type					: "enum"
                    ,options				: overrideDarkOpts.options
                  	,submitOnChange			: true
                )
			}
    }
}

/* href methods	* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
String dimmersPageComplete(){
	if (dimmers && dimDark && dimDusk && dimDay && dimBright){
    	return "complete"
    } else {
    	return ""
    }
}

String dimmerOptionsSelected(safeName){
	def optionsList = ["${safeName}_autoLux","${safeName}_dark","${safeName}_dusk","${safeName}_day","${safeName}_bright"]
    if (optionsList.find{this."${it}"}){
		state.anyOptionsSet = true
		return "complete"
    } else {
    	return ""
    }
}

String anyOptionsSet(){
    if (state.anyOptionsSet) {
    	return "complete"
    } else {
    	return ""
    }
}

String luxPageComplete(){
	if (luxOmatic && luxDark && luxDusk && luxBright){
    	return "complete"
    } else {
    	return ""
    }
}
