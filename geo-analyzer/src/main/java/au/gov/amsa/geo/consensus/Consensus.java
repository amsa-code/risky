package au.gov.amsa.geo.consensus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.gov.amsa.geo.Util;
import au.gov.amsa.geo.model.Fix;
import au.gov.amsa.util.navigation.Position;

public class Consensus {

	private static final Logger log = LoggerFactory.getLogger(Consensus.class);

	private static final double KM_PER_NM = 1.852;
	private static final double MAX_CONSENSUS = 1;
	private static final double DISTANCE_ACCURACY_NM = 0.1 / 1.852;

	/**
	 * Returns a value >=0 such that close to zero or zero indicates a large
	 * effective speed and any value of 1 or above indicates an effective speed
	 * under the maximum speed. For effective speeds slower than the max speed
	 * the returned value increases logarithmically with decreasing effective
	 * speed.
	 * 
	 * @param a
	 * @param b
	 * @param options
	 * @return
	 */
	public static double consensus(Fix a, Fix b, Options options) {
		Position p1 = Util.toPos(a);
		Position p2 = Util.toPos(b);
		double deltaT = Math.abs(a.getTime() - b.getTime())
				/ (double) TimeUnit.HOURS.toMillis(1);

		double deltaD = p1.getDistanceToKm(p2) / KM_PER_NM;
		double k = options.maxSpeedKnots()
				/ Math.max(deltaD, DISTANCE_ACCURACY_NM);
		final double scale = 10000;
		final double value;
		if (deltaT == 0) {
			if (deltaD <= DISTANCE_ACCURACY_NM)
				value = 1;
			else
				value = 0;
		} else {
			double x = deltaT * k;
			if (x <= 1)
				value = x * x * x;
			else
				value = 1 + Math.log(x) / scale;
		}
		return value;
	}

	public static List<ConsensusValue> consensus(List<Fix> fixes,
			Options options) {

		Map<Integer, Double> map = new HashMap<Integer, Double>();

		for (int i = 0; i < fixes.size(); i++) {
			double avg = consensusAt(fixes, options, i);
			map.put(i, avg);
		}
		List<ConsensusValue> result = new ArrayList<ConsensusValue>();
		for (int i = 0; i < fixes.size(); i++)
			result.add(new ConsensusValue(fixes.get(i), map.get(i)));
		return result;
	}

	private static double consensusAt(List<Fix> fixes, Options options, int i) {

		double total = 0;
		int count = 0;
		for (int j = Math.max(0, i - options.before()); j <= Math.min(
				fixes.size() - 1, i + options.after()); j++) {
			if (i != j) {
				double value = consensus(fixes.get(i), fixes.get(j), options);
				total += value;
				count++;
			}
		}
		double avg = total / count;
		return avg;
	}

	public static NavigableSet<Fix> improveConsensus(NavigableSet<Fix> fixes,
			Options options) {
		// copy fixes
		List<ConsensusValue> consensus = consensus(new ArrayList<>(fixes),
				options);
		// start with the fix of highest consensus value.
		// find the fix with the highest consensus score that is not in the
		// starting fixes
		// adjust that fix's time to maximize consensus and if >
		// minAcceptableConsensus then keep it
		// otherwise chuck and loop

		NavigableSet<ConsensusValue> orderedDescending = new TreeSet<>(
				consensus).descendingSet();
		TreeSet<Fix> fixesOrderedByTime = new TreeSet<>();
		Iterator<ConsensusValue> it = orderedDescending.iterator();
		boolean keepGoing = true;
		while (keepGoing) {
			ConsensusValue c = it.next();
			fixesOrderedByTime.add(c.getFix());
			keepGoing = it.hasNext() && c.getValue() >= 1;
		}
		for (ConsensusValue c : orderedDescending) {
			if (fixesOrderedByTime.contains(c.getFix()))
				// do nothing
				;
			else if (fixesOrderedByTime.size() == 0)
				fixesOrderedByTime.add(c.getFix());
			else {
				// calculate consensus of this fix with the set so far
				ConsensusValue adjusted = maximizeConsensus(fixesOrderedByTime,
						options, c.getFix());
				if (adjusted.getValue() > 1) {
					fixesOrderedByTime.add(adjusted.getFix());
					double diffMs = adjusted.getFix().getTime()
							- c.getFix().getTime();
					if (diffMs != 0)
						log.info("fix adjusted by " + diffMs
								/ TimeUnit.MINUTES.toMillis(1) + "mins "
								+ c.getFix());
				} else
					log.info("fix rejected " + c.getFix());
			}
		}
		return fixesOrderedByTime;
	}

	private static ConsensusValue maximizeConsensus(TreeSet<Fix> fixes,
			Options options, Fix fix) {
		NavigableSet<Fix> list = getBeforeAndAfter(fixes, options, fix);
		return maximizeConsensus(list, options, fix);
	}

	private static NavigableSet<Fix> getBeforeAndAfter(TreeSet<Fix> fixes,
			Options options, Fix fix) {
		CircularFifoQueue<Fix> before = new CircularFifoQueue<Fix>(
				options.before());
		CircularFifoQueue<Fix> after = new CircularFifoQueue<Fix>(
				options.after());

		TreeSet<Fix> timed = new TreeSet<>();

		for (Fix f : fixes) {
			if (f.getTime() >= fix.getTime()
					+ options.adjustmentLowerLimitMillis()
					&& f.getTime() < fix.getTime()
							+ options.adjustmentUpperLimitMillis())
				timed.add(f);
			if (f.getTime() < fix.getTime()) {
				before.add(f);
			} else
				after.add(f);
			if (after.size() == options.after())
				break;
		}
		// Now return the union of before,after,timed
		TreeSet<Fix> set = new TreeSet<>(before);
		set.addAll(after);
		set.addAll(timed);
		return set;
	}

	private static ConsensusValue maximizeConsensus(Collection<Fix> fixes,
			Options options, Fix fix) {
		Double maxConsensus = null;
		Long maxT = null;
		long timeStep = TimeUnit.MINUTES.toMillis(1);
		for (long i = 0; i <= Math.max(-options.adjustmentLowerLimitMillis(),
				options.adjustmentUpperLimitMillis()); i += timeStep) {
			if (i <= options.adjustmentUpperLimitMillis()) {
				long t = fix.getTime() + i;
				double cons = consensus(fixes, options, fix.time(t));
				if (maxConsensus == null || cons > maxConsensus) {
					maxConsensus = cons;
					maxT = t;
				}
				if (cons >= 1)
					break;
			}
			if (i <= -options.adjustmentLowerLimitMillis()) {
				long t = fix.getTime() - i;
				double cons = consensus(fixes, options, fix.time(t));
				if (maxConsensus == null || cons > maxConsensus) {
					maxConsensus = cons;
					maxT = t;
				}
				if (cons >= 1)
					break;
			}

		}
		return new ConsensusValue(fix.time(maxT), maxConsensus);
	}

	private static double consensus(Collection<Fix> fixes, Options options,
			Fix fix) {
		double total = 0;
		int count = 0;
		for (Fix f : fixes) {
			if (fix != f) {
				total += consensus(f, fix, options);
				count++;
			}
		}
		if (count == 0)
			return MAX_CONSENSUS;
		else
			return total / count;
	}

	private static double average(List<ConsensusValue> consensus) {
		double total = 0;
		int count = 0;
		for (ConsensusValue c : consensus) {
			total += c.getValue();
			count++;
		}
		return total / count;
	}

}
