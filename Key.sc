Key {
	var scale, rootNote;

	*new { |scale, rootNote|
		^super.new.init(scale, rootNote);
	}

	init { |aScale, aRootNote|
		scale = aScale;
		rootNote = aRootNote;
	}

	degreeToFreq { |degree|
		^scale.degreeToFreq(degree, rootNote.midicps, 0);
	}

	getChords {
		^scale.degrees.size.collect({ |i|
			var chord, romanNumeral;
			chord = [scale.degrees[i], scale.degrees[i + 2 % 7], scale.degrees[i + 4 % 7]];
			chord = chord.collect({ |midinote| midinote + rootNote % 12 });
			romanNumeral = i.toRomanNumeral.formatQuality(chord);
			[romanNumeral, chord];
		}).flatten.asDict;
	}

	test {
		rootNote.midicps.postln;
	}
}