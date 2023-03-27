GenerativeExpression {
	var <func, <lastArgs, <lastBeat;

	*new { |expr|
		^super.new.init(expr);
	}

	init { |expr|
		func = { |x| interpret(expr.replace("x", x)) };
		lastArgs = Dictionary();
	}

	slopeDeg { |x, dx = 0.0001|
		var dy = func.(x + dx) - func.(x);
		var slopeRad = atan(dy / dx);
		var slopeDeg = slopeRad.raddeg;
		^slopeDeg;
	}

	generate { |x, startBeat, length, durs, mul, add|
		var calcMidi, calcDur, stream, notes, currBeat;

		x = if(x != nil, x, lastArgs[\x]);
		startBeat = if(startBeat != nil, startBeat, lastArgs[\startBeat]);
		length = if(length != nil, length, lastArgs[\length]);
		durs = if(durs != nil, durs, lastArgs[\durs]);
		mul = if(mul != nil, mul, lastArgs[\mul]);
		add = if(add != nil, add, lastArgs[\add]);

		calcMidi = { |x|
			var y, degree, midi;
			y = func.(x);
			degree = (y.round * mul + add).round;
		};

		calcDur = { |x|
			var slopeDeg, minDur, maxDur, dur;
			slopeDeg = this.slopeDeg(x).abs;
			minDur = durs[durs.minIndex];
			maxDur = durs[durs.maxIndex];
			dur = slopeDeg.linlin(0, 90, maxDur, minDur).closestInList(durs);
		};

		stream = Pbind(
			\x, x,
			\degree, Pfunc({ |event| calcMidi.(event[\x])}),
			\dur, Pfunc({ |event| calcDur.(event[\x])}),
		).asStream;

		notes = List.new;
		currBeat = 0;

		while({currBeat < (startBeat + length)}, {
				var note = stream.next(());

				if((currBeat >= startBeat), {
					notes.add(note);
				});

				currBeat = currBeat + note[\dur];
			}
		);

		lastArgs = Dictionary[
			\x -> x,
			\startBeat -> startBeat,
			\length -> length,
			\durs -> durs,
			\mul -> mul,
			\add -> add,
		];

		lastBeat = startBeat + length;

		^notes;
	}
}

+ SimpleNumber {
	closestInList { |list|
		^list[list.absdif(this).minIndex];
	}
}


