package au.gov.amsa.navigation;

class IdentifierPair {

	private final Identifier id1;
	private final Identifier id2;

	IdentifierPair(Identifier id1, Identifier id2) {
		this.id1 = id1;
		this.id2 = id2;
	}

	Identifier id1() {
		return id1;
	}

	Identifier id2() {
		return id2;
	}

}
