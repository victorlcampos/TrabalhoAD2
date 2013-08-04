package models;

import java.util.Set;
import java.util.TreeSet;

import models.interfaces.Listerner;
import Controller.Simulator;
import Enum.EventType;

public class Receiver implements Listerner {
	private Server server;
	private PackageModel nextPackge;
	private Simulator simulator;
	private Set<PackageModel> receivedPackage;
	
	public Receiver(Server server) {
		super();
		this.server = server;
		nextPackge = new PackageModel(0);
		
		receivedPackage = new TreeSet<PackageModel>();
		
		simulator = Simulator.getInstance();
		simulator.registerListener(this, EventType.PACKAGE_DELIVERED);
	}

	@Override
	public void Listen(Event event) {
		if (event.getSender().equals(server)) {
			PackageModel eventPackage = event.getPackageModel();
			if (eventPackage.equals(nextPackge)) {
				nextPackge = new PackageModel(nextPackge.getValue() + simulator.getMss());
				Set<PackageModel> removePackage = new TreeSet<PackageModel>();
				
				while (receivedPackage.contains(nextPackge)) {
					removePackage.add(nextPackge);
					nextPackge = new PackageModel(nextPackge.getValue() + simulator.getMss());
				}
				
				receivedPackage.removeAll(removePackage);
			} else if (eventPackage.compareTo(nextPackge) == 1) {
				receivedPackage.add(eventPackage);
			}
			
			nextPackge.setSackOption(receivedPackage);
			
			long initialTime = event.getTime();
			simulator.shotEvent(this, initialTime + server.getGroup().getDelay(), event.getGoOutServerTime(), EventType.ACK, nextPackge);
		}				
	}	
}
