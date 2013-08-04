package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import models.interfaces.Listerner;
import Controller.Simulator;
import Enum.EventType;
import Enum.ServerStatus;

public class Server implements Listerner {
	private static Integer id = 1;
	private Integer myId;
	
	private Long broadcastRate;
	private Long rtt;
	private Double threshold;
	private Double cwnd;
	
	private Receiver receiver;
	private ServerStatus status;
	private ServerGroup group;

	private Simulator simulator;
	
	private Integer numOfPackages;
	private Integer duplicatedAcks;
	
	private PackageModel lastAck;
	private PackageModel nextPackageToSend;
	
	private Set<PackageModel> sendedPackages;
	private Set<PackageModel> receivedAckPackages;
	
	private long expectedReturnTime;
	private long deviationReturnTime;
	
	public Server(Long cwnd, ServerGroup group,
			Long broadcastRate) {
		super();
		this.cwnd = (double) cwnd;
		this.group = group;
		this.broadcastRate = broadcastRate;
		
		threshold = 65535d;
		status = ServerStatus.SLOW_START;
		myId = id++;
		numOfPackages = 1;
		
		sendedPackages = new TreeSet<PackageModel>();
		receivedAckPackages = new TreeSet<PackageModel>();
		
		expectedReturnTime = 4*group.getDelay();
		rtt = expectedReturnTime;
		deviationReturnTime = 0;
		
		simulator = Simulator.getInstance();
		simulator.registerListener(this, EventType.TIME_OUT);
		simulator.registerListener(this, EventType.ACK);
	}
	
	public void startServer(Receiver receiver) {
		this.receiver = receiver;
		lastAck = new PackageModel(0);
		
		Random rand = new Random(System.nanoTime());
		
		sendPackage(rand.nextInt(100)*1000000l, lastAck);
	}

	private void sendPackage(Long initialTime, PackageModel packageModel) {
		cancelTimeout(packageModel);
		
		Long serviceTime = (long) (1000l*1000l*1000l*simulator.getMss()/broadcastRate);
		Long finishedServiceTime = initialTime+serviceTime;		
		simulator.shotEvent(this, finishedServiceTime+group.getDelay(), initialTime, EventType.PACKAGE_SENT, packageModel);
		
		Long timeoutTime = getTimoutTime(finishedServiceTime);
		simulator.shotEvent(this, timeoutTime, initialTime, EventType.TIME_OUT, packageModel);
		
		sendedPackages.add(packageModel);		
		numOfPackages--;
		
		if (numOfPackages > 0) {
			getNextPackage();
			sendPackage(finishedServiceTime, nextPackageToSend);
		}
	}

	private Long getTimoutTime(Long finishedServiceTime) {
		Long differenceBetweenRealAndExpectation = rtt - expectedReturnTime;
		deviationReturnTime += (long) (Math.abs(differenceBetweenRealAndExpectation) - deviationReturnTime)/4;
		expectedReturnTime += (long) differenceBetweenRealAndExpectation/8;
		Long timeOutTime = expectedReturnTime + 4l*deviationReturnTime;
		
		if (timeOutTime < 2*group.getDelay()) {
			throw new RuntimeException("Tempo do timeout calculado errado");
		}
		
		return finishedServiceTime + timeOutTime;
	}

	@Override
	public void Listen(Event event) {
		switch (event.getType()) {
		case ACK:
			listenAck(event);			
			break;
		case TIME_OUT:
			listenTimeOut(event);
			break;
		default:
			break;
		}

	}

	private void listenTimeOut(Event event) {
		if (event.getSender().equals(this)) {
			threshold = cwnd/2;
			cwnd = (double) simulator.getMss();
			status = ServerStatus.CONGESTION_AVOIDANCE;
			
			nextPackageToSend = event.getPackageModel();			
			resendPackages(event.getTime());
		}		
	}

