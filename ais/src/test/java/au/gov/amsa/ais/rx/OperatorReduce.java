package au.gov.amsa.ais.rx;

import java.util.concurrent.atomic.AtomicReference;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.functions.Func2;

public class OperatorReduce<T, R> implements Operator<R, T> {

	private final Func2<R, ? super T, R> reduction;
	private R initialValue;

	public OperatorReduce(Func2<R, ? super T, R> reduction, R initialValue) {
		this.reduction = reduction;
		this.initialValue = initialValue;
	}

	@Override
	public Subscriber<? super T> call(Subscriber<? super R> child) {
		final ParentSubscriber<T, R> parent = new ParentSubscriber<T, R>(child,
				reduction, initialValue);
		child.setProducer(new Producer() {

			@Override
			public void request(long n) {
				parent.requestMore(n);
			}
		});
		child.add(parent);
		return parent;
	}

	private static class ParentSubscriber<T, R> extends Subscriber<T> {

		private static enum Status {
			NOT_REQUESTED_NOT_COMPLETED, NOT_REQUESTED_COMPLETED, REQUESTED_NOT_COMPLETED, REQUESTED_COMPLETED, EMITTED;
		}

		private final Subscriber<? super R> child;
		private R value;
		private AtomicReference<Status> status = new AtomicReference<Status>(
				Status.NOT_REQUESTED_NOT_COMPLETED);
		private final Func2<R, ? super T, R> reduction;

		ParentSubscriber(Subscriber<? super R> child,
				Func2<R, ? super T, R> reduction, R initialValue) {
			this.child = child;
			this.reduction = reduction;
			this.value = initialValue;
		}

		void requestMore(long n) {
			if (n > 0) {
				if (!status.compareAndSet(Status.NOT_REQUESTED_NOT_COMPLETED,
						Status.REQUESTED_NOT_COMPLETED)) {
					status.compareAndSet(Status.NOT_REQUESTED_COMPLETED,
							Status.REQUESTED_COMPLETED);
				}
			}
			// even if request = 0 might be ready to emit
			// so we will check again
			drain();
		}

		@Override
		public void onCompleted() {
			if (!status.compareAndSet(Status.REQUESTED_NOT_COMPLETED,
					Status.REQUESTED_COMPLETED)) {
				status.compareAndSet(Status.NOT_REQUESTED_NOT_COMPLETED,
						Status.NOT_REQUESTED_COMPLETED);
			}

			drain();
		}

		void drain() {
			if (status
					.compareAndSet(Status.REQUESTED_COMPLETED, Status.EMITTED)) {
				// synchronize to ensure that value is safely published
				synchronized (this) {
					if (isUnsubscribed())
						return;
					child.onNext(value);
					// release for gc
					value = null;
					if (!isUnsubscribed())
						child.onCompleted();
				}
			}
		}

		@Override
		public void onError(Throwable e) {
			child.onError(e);
		}

		@Override
		public void onNext(T t) {
			value = reduction.call(value, t);
		}

	}

}
