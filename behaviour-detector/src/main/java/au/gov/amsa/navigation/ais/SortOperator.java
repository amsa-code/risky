package au.gov.amsa.navigation.ais;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import rx.Observable.Operator;
import rx.Subscriber;

public class SortOperator<T> implements Operator<T, T> {

	private ArrayList<T> list;
	private Comparator<T> comparator;

	public SortOperator(Comparator<T> comparator, int size) {
		this.comparator = comparator;
		this.list = new ArrayList<T>(size);
	}

	@Override
	public Subscriber<? super T> call(final Subscriber<? super T> subscriber) {

		return new Subscriber<T>() {

			long count = 0;
			
			@Override
			public void onCompleted() {
				Collections.sort(list, comparator);
				for (T t : list)
					if (subscriber.isUnsubscribed())
						return;
					else
						subscriber.onNext(t);
				subscriber.onCompleted();
			}

			@Override
			public void onError(Throwable e) {
				subscriber.onError(e);
			}

			@Override
			public void onNext(T t) {
				if (++count % 100000==0) {
					System.out.println("count="+ count);
				}
				list.add(t);
			}
		};
	}

}
