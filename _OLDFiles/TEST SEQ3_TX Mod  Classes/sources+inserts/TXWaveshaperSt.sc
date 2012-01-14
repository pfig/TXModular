// Copyright (C) 2009  Paul Miller. This file is part of TX Modular system distributed under the terms of the GNU General Public License (see file LICENSE).

TXWaveshaperSt : TXModuleBase {		// Distortion module 

	classvar <>arrInstances;	
	classvar <defaultName;  		// default module name
	classvar <moduleRate;			// "audio" or "control"
	classvar <moduleType;			// "source", "insert", "bus",or  "channel"
	classvar <noInChannels;			// no of input channels 
	classvar <arrAudSCInBusSpecs; 	// audio side-chain input bus specs 
	classvar <>arrCtlSCInBusSpecs; 	// control side-chain input bus specs
	classvar <noOutChannels;		// no of output channels 
	classvar <arrOutBusSpecs; 		// output bus specs
	classvar	<guiHeight=300;
	classvar	<guiWidth=450;
	classvar	<guiLeft=100;
	classvar	<guiTop=300;
	classvar	<arrBufferSpecs;
	
	var arrCurveValues;
	var arrSlotData;
	var displayOption;
	
*initClass{
	arrInstances = [];		
	//	set class specific variables
	defaultName = "Waveshaper St";
	moduleRate = "audio";
	moduleType = "insert";
	noInChannels = 2;			
	arrCtlSCInBusSpecs = [ 
		["Dry-Wet Mix", 1, "modWetDryMix", 0]
	];	
	noOutChannels = 2;
	arrOutBusSpecs = [ 
		["Out L + R", [0,1]], 
		["Out L only", [0]], 
		["Out R only", [1]] 
	];	
	arrBufferSpecs = [ ["bufnumShape", 512, 1] ];
} 

*new{ arg argInstName;
	 ^super.new.init(argInstName);
} 

init {arg argInstName;
	//	set  class specific instance variables
	extraLatency = 0.2;	// allow extra time when recreating
	displayOption = "showSettings";
	arrSynthArgSpecs = [
		["in", 0, 0],
		["out", 0, 0],
		["bufnumShape", 0, \ir],
		["inGain", 1, defLagTime],
		["outGain", 1, defLagTime],
		["wetDryMix", 1.0, defLagTime],
		["modWetDryMix", 0, defLagTime],
		["noiseMix", 10, \ir],
		["harmonicsMix", 100, \ir],
		["arrHarmonics", [1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0], \ir],
	]; 
	arrOptions = [0];
	arrOptionData = [
		[
			["Variable waveshaping - changes with amplitude", 
				{arg inSound, inBufnum; Shaper.ar(inBufnum, inSound); }
			],
			["Constant waveshaping - no changes with amplitude", 
				{arg inSound, inBufnum; 
					Balance.ar(Shaper.ar(inBufnum, Normalizer.ar(inSound)), inSound); 
				}
			],
		]
	];
	synthDefFunc = { arg in, out, bufnumShape, inGain, outGain, wetDryMix, modWetDryMix=0.0;
		var input, outFunction, outDistorted, outClean, mixCombined;
		input = InFeedback.ar(in,2);
		mixCombined = (wetDryMix + modWetDryMix).max(0).min(1);
		outFunction = arrOptionData.at(0).at(arrOptions.at(0)).at(1);
		outDistorted = outGain * outFunction.value(inGain * input, bufnumShape);
		Out.ar(out, (outDistorted * mixCombined) + (input * (1-mixCombined)) );
	};
	this.buildGuiSpecArray;
	arrActionSpecs = this.buildActionSpecs([
		["SynthOptionPopup", "Shaping type", arrOptionData, 0], 
		["EZslider", "In Gain", ControlSpec(0, 10), "inGain"],
		["TXCurveDraw", "Curve", {arrCurveValues}, 
			{arg view; arrCurveValues = view.value; arrSlotData = view.arrSlotData; 
				this.bufferStore(view.value);}, 
			{arrSlotData}, "Waveshaper"], 
		["EZslider", "Out Gain", ControlSpec(0, 1), "outGain"],
		["WetDryMixSlider"], 
	]);
	//	use base class initialise 
	this.baseInit(this, argInstName);
	//	make buffers, load the synthdef and create the synth
	this.makeBuffersAndSynth(arrBufferSpecs);
	//	initialise buffer to linear curve
	arrCurveValues = Array.newClear(257).seriesFill(0, 1/256);
	Routine.run {
		var holdModCondition;
		// add condition to load queue
		holdModCondition = system.holdLoadQueue.addCondition;
		// pause
		holdModCondition.wait;
		system.server.sync;
		// buffer store
		this.bufferStore(arrCurveValues);
		// remove condition from load queue
		system.holdLoadQueue.removeCondition(holdModCondition);
	};
	//	initialise slots to linear curves
	arrSlotData = arrCurveValues.dup(5);
}

buildGuiSpecArray {
	guiSpecArray = [
		["ActionButton", "Settings", {displayOption = "showSettings"; 
			this.buildGuiSpecArray; system.showView;}, 130, 
			TXColor.white, this.getButtonColour(displayOption == "showSettings")], 
		["Spacer", 3], 
		["ActionButton", "Harmonics", {displayOption = "showHarmonics"; 
			this.buildGuiSpecArray; system.showView;}, 130, 
			TXColor.white, this.getButtonColour(displayOption == "showHarmonics")], 
		["Spacer", 3], 
		["ActionButton", "Noise", {displayOption = "showNoise"; 
			this.buildGuiSpecArray; system.showView;}, 130, 
			TXColor.white, this.getButtonColour(displayOption == "showNoise")], 
		["Spacer", 3], 
		["SpacerLine", 1], 
	];
	if (displayOption == "showSettings", {
		guiSpecArray = guiSpecArray ++[
			["SynthOptionPopup", "Shaping type", arrOptionData, 0], 
			["TXCurveDraw", "Curve", {arrCurveValues}, 
				{arg view; arrCurveValues = view.value; arrSlotData = view.arrSlotData; 
					this.bufferStore(view.value);}, 
				{arrSlotData}, "Waveshaper"], 
			["ActionButton", "Rebuild curve by mirroring & inverting", {this.runMirror}, 400],
			["SpacerLine", 4],
			["EZslider", "In Gain", ControlSpec(0, 10), "inGain"],
			["SpacerLine", 4],
			["EZslider", "Out Gain", ControlSpec(0, 1), "outGain"],
			["SpacerLine", 4],
			["WetDryMixSlider"], 
		];
	});
	if (displayOption == "showHarmonics", {
		guiSpecArray = guiSpecArray ++[
			["SynthOptionPopup", "Shaping type", arrOptionData, 0], 
			["TXCurveDraw", "Curve", {arrCurveValues}, 
				{arg view; arrCurveValues = view.value; arrSlotData = view.arrSlotData; 
					this.bufferStore(view.value);}, 
				{arrSlotData}, "Waveshaper"], 
			["TXMultiSlider", "Harmonics", ControlSpec(0, 1), "arrHarmonics", 16, 0, 100],
			["EZslider", "Harm mix", ControlSpec(0, 100), "harmonicsMix"],
			["ActionButton", "Add harmonics to the curve", {this.addHarmonics}, 400],
		];
	});
	if (displayOption == "showNoise", {
		guiSpecArray = guiSpecArray ++[
			["SynthOptionPopup", "Shaping type", arrOptionData, 0], 
			["SpacerLine", 4],
			["TXCurveDraw", "Curve", {arrCurveValues}, 
				{arg view; arrCurveValues = view.value; arrSlotData = view.arrSlotData; 
					this.bufferStore(view.value);}, 
				{arrSlotData}, "Waveshaper"], 
			["SpacerLine", 4],
			["EZslider", "Noise mix", ControlSpec(0, 100), "noiseMix"],
			["SpacerLine", 4],
			["ActionButton", "Add tapered noise to the curve", {this.addNoiseVarying}, 400],
			["SpacerLine", 4],
			["ActionButton", "Add untapered noise to the curve", {this.addNoise}, 400],
		];
	});
}
	
getButtonColour { arg colour2Boolean;
	if (colour2Boolean == true, {
		^TXColor.sysGuiCol4;
	},{
		^TXColor.sysGuiCol1;
	});
}

bufferStore { arg argArray;
	var holdSignal, holdArray;
	holdArray = argArray.deepCopy;
	// make array bipolar
	holdArray = (holdArray * 2) - 1;
	// make sure centre of array is 0 to stop DC offset
	holdArray[128] = 0;
	holdSignal = Signal.newFrom(holdArray);
	buffers.at(0).sendCollection(holdSignal.asWavetableNoWrap); 
}

addHarmonics {
	var harmonicsMix, harmonicsSignal, arrHarmonics, holdSignal;
	harmonicsMix = this.getSynthArgSpec("harmonicsMix") / 100;
	arrHarmonics = this.getSynthArgSpec("arrHarmonics");
	harmonicsSignal = (Signal.chebyFill(257, arrHarmonics) + 1) / 2;
	holdSignal = Signal.newFrom(arrCurveValues).blend(harmonicsSignal, harmonicsMix);
	arrCurveValues = Array.newFrom(holdSignal);
	this.bufferStore(arrCurveValues);
	system.showView;
}

addNoise {
	var noiseMix, noiseSignal, holdSignal;
	noiseMix = this.getSynthArgSpec("noiseMix") / 100;
	noiseSignal = Signal.newFrom(Array.rand(257, 0.0, 1.0));
	holdSignal = Signal.newFrom(arrCurveValues).blend(noiseSignal, noiseMix);
	arrCurveValues = Array.newFrom(holdSignal);
	this.bufferStore(arrCurveValues);
	system.showView;
	}

addNoiseVarying {
	var noiseMix, noiseSignal, holdSignal, arrFullNoise, arrLinear, arrVaryNoise;
	noiseMix = this.getSynthArgSpec("noiseMix") / 100;

	arrFullNoise = Array.rand(257, 0.0, 1.0);
	arrLinear = Array.newClear(128).seriesFill(0, 1/127);
	arrVaryNoise = arrLinear.collect({arg item,i; var holdProportion;
		holdProportion = i/127;
		(item * (1 - holdProportion)) + (arrFullNoise[i] * holdProportion)
	});
	noiseSignal = Signal.newFrom((arrVaryNoise.copy.reverse.neg) ++ [0] ++ arrVaryNoise);
	noiseSignal = (noiseSignal + 1) / 2;	// adjust range
	holdSignal = Signal.newFrom(arrCurveValues).blend(noiseSignal, noiseMix);
	arrCurveValues = Array.newFrom(holdSignal);
	this.bufferStore(arrCurveValues);
	system.showView;
	}

runMirror {
	var holdArray, holdSignal;
	holdArray = arrCurveValues.deepCopy;
	holdArray.removeAt(0);
	holdArray = holdArray.clump(2).collect({arg item, i; item.sum/2;});
	holdArray = holdArray.copy.neg.reverse ++ [0] ++ holdArray;
	holdArray = (holdArray + 1) / 2;
	holdSignal = Signal.newFrom(holdArray);
	arrCurveValues = Array.newFrom(holdSignal);
	this.bufferStore(arrCurveValues);
	system.showView;
}

extraSaveData { // override default method
	^[arrCurveValues, arrSlotData];
}

loadExtraData {arg argData;  // override default method
	arrCurveValues = argData.at(0);
	Routine.run {
		var holdModCondition;
		// add condition to load queue
		holdModCondition = system.holdLoadQueue.addCondition;
		// pause
		holdModCondition.wait;
		system.server.sync;
		// buffer store
		this.bufferStore(arrCurveValues);
		// remove condition from load queue
		system.holdLoadQueue.removeCondition(holdModCondition);
	};
	arrSlotData = argData.at(1);
}

}

