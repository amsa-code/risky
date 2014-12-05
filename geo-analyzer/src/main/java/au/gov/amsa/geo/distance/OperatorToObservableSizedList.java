package au.gov.amsa.geo.distance;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.observers.Subscribers;

/**
 * Presizes the ArrayList returned by the given size parameter. This will save
 * some seconds in run time for large lists.
 */
public class OperatorToObservableSizedList<T> implements Operator<List<T>, T> {

	private static Logger log = Logger
			.getLogger(OperatorToObservableSizedList.class);

	private final int size;

	public OperatorToObservableSizedList(int size) {
		this.size = size;
	}

	@Override
	public Subscriber<? super T> call(final Subscriber<? super List<T>> child) {
		log.info("allocating list of size " + size);
		final List<T> list;
		if (size == 0)
			list = new LinkedList<T>();
		else
			list = new ArrayList<T>(size);
		log.info("allocated");
		Subscriber<T> parent = Subscribers.from(new Observer<T>() {

			@Override
			public void onCompleted() {
				child.onNext(list);
				child.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				child.onError(e);
			}

			@Override
			public void onNext(T t) {
				list.add(t);
			}
		});
		child.add(parent);
		return parent;
	}

}
