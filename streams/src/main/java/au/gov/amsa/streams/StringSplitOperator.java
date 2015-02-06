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
 * Splits and joins items in a sequence of strings based on a regex pattern.
 * Supports backpressure.
 */
public final class StringSplitOperator implements Operator<String, String> {

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

		// ensure unsubscription of child unsubscribes parent
		child.add(parent);
		return parent;
	}

	private static final class ParentSubscriber extends Subscriber<String> {

		// utility object for reactive events
		private final NotificationLite<String> on = NotificationLite.instance();

		// report events to this downstream subscriber
		private final Subscriber<? super String> child;

		// fast path or not
		private volatile boolean requestAll = false;

		// number of items being waited for (not used if fast path selected)
		private volatile long expected = 0;

		// updater object for the expected field above
		private final AtomicLongFieldUpdater<ParentSubscriber> EXPECTED = AtomicLongFieldUpdater
				.newUpdater(ParentSubscriber.class, "expected");

		// queue to hold messages till they are requested
		private final Deque<Object> queue = new LinkedList<Object>();

		// pattern to split upon
		private final Pattern pattern;

		// the bit left over from that last string that hasn't been terminated
		// yet
		private String leftOver = null;

		private ParentSubscriber(Subscriber<? super String> child,
				Pattern pattern) {
			this.child = child;
			this.pattern = pattern;
		}

		private void requestMore(long n) {
			// this method may be run concurrent with any of the event methods
			// below so watch out for thread safety
			if (requestAll || n <= 0)
				// ignore request if all items have already been requested or if
				// invalid request is received
				return;
			else if (n == Long.MAX_VALUE)
				requestAll = true;
			else
				EXPECTED.addAndGet(this, n);
			request(n);
		}

		@Override
		public void onCompleted() {
			if (requestAll) {
				// fast path
				if (leftOver != null)
					child.onNext(leftOver);
				if (!isUnsubscribed()) {
					child.onCompleted();
				}
			} else {
				// backpressure path
				if (leftOver != null)
					queue.add(leftOver);
				queue.add(on.completed());
				drainQueue();
			}
		}

		@Override
		public void onError(Throwable e) {
			if (requestAll) {
				// fast path
				child.onError(e);
			} else {
				// backpressure path
				queue.add(on.error(e));
				drainQueue();
			}
		}

		@Override
		public void onNext(String s) {
			String[] parts = pattern.split(s, -1);
			// prepend leftover to the first part
			if (leftOver != null)
				parts[0] = leftOver + parts[0];

			// can emit all parts except the last part because it hasn't been
			// terminated by the pattern/end-of-stream yet
			if (requestAll) {
				// fast path
				for (int i = 0; i < parts.length - 1; i++)
					child.onNext(parts[i]);
			} else {
				// backpressure path
				for (int i = 0; i < parts.length - 1; i++)
					queue.add(on.next(parts[i]));
				drainQueue();
			}
			// we have to assign the last part as leftOver because we don't know
			// if it has been terminated yet
			leftOver = parts[parts.length - 1];
		}

		private void drainQueue() {
			// only used by backpressure path
			while (true) {
				Object item = queue.peek();
				if (item == null || isUnsubscribed())
					break;
				else if (on.isCompleted(item) || on.isError(item)) {
					on.accept(child, queue.poll());
					break;
				} else if (expected == 0)
					break;
				else {
					// expected won't be Long.MAX_VALUE so can safely
					// decrement
					EXPECTED.decrementAndGet(this);
					on.accept(child, queue.poll());
				}
			}
		}
	}

}
