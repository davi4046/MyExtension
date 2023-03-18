+ SimpleNumber {
	circAdd { |num, rmin, rmax|
		var result = this + num;
		var range = rmax - rmin;
		while({ result >= rmax }, { result = result - range });
		while({ result < rmin }, { result = result + range });
		^result;
	}

	circSub { |num, rmin, rmax|
		var result = this - num;
		var range = rmax - rmin;
		while({ result >= rmax }, { result = result - range });
		while({ result < rmin }, { result = result + range });
		^result;
	}

	circMul { |num, rmin, rmax|
		var result = this * num;
		var range = rmax - rmin;
		while({ result >= rmax }, { result = result - range });
		while({ result < rmin }, { result = result + range });
		^result;
	}

	minCircDist { |to, rmin, rmax|
		var a = circSub(to, this, rmin, rmax);
		var b = circSub(this, to, rmin, rmax);
		if(b > a, {
			^a;
		}, {
			^b.neg;
		});
	}

	maxCircDist { |to, rmin, rmax|
		var a = circSub(to, this, rmin, rmax);
		var b = circSub(this, to, rmin, rmax);
		if(b < a, {
			^a;
		}, {
			^b.neg;
		});
	}
}