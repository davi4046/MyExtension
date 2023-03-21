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
			var currBeat = 0;

			inf.do({ |i|
				i = i.asInteger;

				currBeat = if(i < data[voice].size, { currBeat + data[voice][i][\dur] }, currBeat);

				if((currBeat >= beat) || (i >= data[voice].size), {
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
		});
	}

	getIndexAtBeat { |voice, beat|
		var noteBeat = 0;
		data[voice].size.do({ |noteIdx|
			var note = data[voice][noteIdx];
			if((noteBeat <= beat) && (noteBeat + note[\dur] >= beat), {
				^noteIdx;
			});
			noteBeat = noteBeat + note[\dur];
		});
		^nil;
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
						var minIndex = distances.abs.minIndex;
						distances[minIndex];
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
			var pitchesBetween = key.getPitchesBetween(note[\midinote], nextNote[\midinote]);
			if((pitchesBetween.size > 0) && (pitchesBetween.size <= maxInRow), {
				var hasAdded = false;
				var iter = 0;
				durs = durs.sort.reverse;

				while({ hasAdded.not && (iter < durs.size) }, {
					var dur = durs[iter];
					var remainDur = note[\dur] - (pitchesBetween.size * dur);

					if(remainDur > 0, {

						var passingNotes = pitchesBetween.collect({ |pitch| (midinote: pitch, dur: dur) });
						data[voice][i][\dur] = remainDur;
						data[voice] = data[voice].insert(i + 1, passingNotes).flatten;

						hasAdded = true;
					});

					iter = iter + 1;
				});
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