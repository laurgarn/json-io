package com.cedarsoftware.io;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.cedarsoftware.util.SealableList;

class RefPrep {
	private long id = 0;

	private int minDepth = Integer.MAX_VALUE;
	private int count    = 0;

	public void recordOneUse(int depth) {
		if (minDepth > depth) {
			minDepth = depth;
		}
		count += 1;
	}

	public long getId() {
		return id;
	}

	void setId(long id) {
		if (this.id < 1 && id > 0) {
			this.id = id;
		}
	}

	public int getMinDepth() {
		return minDepth;
	}

	public int getCount() {
		return count;
	}

	void updateCount(int c) {
		count = c;
	}

	@Override
	public String toString()
	{
		return "RefPrep{" +
				"id=" + id +
				", minDepth=" + minDepth +
				", count=" + count +
				'}';
	}
}

public class RefsAccounter {

	private final boolean              leastDeep;
	private final boolean              dropsALot;
	private final Map<Object, RefPrep> refPrepsByObj = new IdentityHashMap<>();
	private       long                 growing       = 1;

	public RefsAccounter(boolean leastDeep, boolean dropsALot) {
		this.leastDeep = leastDeep;
		this.dropsALot = dropsALot;
	}

	public boolean recordOneUse(Object obj, int depth) {
		if (dropsALot) {
			if (obj == null) return false;
			if (MetaUtils.isLogicalPrimitive(obj.getClass())) return false;
			if (obj instanceof Collection && ((Collection<?>)obj).isEmpty()) {
				if (obj.getClass().getSimpleName().startsWith("Sealable"))
					return false;
			}
		}


		RefPrep refPrep = refPrepsByObj.get(obj);
		boolean wasNotHere = refPrep == null;
		if (wasNotHere) {
			refPrep = new RefPrep();
			refPrepsByObj.put(obj, refPrep);
		}

		refPrep.recordOneUse(depth);
		return wasNotHere;
	}

	/**
	 * Return possible id for given object at given depth.
	 * null -> no need for id
	 * < 0 should output id with object
	 * > 0 should just output a reference
	 */
	public Long getCodedId(Object obj, int depth)
	{
		RefPrep prep = refPrepsByObj.get(obj);

		if (prep == null) return null;

		if (prep.getCount() == 1) {
			// at least one
			List<Object> toRemove = refPrepsByObj.entrySet().stream()
				.filter(p -> p.getValue().getCount() == 1).map(Map.Entry::getKey)
				.collect(Collectors.toList());
			toRemove.forEach(refPrepsByObj::remove);
			// no more, now

			return null;
		}

		if (prep.getId() == 0) {
			prep.setId(growing++);
			if (prep.getCount() > 0 && (!leastDeep || depth <= prep.getMinDepth())) {
				prep.updateCount(0);
				return -prep.getId(); // -> id
			}

			if (!leastDeep || prep.getCount() <= 0) {
				prep.updateCount(prep.getCount() - 1);
			}

			return prep.getId(); // -> ref
		}

		if (leastDeep && depth <= prep.getMinDepth() && prep.getCount() > 0) {
			prep.updateCount(0);
			return -prep.getId(); // -> id
		}

		return prep.getId(); // -> ref
	}

	public boolean isReferenced(Object obj) {
		// 0 can not be a depth, so should not return the id case, just ref
		Long mayId = getCodedId(obj, 0);
		return mayId != null;
	}

	Map<Object, RefPrep> getView() {
		return Collections.unmodifiableMap(refPrepsByObj);
	}

	public String debugString() {
		int[] stats = new int[10];
		Class<?>[] classes = new Class[1];
		refPrepsByObj.forEach((k, v) -> {
			int c = v.getCount();
			int md = v.getMinDepth();
			int idx = (c == 0 || c == 1) ? c : 2;
			stats[idx] += 1;
			if (c > stats[3]) {
				stats[3] = c;
				classes[0] = k == null ? Void.class : k.getClass();
				stats[8] = md;
			}
			if (md > stats[9]) stats[9] = md;
			if (k instanceof String) {
				stats[4] += 1;
				if (c > 1) stats[5] += 1;
			}
			if (k instanceof Number) {
				stats[6] += 1;
				if (c > 1) stats[7] += 1;
			}
		});

		return String.format("#:%d, 0s:%d, 1s;%d, +s:%d, maxC:%d (%s, %d), sk:%d(%d), nk:%d(%d), maxD:%d",
				refPrepsByObj.size(), stats[0], stats[1], stats[2],
				stats[3], classes[0], stats[8],
				stats[4], stats[5], stats[6], stats[7],
				stats[9]
				);
	}
}
