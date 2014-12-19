package au.gov.amsa.streams;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.regex.Pattern;

import rx.Observable.Operator;
import rx.Producer;
import rx.Subscriber;
import rx.internal.operators.NotificationLite;

/**
 * Splits strings by a regex pattern. Supports backpressure.
 */
public class StringSplitOperator implements Operator<String, String> {

	/**
	 * Pattern to split the strings by.
	 */
	private final Pattern pattern;

	/**
	 * Constructor.
	 * 
	 * @param pattern
	 */
	public StringSplitOperator(Pattern pattern) {
		this.pattern = pattern;
	}

	@Override
	public Subscriber<? super String> call(Subscriber<? super String> child) {
		final ParentSubscriber parent = new ParentSubscriber(child, pattern);

		child.setProducer(new Producer() {
			@Override
			public void request(long n) {
				parent.requestMore(n);
			}
		});

		return parent;
	}

	private static final class ParentSubscriber extends Subscriber<String> {

		// utility object for reactive events
		private final NotificationLite<String> on = NotificationLite.instance();

		// report events to this downstream subscriber
		private final Subscriber<? super String> child;

		// fast path or not
		private boolean requestAll = false;

		// number of items being waited for (not used if fast path selected)
		private volatile long expected = 0;

		// updater object for the expected field above
		private final AtomicLongFieldUpdater<ParentSubscriber> EXPECTED = AtomicLongFieldUpdater
				.newUpdater(ParentSubscriber.class, "expected");

		// queue to hold messages till they are requested
		private final Deque<Object> queue = new LinkedList<Object>();

		// pattern to split upon
		private final Pattern pattern;

		private String leftOver = "";

		private ParentSubscriber(Subscriber<? super String> child,
				Pattern pattern) {
			this.child = child;
			this.pattern = pattern;
		}

		private void requestMore(long n) {
			// this method may be run concurrent with any of the event methods
			// below so watch out for thread safety

			if (requestAll)
				// ignore request if all items have already been requested
				return;
			if (n == Long.MAX_VALUE)
				requestAll = true;
			else
				EXPECTED.addAndGet(this, n);
			request(n);
		}

		@Override
		public void onCompleted() {
			if (requestAll) {
				if (leftOver.length() > 0)
					child.onNext(leftOver);
				child.onCompleted();
			} else {
				if (leftOver.length() > 0)
					queue.add(leftOver);
				queue.add(on.completed());
				drainQueue();
			}
		}

		@Override
		public void onError(Throwable e) {
			if (requestAll)
				child.onError(e);
			else {
				queue.add(on.error(e));
				drainQueue();
			}
		}

		@Override
		public void onNext(String s) {
			String[] parts = pattern.split(s, -1);
			// can emit all parts except the last part because it hasn't been
			// terminated by the pattern yet
			parts[0] = leftOver + parts[0];
			if (requestAll) {
				for (int i = 0; i < parts.length - 1; i++)
					child.onNext(parts[i]);
			} else {
				for (int i = 1; i < parts.length - 1; i++)
					queue.add(on.next(parts[i]));
				drainQueue();
			}
			leftOver = parts[parts.length - 1];
		}

		private void drainQueue() {
			while (true) {
				Object item = queue.peek();
				if (item == null)
					break;
				else if (on.isCompleted(item) || on.isError(item)) {
					on.accept(child, queue.poll());
					break;
				} else {
					if (expected > 0) {
						// expected won't be Long.MAX_VALUE so can safely
						// decrement
						EXPECTED.decrementAndGet(this);
						on.accept(child, queue.poll());
					} else {
						break;
					}
				}
			}
		}
	}

}
