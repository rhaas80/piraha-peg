package edu.lsu.cct.piraha;

class Mapping {
	int from, delta;
	Mapping(int from,int delta) {
		this.from = from;
		this.delta = delta;
	}
	public String toString() {
		return "["+from+" += "+delta+"]";
	}
}