MyScore {
	var data, <key;

	*new { |key|
		^super.new.init(key);
	}

	init { |aKey|
		data = [[], [], [], [], [], [], [], []];
		key = aKey;
	}

	add { |voice, notes|

		notes = notes.collect({
			|note| (midinote: key.degreeToMidi(note[\degree]), dur: note[\dur])
		});

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

			notes = notes.collect({
				|note| (midinote: key.degreeToMidi(note[\degree]), dur: note[\dur])
			});

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

			if((noteBeat <= beat) && (noteBeat + note[\dur] > beat), {
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
		chords = chords.collect({ |numeral|
			var chord = key.getChord(numeral);
			numeral = numeral.asString.formatQuality(chord);
			[numeral, chord];
		}).flatten.asDict;

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

	/*combineRepeats { |acrossBars = true|
		var newData = data.size.collect({ List.new });

		data.size.do({ |voiceIdx|
			var prevNote;
			data[voiceIdx].size.do({ |noteIdx|
				var note = data[voiceIdx][noteIdx].copy;
				var noteBar = this.getBarAt(voiceIdx, noteIdx);

				var isRepeat = false;

				if(prevNote != nil, {
					if(note[\midinote] == prevNote[\midinote], {
						var prevNoteBar = this.getBarAt(voiceIdx, noteIdx - 1);

						if((noteBar == prevNoteBar) || (acrossBars == true), {
							isRepeat = true;
						});
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
								var followNoteBar = this.getBarAt(voiceIdx, noteIdx + iter);

								if((noteBar == followNoteBar) || (acrossBars == true), {
									note[\dur] = note[\dur] + followNote[\dur];
								}, {
									bool = false;
								});
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
	}*/

	combineRepeats { |voice, acrossBars = true|
		var newData = List.new;

		var prevNote;
		data[voice].size.do({ |noteIdx|
			var note = data[voice][noteIdx].copy;
			var noteBar = this.getBar(voice, noteIdx);

			var isRepeat = false;

			if(prevNote != nil, {
				if(note[\midinote] == prevNote[\midinote], {
					var prevNoteBar = this.getBar(voice, noteIdx - 1);

					if((noteBar == prevNoteBar) || (acrossBars == true), {
						isRepeat = true;
					});
				});
			});

			if(isRepeat.not, {
				var bool = true;
				var iter = 0;

				while({bool}, {
					iter = iter + 1;

					if(noteIdx + iter <= data[voice].lastIndex, {
						var followNote = data[voice][noteIdx + iter];

						if(followNote[\midinote] == note[\midinote], {
							var followNoteBar = this.getBar(voice, noteIdx + iter);

							if((noteBar == followNoteBar) || (acrossBars == true), {
								note[\dur] = note[\dur] + followNote[\dur];
							}, {
								bool = false;
							});
						}, {
							bool = false;
						});
					}, {
						bool = false;
					});
				});
				newData.add(note);
				prevNote = note;
			});
		});
		data[voice] = newData;
	}

	makeRepeatsPauses { |voice, maxInRow = inf, acrossBars = true|
		var prevNote;
		var inRow = 0;
		data[voice].size.do({ |noteIdx|
			var note = data[voice][noteIdx];
			var isRepeat = false;

			if(prevNote != nil, {
				if(note[\midinote] == prevNote[\midinote], {
					var noteBar = this.getBar(voice, noteIdx);
					var prevNoteBar = this.getBar(voice, noteIdx - 1);

					if((noteBar == prevNoteBar) || acrossBars, {
						isRepeat = true;
					});
				});
			});

			if(isRepeat, {
				if(inRow < maxInRow, {
					data[voice][noteIdx][\midinote] = -1;
					inRow = inRow + 1;
				}, {
					inRow = 0;
				});
			}, {
				prevNote = note;
				inRow = 0;
			});
		});
	}

	getBar { |voiceIdx, noteIdx|
		^floor(this.dursum(voiceIdx, 0, noteIdx - 1)).asInteger;
	}

	/*addPTs { |voice, durs, maxInRow = 1, offBeatOnly = true|
		(data[voice].size - 1).do({ |i|
			var note = data[voice][i];
			var nextNote = data[voice][i + 1];
			var pitchesBetween = key.getPitchesBetween(note[\midinote], nextNote[\midinote]);

			if((pitchesBetween.size > 0) && (pitchesBetween.size <= maxInRow), {
				var noteBeat = if(offBeatOnly, { this.dursum(voice, 0, i - 1) }, 0 /*placeholder value*/ );
				var hasAdded = false;
				var iter = 0;

				durs = durs.sort.reverse;

				while({ hasAdded.not && (iter < durs.size) }, {
					var dur = durs[iter];
					var remainDur = note[\dur] - (pitchesBetween.size * dur);

					if(remainDur > 0, {
						var beats = pitchesBetween.size.collect({ |n| noteBeat + remainDur + (n * dur) });
						var isOffBeatOnly = beats.mod(0.25).includes(0.0).not;

						if(isOffBeatOnly || offBeatOnly.not, {
							var passingNotes = pitchesBetween.collect({ |pitch| (midinote: pitch, dur: dur) });

							data[voice][i][\dur] = remainDur;
							data[voice] = data[voice].insert(i + 1, passingNotes).flatten;

							hasAdded = true;
						});
					});
					iter = iter + 1;
				});
			});
		});
	}*/

	addPTs { |voice, durs, maxInRow = 1, offBeatOnly = true|
		var newData = List.new;

		(data[voice].size - 1).do({ |i|
			var note = data[voice][i].copy;
			var nextNote = data[voice][i + 1].copy;
			var pitchesBetween = key.getPitchesBetween(note[\midinote], nextNote[\midinote]);

			newData.add(note);

			if((pitchesBetween.size > 0) && (pitchesBetween.size <= maxInRow), {
				var noteBeat = if(offBeatOnly, { this.dursum(voice, 0, i - 1) }, 0 /*placeholder value*/ );
				var hasAdded = false;
				var iter = 0;

				durs = durs.sort.reverse;

				while({ hasAdded.not && (iter < durs.size) }, {
					var dur = durs[iter];
					var remainDur = note[\dur] - (pitchesBetween.size * dur);

					if(remainDur > 0, {
						var beats = pitchesBetween.size.collect({ |n| noteBeat + remainDur + (n * dur) });
						var isOffBeatOnly = beats.mod(0.25).includes(0.0).not;

						if(isOffBeatOnly || offBeatOnly.not, {
							var passingNotes = pitchesBetween.collect({ |pitch| (midinote: pitch, dur: dur) });

							note[\dur] = remainDur;
							newData.add(passingNotes);

							hasAdded = true;
						});
					});
					iter = iter + 1;
				});
			});
		});
		data[voice] = newData.flatten;
	}

	addCPTs { |voice, durs, maxInRow = 1, offBeatOnly = true|
		var newData = List.new;

		(data[voice].size - 1).do({ |i|
			var note = data[voice][i].copy;
			var nextNote = data[voice][i + 1].copy;

			var min = min(note[\midinote], nextNote[\midinote]);
			var max = max(note[\midinote], nextNote[\midinote]);

			var steps = (max - min - 1).asInteger;

			newData.add(note);

			if((steps > 0) && (steps <= maxInRow), {
				var noteBeat = if(offBeatOnly, { this.dursum(voice, 0, i - 1) }, 0 /*placeholder value*/ );
				var hasAdded = false;
				var iter = 0;

				durs = durs.sort.reverse;

				while({ hasAdded.not && (iter < durs.size) }, {
					var dur = durs[iter];
					var remainDur = note[\dur] - (steps * dur);

					if(remainDur > 0, {
						var beats = steps.collect({ |n| noteBeat + remainDur + (n * dur) });
						var isOffBeatOnly = beats.mod(0.25).includes(0.0).not;

						if( isOffBeatOnly || offBeatOnly.not, {
							var passingNotes = steps.collect({ |n| (midinote: min + n + 1, dur: dur) });
							if(note[\midinote] > nextNote[\midinote], { passingNotes = passingNotes.reverse });

							note[\dur] = remainDur;
							newData.add(passingNotes);

							hasAdded = true;
						});
					});
					iter = iter + 1;
				});
			});
		});
		data[voice] = newData.flatten;
	}

	makeHarmonicMinor {
		data.size.do({ |voiceIdx|
			(data[voiceIdx].size - 1).do({ |noteIdx|
				var note = data[voiceIdx][noteIdx];
				var noteDegree = key.midiToDegree(note[\midinote]);

				if(noteDegree == 6, {
					note[\midinote] = note[\midinote] + 1;
				});
			});
		});
	}

	makeMelodicMinor {
		data.size.do({ |voiceIdx|
			(data[voiceIdx].size - 1).do({ |noteIdx|
				var note = data[voiceIdx][noteIdx];
				var noteDegree = key.midiToDegree(note[\midinote]);

				if(noteDegree == 6, {
					var nextNote = data[voiceIdx][noteIdx + 1];
					var nextNoteDegree = key.midiToDegree(nextNote[\midinote]);

					if(nextNoteDegree == 0, {
						var beat = this.dursum(voiceIdx, 0, noteIdx - 1);

						// Raise all leading-tones on this beat
						data.size.do({ |voiceIdx|
							var noteIdx = this.getIndexAtBeat(voiceIdx, beat);

							if(noteIdx != nil, {
								var note = data[voiceIdx][noteIdx];
								var noteDegree = key.midiToDegree(note[\midinote]);

								if(noteDegree == 6, {
									note[\midinote] = note[\midinote] + 1;
								});
							});
						});
					});
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