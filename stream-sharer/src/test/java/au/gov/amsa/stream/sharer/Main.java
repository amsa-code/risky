package au.gov.amsa.stream.sharer;

import rx.functions.Action1;

public class Main {

	public static void main(String[] args) {
		Lines.from("mariweb.amsa.gov.au", 9010).forEach(new Action1<String>() {
			@Override
			public void call(String line) {
				System.out.print(line);
			}
		});
	}

}
