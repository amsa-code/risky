package au.gov.amsa.util.rx;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;

public class OperatorWriteBytes implements Operator<String, byte[]> {

	private static final int DEFAULT_BUFFER_SIZE = 8192;
	private final OutputStream out;
	private final boolean closeOnTerminate;
	private final File file;
	private final boolean append;
	private final int bufferSize;
	private final boolean createTempFile;

	private OperatorWriteBytes(File file, boolean createTempFile,
			boolean append, OutputStream out, boolean closeOnTerminate,
			int bufferSize) {
		this.file = file;
		this.createTempFile = createTempFile;
		this.append = append;
		this.out = out;
		this.closeOnTerminate = closeOnTerminate;
		this.bufferSize = bufferSize;
	}

	public OperatorWriteBytes(File file, boolean append) {
		this(file, false, append, null, true, DEFAULT_BUFFER_SIZE);
	}

	public OperatorWriteBytes(File file, boolean append, int bufferSize) {
		this(file, false, append, null, true, bufferSize);
	}

	public OperatorWriteBytes() {
		this(null, true, true, null, true, DEFAULT_BUFFER_SIZE);
	}

	public OperatorWriteBytes(int bufferSize) {
		this(null, true, true, null, true, bufferSize);
	}

	public OperatorWriteBytes(OutputStream out, boolean closeOnTerminate) {
		this(null, false, true, out, closeOnTerminate, DEFAULT_BUFFER_SIZE);
	}

	public OperatorWriteBytes(OutputStream out, boolean closeOnTerminate,
			int bufferSize) {
		this(null, false, true, out, closeOnTerminate, DEFAULT_BUFFER_SIZE);
	}

	@SuppressWarnings("resource")
	@Override
	public Subscriber<? super byte[]> call(
			final Subscriber<? super String> child) {

		// TODO prevent multiple active subscribers
		final OutputStream o;
		final File actualFile;
		if (file != null || createTempFile)
			try {
				if (createTempFile)
					actualFile = File.createTempFile(
							OperatorWriteBytes.class.getName(), ".bin");
				else
					actualFile = file;
				o = new FileOutputStream(actualFile, append);
			} catch (IOException e) {
				child.onError(e);
				return Subscribers.empty();
			}
		else {
			o = out;
			actualFile = null;
		}
		final OutputStream os = new BufferedOutputStream(o, bufferSize);
		final Subscriber<byte[]> parent = Subscribers
				.from(new Observer<byte[]>() {

					@Override
					public void onCompleted() {
						if (closeOnTerminate)
							try {
								os.close();
								if (actualFile != null)
									child.onNext(actualFile.getPath());
								child.onCompleted();
							} catch (IOException e) {
								child.onError(e);
							}
						else
							child.onCompleted();
					}

					@Override
					public void onError(Throwable e) {
						if (closeOnTerminate)
							try {
								os.close();
								child.onError(e);
							} catch (IOException e2) {
								child.onError(new CompositeException(e, e2));
							}
						else
							child.onError(e);
					}

					@Override
					public void onNext(byte[] bytes) {
						try {
							os.write(bytes);
						} catch (IOException e) {
							child.onError(e);
						}
					}
				});
		child.add(parent);
		return parent;
	}

}
