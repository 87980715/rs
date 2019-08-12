package BinanceJavaAPIPackage;

import java.math.BigDecimal;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pack.binance.utils.ComputeValue;

public class TradeExchange {

	BigDecimal volume = new BigDecimal(0.0);

	public void addVolume(BigDecimal vol) {

		volume = volume.add(vol);
	}
	

	public boolean calculateBids(NavigableMap<BigDecimal, BigDecimal> bids) {

		volume = new BigDecimal(0.0);
		BigDecimal bestBid = bids.firstKey();
		BigDecimal cutOffBid = ComputeValue.percentage(bestBid, new BigDecimal(0.97));
		AtomicBoolean bidFound = new AtomicBoolean(false);

		AtomicBoolean loopContinue = new AtomicBoolean(true);

		for (Entry<BigDecimal, BigDecimal> entry : bids.entrySet()) {

			if (compareDecimals(entry.getKey(), cutOffBid) > 0) {

				System.out.println(entry.getValue());

				addVolume(entry.getKey());
				System.out.println("The current volume is " + volume);

				if (volumeCompare()) {
					bidFound.set(true);
					loopContinue.set(false);
					break;
				}
			}

			else {
				loopContinue.set(false);
			}

		}

		if (bidFound.get()) {
			return true;
		}
		else 
			return false;

	}

	public boolean calculatesells(NavigableMap<BigDecimal, BigDecimal> sells) {

		volume = new BigDecimal(0.0);
		BigDecimal bestsell = sells.lastKey();
		BigDecimal cutOffsell = ComputeValue.percentage(bestsell, new BigDecimal(1.03));
		AtomicBoolean sellFound = new AtomicBoolean(false);

		AtomicBoolean loopContinue = new AtomicBoolean(true);

		for (Entry<BigDecimal, BigDecimal> entry : sells.entrySet()) {

			if (compareDecimals(cutOffsell, entry.getKey()) > 0) {

				System.out.println(entry.getValue());

				addVolume(entry.getKey());
				System.out.println("The current volume is " + volume);

				if (volumeCompare()) {
					sellFound.set(true);
					loopContinue.set(false);
					break;

				}
			}

			else {

				loopContinue.set(false);
			}

		}

		if (sellFound.get())
			return true;
		return false;
	}

	private boolean volumeCompare() {
		if (this.volume.compareTo(new BigDecimal(3.0)) > 0)
			return true;
		return false;
	}

	private int compareDecimals(BigDecimal key, BigDecimal cutOffBid) {
		return key.compareTo(cutOffBid);
	}

}
