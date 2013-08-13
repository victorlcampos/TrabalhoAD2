package Utils;

import java.util.List;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * 
 * Implementação dos cálculos necessários para geração dos intervalos de confiança.
 *
 */
public class ConfidenceInterval {
	/**
	 * 
	 */
	private static SummaryStatistics stats;
	/**
	 * 
	 */
	private static TDistribution tDistribution;
	
	/**
	 * 
	 * @param data
	 * @param t
	 * @return
	 */
	public static double getPrecision(List<Double> data) {
		if (data.size() > 1) {
			stats = new SummaryStatistics();
			for (Double d : data) {
				stats.addValue(d);
			}
			tDistribution = new TDistribution(stats.getN() - 1);
			double t = tDistribution.inverseCumulativeProbability(1.0 - 0.1/2);	
			
			return 100*t*stats.getStandardDeviation()/(stats.getMean()*Math.sqrt(stats.getN()));			
		}
		return 100;
	}
	
	/**
	 * 
	 * @param summaryStatistics
	 * @param t
	 * @return
	 */
	private static double getConfidenceIntervalWidth(SummaryStatistics summaryStatistics, double t) {
		  return t * summaryStatistics.getStandardDeviation() / Math.sqrt(summaryStatistics.getN());
		}

	/**
	 * 
	 * @param data
	 * @return
	 */
	public static String getConfidenceInterval(List<Double> data) {
		if (data != null && data.size() > 0) {
			stats = new SummaryStatistics();
			for (Double d : data) {
				stats.addValue(d);
			}
			tDistribution = new TDistribution(stats.getN() - 1);
			double t = tDistribution.inverseCumulativeProbability(1.0 - 0.1/2);			
						
			double avarege = stats.getMean();
			
			return "(" + avarege + " +- "+getConfidenceIntervalWidth(stats, t)+")";
		}

		return null;
	}
	
	public static Double getMean(List<Double> data) {
		if (data != null && data.size() > 0) {
			stats = new SummaryStatistics();
			for (Double d : data) {
				stats.addValue(d);
			}
			return stats.getMean();
		}
		return -1d;
	}

}
