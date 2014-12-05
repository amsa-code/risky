package au.gov.amsa.geo;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;
import au.gov.amsa.geo.model.Bounds;
import au.gov.amsa.geo.model.CellValue;
import au.gov.amsa.geo.model.Options;

public class OperatorCellValuesToBytes implements Operator<byte[], CellValue> {

	private final Options options;

	public OperatorCellValuesToBytes(Options options) {
		this.options = options;
	}

	@Override
	public Subscriber<? super CellValue> call(
			final Subscriber<? super byte[]> child) {

		Subscriber<CellValue> parent = Subscribers
				.from(new Observer<CellValue>() {

					final AtomicBoolean first = new AtomicBoolean(true);

					@Override
					public void onCompleted() {
						child.onCompleted();
					}

					@Override
					public void onError(Throwable e) {
						child.onError(e);
					}

					@Override
					public void onNext(CellValue cv) {
						if (first.getAndSet(false))
							child.onNext(toBytes(options));
						child.onNext(toBytes(cv));
					}
				});
		child.add(parent);
		return parent;
	}

	private static byte[] toBytes(Options options) {
		ByteBuffer bb = ByteBuffer.allocate(40);
		Bounds b = options.getBounds();
		bb.putDouble(options.getCellSizeDegreesAsDouble());
		bb.putDouble(b.getTopLeftLat());
		bb.putDouble(b.getTopLeftLon());
		bb.putDouble(b.getBottomRightLat());
		bb.putDouble(b.getBottomRightLon());
		return bb.array();
	}

	private static byte[] toBytes(CellValue cv) {
		ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putFloat((float) cv.getCentreLat());
		bb.putFloat((float) cv.getCentreLon());
		bb.putDouble(cv.getValue());
		return bb.array();
	}
}
