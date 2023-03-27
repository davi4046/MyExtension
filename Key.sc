Key {
	var <degrees;
	classvar <all;

	*new { |root, mode|
		^super.new.init(root, mode);
	}

	init { |aRoot, aMode|
		degrees = Scale.at(aMode).degrees.collect({ |degree| degree + (aRoot % 12)});
	}

	degreeToMidi { |degree|
		var octave = floor(degree / 7) + 4;
		^(degrees[degree % 7] + (12 * octave)).asInteger;
	}

	midiToDegree { |midi|
		^degrees.mod(12).indexOf(midi % 12);
	}

	getChord { |chordSymbol|
		var nums, num, idx, ext, chord;

		nums = [\I, \II, \III, \IV, \V, \VI, \VII];
		num = chordSymbol.asString.findRegexpAt("[IV]+")[0];
		idx = nums.indexOf(num.asSymbol);

		ext = chordSymbol.asString.replace(num);

		if(ext == "",   { ext = [1, 3, 5] });
		if(ext == "7",  { ext = [1, 3, 5, 7] });
		if(ext == "9",  { ext = [1, 3, 5, 7, 9] });
		if(ext == "11", { ext = [1, 3, 5, 7, 9, 11] });
		if(ext == "13", { ext = [1, 3, 5, 7, 9, 11, 13] });

		^chord = ext.collect({ |n|
			degrees[idx + n - 1 % 7]
		});
	}

	getPitchesBetween { |a, b|
		var result = List.new;
		var min = min(a, b);
		var max = max(a, b);
		var closest = circClosest(min, degrees, 0, 12);
		var octave = floor(min - closest / 12);
		var pitch = closest + (12 * octave);
		var index = degrees.indexOf(closest);

		while({ pitch < max }, {
			pitch = pitch + circSub(degrees[index + 1 % 7], degrees[index % 7], 0, 12);
			result.add(pitch);
			index = index + 1 % 7;
		});

		result.remove(result.maxItem);

		if(a < b, {
			^result;
		}, {
			^result.reverse;
		});
	}
}

+ Key {

	*initClass {

		all = IdentityDictionary[

			// major keys
			\Ab -> Key.new(8, \major),
			\A -> Key.new(9, \major),
			\As -> Key.new(10, \major),
			\Bb -> Key.new(10, \major),
			\B -> Key.new(11, \major),
			\Bs -> Key.new(0, \major),
			\Cb -> Key.new(11, \major),
			\C -> Key.new(0, \major),
			\Cs -> Key.new(1, \major),
			\Db -> Key.new(1, \major),
			\D -> Key.new(2, \major),
			\Ds -> Key.new(3, \major),
			\Eb -> Key.new(3, \major),
			\E -> Key.new(4, \major),
			\Es -> Key.new(5, \major),
			\Fb -> Key.new(4, \major),
			\F -> Key.new(5, \major),
			\Fs -> Key.new(6, \major),
			\Gb -> Key.new(6, \major),
			\G -> Key.new(7, \major),
			\Gs -> Key.new(8, \major),

			// minor keys
			\Abm -> Key.new(8, \minor),
			\Am -> Key.new(9, \minor),
			\Asm -> Key.new(10, \minor),
			\Bbm -> Key.new(10, \minor),
			\Bm -> Key.new(11, \minor),
			\Bsm -> Key.new(0, \minor),
			\Cbm -> Key.new(11, \minor),
			\Cm -> Key.new(0, \minor),
			\Csm -> Key.new(1, \minor),
			\Dbm -> Key.new(1, \minor),
			\Dm -> Key.new(2, \minor),
			\Dsm -> Key.new(3, \minor),
			\Ebm -> Key.new(3, \minor),
			\Em -> Key.new(4, \minor),
			\Esm -> Key.new(5, \minor),
			\Fbm -> Key.new(4, \minor),
			\Fm -> Key.new(5, \minor),
			\Fsm -> Key.new(6, \minor),
			\Gbm -> Key.new(6, \minor),
			\Gm -> Key.new(7, \minor),
			\Gsm -> Key.new(8, \minor),

		];

		all = all.freezeAsParent;

	}

}

+ Integer {
	toRomanNumeral {
		var symbols = ["I", "II", "III", "IV", "V", "VI", "VII"];
		^symbols[this];
	}
}

+ String {
	formatQuality { |chord|
		var intervals = [chord[1] - chord[0] + 12 % 12, chord[2] - chord[1] + 12 % 12];
		var quality = switch(intervals,
			[4, 3], this.toUpper,
			[3, 4], this.toLower,
			[3, 3], this.toLower ++ "°",
			[4, 4], this.toUpper ++ "+",
			this ++ "?",
		);
		^quality;
	}
}