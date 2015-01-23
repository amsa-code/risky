package au.gov.amsa.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class Files {

	public static List<File> find(File file, final Pattern pattern) {
		if (!file.exists())
			return Collections.emptyList();
		else {
			if (!file.isDirectory()
					&& pattern.matcher(file.getName()).matches())
				return Collections.singletonList(file);
			else if (file.isDirectory()) {
				List<File> list = new ArrayList<File>();
				File[] files = file.listFiles();
				if (files != null)
					for (File f : file.listFiles()) {
						if (!f.getName().startsWith(".")) {
							if (f.isFile()
									&& pattern.matcher(f.getName()).matches())
								list.add(f);
							else
								list.addAll(find(f, pattern));
						}
					}
				return list;
			} else
				return Collections.emptyList();
		}
	}

}