	private void listenAck(Event event) {
		if (event.getSender().equals(receiver)) {

			PackageModel eventPackage = event.getPackageModel();
			receivedAckPackages = eventPackage.getSackOption();
			for (PackageModel packageModel : receivedAckPackages) {
				cancelTimeout(packageModel);
				if (eventPackage.compareTo(packageModel) == 1) {
					throw new RuntimeException("Pacotes recebidos não foram apagados direito");
				}
			}
			
			rtt = event.getTime() - event.getGoOutServerTime();					
			
			if (eventPackage.equals(lastAck)) {				
				if (this.status.equals(ServerStatus.FAST_RETRANSMIT)) {
					cwnd += simulator.getMss();
					numOfPackages = getNumOfPackages();
					getNextPackage();
					sendPackage(event.getTime(), nextPackageToSend);					
				}else {
					duplicatedAcks++;
					if (duplicatedAcks == 3) {
						duplicatedAcks = 0;
						threshold = cwnd/2;
						cwnd = threshold + 3*simulator.getMss();
						removeSendedPackage(lastAck);
						nextPackageToSend = lastAck;
						
						resendPackages(event.getTime());
						
						status = ServerStatus.FAST_RETRANSMIT;
					}
				}
			}else {
				cancelTimeout(eventPackage);
				duplicatedAcks = 0;
				setStatus();
				
				
				if (this.status.equals(ServerStatus.SLOW_START)) {
					cwnd += simulator.getMss();
				}else if(this.status.equals(ServerStatus.CONGESTION_AVOIDANCE)) {
					Double numOfAcks = cwnd/simulator.getMss();
					if (numOfAcks == 0) {
						System.out.println(numOfAcks);
					}
					cwnd += simulator.getMss()/numOfAcks;
				} else {
					status = ServerStatus.CONGESTION_AVOIDANCE;
					cwnd = threshold;
				}
				numOfPackages = getNumOfPackages();
				cancelTimeout(lastAck);

				lastAck = event.getPackageModel();
				
				Set<PackageModel> removeSendedPackages = new TreeSet<PackageModel>();
				for (PackageModel packageModel : sendedPackages) {
					if (packageModel.compareTo(lastAck) == -1) {
						removeSendedPackages.add(packageModel);
					}
				}
				sendedPackages.removeAll(removeSendedPackages);				
				nextPackageToSend = lastAck;
								
				getNextPackage();
				
				sendPackage(event.getTime(), nextPackageToSend);	
			}
		}
	}

	private void resendPackages(Long time) {
		cancelAllSentEventsEvent();
		List<PackageModel> removedEvents = new ArrayList<PackageModel>();

		for (PackageModel packageModel : sendedPackages) {
			if (!receivedAckPackages.contains(packageModel)) {
				removedEvents.add(packageModel);
			}
		}
		sendedPackages.removeAll(removedEvents);
		
		numOfPackages = getNumOfPackages();
		sendPackage(time, nextPackageToSend);		

	}

	private void cancelTimeout(PackageModel packageModel) {
		List<Event> removedEvents = new ArrayList<Event>();
		List<Event> eventBuffer = simulator.getEventBuffer();
		
		for (int i = 0; i < eventBuffer.size(); i++) {
			Event event = eventBuffer.get(i);
			if (event.getSender().equals(this) && event.getType().equals(EventType.TIME_OUT) && event.getPackageModel().equals(packageModel)) {
				removedEvents.add(event);
				break;
			}
		}	
		eventBuffer.removeAll(removedEvents);
	}
	
	private void cancelAllSentEventsEvent() {
		List<Event> removedEvents = new ArrayList<Event>();
		List<Event> eventBuffer = simulator.getEventBuffer();
		
		for (int i = 0; i < eventBuffer.size(); i++) {
			Event event = eventBuffer.get(i);
			if (event.getSender().equals(this) && event.getType().equals(EventType.PACKAGE_SENT)) {
				removedEvents.add(event);
				removeSendedPackage(event.getPackageModel());
			}
		}	
		eventBuffer.removeAll(removedEvents);
	}

	private void removeSendedPackage(PackageModel packageModel) {
		sendedPackages.remove(packageModel);
	}

	private void getNextPackage() {
		while(sendedThisPackage(nextPackageToSend)) {
			nextPackageToSend = new PackageModel(nextPackageToSend.getValue() + simulator.getMss());
		}
	}

	public Double getCwnd() {
		return cwnd;
	}

	public void setCwnd(Double cwnd) {
		this.cwnd = cwnd;
	}

	private boolean sendedThisPackage(PackageModel nextPackageToSend) {
		return sendedPackages.contains(nextPackageToSend);
	}

	private void setStatus() {
		if (status != ServerStatus.FAST_RETRANSMIT) {
			if (cwnd < threshold) {
				status = ServerStatus.SLOW_START;
			} else {
				status = ServerStatus.CONGESTION_AVOIDANCE;
			}
		}
	}
	
	private Integer getNumOfPackages() {
		Integer value = (int) Math.floor(cwnd/simulator.getMss()) - sendedPackages.size() + receivedAckPackages.size();
		return  Math.max(0, value);
	}

	public ServerGroup getGroup() {
		return group;
	}
	
	@Override
	public String toString() {
		return "Servidor "+myId;
	}
}
