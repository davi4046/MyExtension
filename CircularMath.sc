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

	circCeilToNearest { |coll, rmin, rmax|
		^coll[coll.collect({ |item| circSub(item, this, rmin, rmax) }).minIndex];
	}

	circFloorToNearest { |coll, rmin, rmax|
		^coll[coll.collect({ |item| circSub(this, item, rmin, rmax) }).minIndex];
	}

	circClosest { |coll, rmin, rmax|
		^coll[ circClosestIndex(this, coll, rmin, rmax) ];
	}

	circClosestIndex { |coll, rmin, rmax|
		^coll.collect({ |item| minCircDist(this, item, rmin, rmax) }).abs.minIndex;
	}
}