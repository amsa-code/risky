package au.gov.amsa.risky.format;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import rx.functions.Action2;
import rx.functions.Func1;

public class ZipMain {

	public static void main(String[] args) {
		final File input = new File(System.getProperty("input"));
		final File output = new File(System.getProperty("output"));
		Pattern pattern = Pattern.compile(System.getProperty("pattern"));
		Action2<List<Fix>, File> fixesWriter = new Action2<List<Fix>, File>() {
			@Override
			public void call(List<Fix> fixes, File file) {
				BinaryFixesWriter.writeFixes(fixes, file, false, true);
			}
		};
		Func1<String, String> renamer = new Func1<String, String>() {
			@Override
			public String call(String name) {
				return name + ".zip";
			}
		};
		Formats.transform(input, output, pattern,
				Transformers.<Fix> identity(), fixesWriter, renamer).count()
				.toBlocking().single();
	}

}
