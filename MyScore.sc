MyScore {
	var data;

	*new {
		^super.new.init;
	}

	init {
		data = [[], [], [], [], [], [], [], []];
	}

	add { |voice, notes|
		if((voice < data.size),
			{
				data[voice] = (data[voice] ++ notes);
			},
			{
				("Warning: There's no voice at index" + voice).postln;
			}
		);
	}

	dursum { |voice, firstIdx, lastIdx|
		^data[voice].copyRange(firstIdx, lastIdx).collect({ |note| note[\dur] }).sum;
	}

	voiceLength { |voice|
		^this.dursum(voice, 0, data[voice].size - 1);
	}

	scoreLength {
		^data.size.collect({ |i| this.voiceLength(i) }).maxItem;
	}

	insert { |voice, beat, notes|
		if((voice < data.size), {
			inf.do({ |i|
				var currBeat;

				i = i.asInteger;

				currBeat = this.dursum(voice, 0, i);

				if((currBeat >= beat) || (i > data[voice].size), {
					var split1Dur, split2Dur, splitMidi;

					split1Dur = beat - this.dursum(voice, 0, i - 1);
					split2Dur = if(i < data[voice].size, { data[voice][i][\dur] - split1Dur }, 0);
					splitMidi = if(i < data[voice].size, { data[voice][i][\midinote] }, -1);

					if(i < data[voice].size, { data[voice].removeAt(i) });

					if(split1Dur > 0, {
						notes.addFirst((midinote: splitMidi, dur: split1Dur));
					});
					if(split2Dur > 0, {
						notes.add((midinote: splitMidi, dur: split2Dur));
					});

					data[voice] = data[voice].insert(i, notes).flat;

					^this;
				});
			});
		}, {
			("Warning: There's no voice at index" + voice).postln;
		});
	}

	getNotesStartingInRange { |voice, fromBeat, toBeat|
		var indexes = List.new;
		data[voice].size.do({ |index|
			var midinote = data[voice][index][\midinote];
			if((midinote > 0) && (midinote < 127), {
				var beat = if(index > 0, this.dursum(voice, 0, index - 1), 0);
				if((beat >= fromBeat) && (beat < toBeat), {
					indexes.add(index);
				});
			});
		});
		^indexes;
	}

	harmonize { |chords, harmonicRythm|

		inf.do({ |i|
			var fromBeat, toBeat, indexes, adjustments, ratings, chord;

			i = i.asInteger;

			fromBeat = i.collect({ |n| harmonicRythm[n % harmonicRythm.size] }).sum;

			if(fromBeat >= this.scoreLength, {^this});

			toBeat = (i + 1).collect({ |n| harmonicRythm[n % harmonicRythm.size] }).sum;

			// Get array of indexes of notes in range per voice
			indexes = data.size.collect({ |voice|
				this.getNotesStartingInRange(voice, fromBeat, toBeat);
			});

			adjustments = chords.keys.collect({ |key|
				[key, indexes.size.collect({ |voiceIdx|
					indexes[voiceIdx].collect({ |noteIdx|
						var note = data[voiceIdx][noteIdx];
						var distances = chords[key].collect({ |n| minCircDist(note[\midinote], n, 0, 12) });
						distances.abs.minItem;
					});
				})];
			}).asArray.flatten.asDict;

			ratings = adjustments.keys.collect({ |key|
				[key, adjustments[key].flat.abs.mean];
			}).asArray.flatten.asDict;

			chord = ratings.select { |item| item == ratings.values.minItem}.keys.choose;

			chord.postln;

			// Adjust notes
			indexes.size.do({ |voiceIdx|
				indexes[voiceIdx].size.do({ |i|
					var noteIdx = indexes[voiceIdx][i];
					var note = data[voiceIdx][noteIdx];
					var adj = adjustments[chord][voiceIdx][i];
					note[\midinote] = note[\midinote] + adj;
					data[voiceIdx][noteIdx] = note;
				});
			});
		});
	}

	combineRepeats {
		var newData = data.size.collect({ List.new });
		data.size.do({ |voiceIdx|
			var prevNote;
			data[voiceIdx].size.do({ |noteIdx|
				var note = data[voiceIdx][noteIdx];
				var isRepeat = false;

				if(prevNote != nil, {
					if(note[\midinote] == prevNote[\midinote], {
						isRepeat = true;
					});
				});

				if(isRepeat.not, {
					var bool = true;
					var iter = 0;

					while({bool}, {
						iter = iter + 1;

						if(noteIdx + iter <= data[voiceIdx].lastIndex, {
							var followNote = data[voiceIdx][noteIdx + iter];

							if(followNote[\midinote] == note[\midinote], {
								note[\dur] = note[\dur] + followNote[\dur];
							}, {
								bool = false;
							});
						}, {
							bool = false;
						});
					});
					newData[voiceIdx].add(note);
					prevNote = note;
				});
			});
		});
		data = newData;
	}

	makeRepeatsPauses { |maxInRow = inf|
		data.size.do({ |voiceIdx|
			var prevNote;
			var inRow = 0;
			data[voiceIdx].size.do({ |noteIdx|
				var note = data[voiceIdx][noteIdx];
				var isRepeat = false;

				if(prevNote != nil, {
					if(note[\midinote] == prevNote[\midinote], {
						isRepeat = true;
					});
				});

				if(isRepeat, {
					if(inRow < maxInRow, {
						data[voiceIdx][noteIdx][\midinote] = -1;
						inRow = inRow + 1;
					}, {
						inRow = 0;
					});
				}, {
					prevNote = note;
					inRow = 0;
				});

			});
		});
	}

	addPassingNotes { |voice, key, durs, maxInRow = 1|
		data[voice].size.do({ |i|
			var note = data[voice][i];
			var nextNote = data[voice][i + 1];
			var min = min(note[\midinote], nextNote[\midinote]);
			var max = max(note[\midinote], nextNote[\midinote]);
			var degreesInBetween = key.degrees.select({ |degree|
				circSub(min, degree, 0, 12).isPositive &&
				circSub(max, degree, 0, 12).isNegative
			});
		});
	}

	exportAsMidi { |filePath, seperateVoices = false|
		var mf;

		mf = SimpleMIDIFile(filePath);
		mf.init1(1, 120, "4/4");
		mf.timeMode_(\beats);

		data.do({ |voice|
			var currBeat = 0;
			voice.do({ |note|
				var voiceIndex = data.indexOf(voice);
				var dur = note[\dur] * 2;
				if(((0 <= note[\midinote]) && (note[\midinote] <= 127)), {
					var channel = if(seperateVoices, voiceIndex, 0);
					mf.addNote(note[\midinote], 64, currBeat, dur, 0, channel)}
				);
				currBeat = currBeat + dur;
			});
		});

		mf.write;
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