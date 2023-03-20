Key {
	var <degrees, <root, <mode, <name;
	classvar <all;

	*new { |root, mode|
		^super.new.init(root, mode);
	}

	init { |aRoot, aMode|
		var pitchClassDict = Dictionary.newFrom([C:0,Cs:1,Db:1,D:2,Ds:3,Eb:3,E:4,Es:5,Fb:4,F:5,Fs:6,Gb:6,G:7,Gs:8,Ab:8,A:9,As:10,Bb:10,B:11,Bs:0,Cb:11]);
		var pitchClass = pitchClassDict[aRoot];
		degrees = Scale.at(aMode).degrees.collect({ |degree| degree + pitchClass});
		root = aRoot;
		mode = aMode;
		name = [aRoot, aMode].join(" ");
	}

	degreeToMidi { |degree|
		var octave = floor(degree / 7) + 4;
		^degrees[degree % 7] + (12 * octave);
	}

	getChords {
		^7.collect({ |i|
			var chord, romanNumeral;
			chord = [degrees[i], degrees[i + 2 % 7], degrees[i + 4 % 7]];
			romanNumeral = i.toRomanNumeral.formatQuality(chord);
			[romanNumeral, chord];
		}).flatten.asDict;
	}
}

+ Key {

	*initClass {

		all = IdentityDictionary[

			// major keys
			\Ab -> Key.new(\Ab, \major),
			\A -> Key.new(\A, \major),
			\As -> Key.new(\As, \major),
			\Bb -> Key.new(\Bb, \major),
			\B -> Key.new(\B, \major),
			\Bs -> Key.new(\Bs, \major),
			\Cb -> Key.new(\Cb, \major),
			\C -> Key.new(\C, \major),
			\Cs -> Key.new(\Cs, \major),
			\Db -> Key.new(\Db, \major),
			\D -> Key.new(\D, \major),
			\Ds -> Key.new(\Ds, \major),
			\Eb -> Key.new(\Eb, \major),
			\E -> Key.new(\E, \major),
			\Es -> Key.new(\Es, \major),
			\Fb -> Key.new(\Fb, \major),
			\F -> Key.new(\F, \major),
			\Fs -> Key.new(\Fs, \major),
			\Gb -> Key.new(\Gb, \major),
			\G -> Key.new(\G, \major),
			\Gs -> Key.new(\Gs, \major),

			// minor keys
			\Abm -> Key.new(\Ab, \minor),
			\Am -> Key.new(\A, \minor),
			\Asm -> Key.new(\As, \minor),
			\Bbm -> Key.new(\Bb, \minor),
			\Bm -> Key.new(\B, \minor),
			\Bsm -> Key.new(\Bs, \minor),
			\Cbm -> Key.new(\Cb, \minor),
			\Cm -> Key.new(\C, \minor),
			\Csm -> Key.new(\Cs, \minor),
			\Dbm -> Key.new(\Db, \minor),
			\Dm -> Key.new(\D, \minor),
			\Dsm -> Key.new(\Ds, \minor),
			\Ebm -> Key.new(\Eb, \minor),
			\Em -> Key.new(\E, \minor),
			\Esm -> Key.new(\Es, \minor),
			\Fbm -> Key.new(\Fb, \minor),
			\Fm -> Key.new(\F, \minor),
			\Fsm -> Key.new(\Fs, \minor),
			\Gbm -> Key.new(\Gb, \minor),
			\Gm -> Key.new(\G, \minor),
			\Gsm -> Key.new(\Gs, \minor),

		];

		all = all.freezeAsParent;

	}

}