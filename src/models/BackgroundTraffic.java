package models;

import java.util.Random;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;

import Utils.SimulatorProperties;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;

public class BackgroundTraffic implements Listener {
	private Integer avgGustLength;
	private Integer numPackagesToSend;
	private Integer numPackagesSended;
	private PackageModel nextPackageToSend;
	private ExponentialDistribution exponentialDistribution;
	private Random randomNumber;

	
	public BackgroundTraffic(Integer avgGustLength, Double avgGustInterval) {
		Simulator.getInstance().registerListener(this, EventType.PACKAGE_SENT);
		this.nextPackageToSend = new PackageModel(0);
		this.avgGustLength = avgGustLength;
						
		this.exponentialDistribution = new ExponentialDistribution(avgGustInterval);
		randomNumber = new Random(System.nanoTime());
		sendGust(0l);
	}	
	
	private void sendGust(Long time) {
		numPackagesToSend = (int) Math.round(gustLength());
		numPackagesSended = 0;
		
		Long gustTime = (long) exponentialDistribution.sample();
		if (numPackagesToSend == 0) {
			sendGust(time + gustTime);
		} else {
			for (int i = 0; i < numPackagesToSend; i++) {
				Simulator.getInstance().shotEvent(this, time+gustTime, time+gustTime, EventType.PACKAGE_SENT, nextPackageToSend);
				nextPackageToSend = new PackageModel(nextPackageToSend.getValue() + SimulatorProperties.MSS);
				numPackagesSended++;
			}
		}
	}

	@Override
	public void Listen(Event event) {
		if (event.getSender().equals(this)) {
			if (numPackagesSended == 1) {
				sendGust(event.getTime());
			}else {
				numPackagesSended--;
			}
		}
	}
	
	public ExponentialDistribution getExponentialDistribution() {
		return exponentialDistribution;
	}

	public void setExponentialDistribution(
			ExponentialDistribution exponentialDistribution) {
		this.exponentialDistribution = exponentialDistribution;
	}
	
	public double gustLength(){		
		return Math.log(randomNumber.nextDouble()) / Math.log(1 - 1d/avgGustLength);
	}
	
	public void reseed() {
		randomNumber = new Random(System.nanoTime());
	}
}
