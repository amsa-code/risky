package au.gov.amsa.mariweb;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable.Operator;
import rx.Subscriber;

/**
 * Note that this Operator does not support backpressure.
 */
public class OperatorExtractValuesFromInsertStatement implements
		Operator<List<String>, String> {

	private static final char COMMA = ',';
	private static final char QUOTE = '\'';
	private static final char ESCAPE = '\\';
	private static final char SPACE = ' ';

	@Override
	public Subscriber<? super String> call(
			final Subscriber<? super List<String>> child) {
		return new Subscriber<String>(child) {

			@Override
			public void onCompleted() {
				if (!child.isUnsubscribed())
					child.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				if (!child.isUnsubscribed())
					child.onError(e);
			}

			@Override
			public void onNext(String line) {
				if (!child.isUnsubscribed()) {
					String clause = getClause(line);
					parseValuesFromClause(clause, child);
				}
			}

		};
	}

	private String getClause(String s) {
		int i = s.indexOf("VALUES");
		return s.substring(i + "VALUES".length()).trim();
	}

	private static <T> T unexpected() {
		throw new RuntimeException();
	}

	private static final boolean DEBUG = false;

	static void parseValuesFromClause(String s,
			Subscriber<? super List<String>> subscriber) {
		int charPosition = 0;
		boolean isOpen = false;
		boolean isEscaped = false;
		boolean isInQuotes = false;
		List<String> values = new ArrayList<>();
		StringBuilder token = new StringBuilder();

		char currentCh = '?';
		try {

			for (int i = 0; i < s.length(); i++) {
				if (subscriber.isUnsubscribed())
					return;
				char ch = s.charAt(i);
				currentCh = ch;
				if (DEBUG)
					System.out.println(charPosition + ":'" + currentCh
							+ "' open=" + isOpen + ", escaped=" + isEscaped
							+ ",inQuotes=" + isInQuotes + ",token=" + token
							+ "values=" + values);
				if (ch == '(' && !isInQuotes) {
					if (isOpen)
						unexpected();
					else
						isOpen = true;
				} else if (ch == ')' && !isInQuotes) {
					if (!isOpen)
						unexpected();
					else {
						isOpen = false;
						if (token.length() > 0) {
							values.add(token.toString());
							token = new StringBuilder();
						}
						subscriber.onNext(values);
						values = new ArrayList<String>();
					}
				} else if (ch == QUOTE && !isInQuotes) {
					isInQuotes = true;
				} else if (ch == QUOTE && !isEscaped) {
					isInQuotes = false;
				} else if (ch == QUOTE) {
					// must be escaped
					token.append(QUOTE);
					isEscaped = false;
				} else if (ch == ESCAPE && isInQuotes && isEscaped) {
					token.append(ESCAPE);
					isEscaped = false;
				} else if (ch == ESCAPE && isInQuotes) {
					isEscaped = true;
				} else if (ch == ESCAPE) {
					unexpected();
				} else if (ch == COMMA && !isInQuotes && isOpen) {
					values.add(token.toString());
					token = new StringBuilder();
				} else if (ch == COMMA && isInQuotes) {
					token.append(COMMA);
				} else if (ch == COMMA) {
					// ignore
				} else if (ch == SPACE && !isInQuotes) {
					// ignore
				} else {
					token.append(ch);
					isEscaped = false;
				}
				charPosition++;
				if (token.length() > 2000)
					throw new RuntimeException("token too long (>2000)");
				if (values.size() > 100)
					throw new RuntimeException(
							"too many columns found in a values clause (>100)");
			}
		} catch (RuntimeException e) {
			System.out.println("'" + currentCh + "' open=" + isOpen
					+ ", escaped=" + isEscaped + ",inQuotes=" + isInQuotes
					+ ",token=" + token + "values=" + values);
			writeLineToFile(s);
			throw new RuntimeException("error at position " + charPosition
					+ ". line with problem written to target/error-line.txt", e);
		}
	}

	private static void writeLineToFile(String s) {
		try {
			FileOutputStream fos = new FileOutputStream("target/error-line.txt");
			fos.write(s.getBytes("US-ASCII"));
			fos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
