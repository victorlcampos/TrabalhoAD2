package Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import models.BackgroundTraffic;
import models.Event;
import models.PackageModel;
import models.Receiver;
import models.Router;
import models.Server;
import models.ServerGroup;
import models.interfaces.Listener;
import views.SimulatorView;
import Enum.EventType;
import Utils.ConfidenceInterval;
import Utils.PropertiesReader;
import Utils.SimulatorProperties;

public class Simulator {
	private Map<EventType, List<Listener>> listeners;
	private Map<Server, Map<Long, Integer>> data;
	private List<Event> eventBuffer;
	private List<Server> servers;
	private static Simulator instance;
	
	private Map<Server, List<Double>> means;
	private Map<Server, Integer> serversRate;
	
	private Integer routerRate;

	public static Simulator getInstance() {
		if (instance == null) {
			instance = new Simulator();
		}
		return instance;
	}

	private Simulator() {
		listeners = new HashMap<EventType, List<Listener>>();
		data = new HashMap<Server, Map <Long, Integer>>();
		servers = new ArrayList<Server>();
		eventBuffer = new ArrayList<Event>();
		means = new HashMap<Server, List<Double>>();
		serversRate = new HashMap<Server, Integer>();
	}

	public static void main(String[] args) {
		Simulator simulator = Simulator.getInstance();
		
		PropertiesReader.readProperties();
		
		initSimulator();
		
		Event event = null;
		
		Long time  = 0l;
		Long totalTime = SimulatorProperties.totalSimulationTime;
		Boolean lastTime = false;
		Boolean firstTime = false;
		Long finalTime = totalTime;
		
		simulator.routerRate = 0;
		Collections.sort(simulator.eventBuffer);

		while (simulator.eventBuffer.size() > 0) {
			event = simulator.eventBuffer.remove(0);
			
			if (event.getTime() < time) {
				throw new RuntimeException("Evento no passado");
			}
			
			for (Listener listener : simulator.listeners
					.get(event.getType())) {
				listener.Listen(event);
			}
						
			time = event.getTime();
			switch (event.getType()) {
			case PACKAGE_DELIVERED:
				simulator.routerRate++;
				break;
			case PACKAGE_SENT:
				if (event.getSender().getClass().equals(Server.class)) {
					Server server = getEventServer(event);
					if (simulator.serversRate.get(server) == null) {
						simulator.serversRate.put(server, 0);
					}
					simulator.serversRate.put(server, simulator.serversRate.get(server) + 1);
				}
				break;
			case ACK:
				if (lastTime) {					
					simulator.updatePlot(time, getEventServer(event));
				}
				break;
			case TIME_OUT:
				if (lastTime) {
					simulator.updatePlot(time, getEventServer(event));					
				}
				break;
			default:
				break;
			}
			
			Collections.sort(simulator.eventBuffer);			
			if(finalTime < time) {
				if (lastTime) {				
					break;
				}else {
					if (!firstTime) {
						lastTime = true;
						for (Entry<Server, Integer> serverRate : simulator.serversRate.entrySet()) {
							Server server = serverRate.getKey();
							if (simulator.means.get(server) == null) {								
								simulator.means.put(server, new ArrayList<Double>());
							}
							simulator.means.get(server).add(serverRate.getValue()*1000*1000000d/totalTime);
							if (ConfidenceInterval.getPrecision(simulator.means.get(server)) > 10) {
								lastTime = false;
							}
						}																		
					}					
					finalTime += totalTime;
					for (Server server : simulator.serversRate.keySet()) {
						simulator.serversRate.put(server, 0);
					}
					firstTime = false;
				}
			}
		}
		
		System.out.println(simulator.means);
		for (Entry<Server, List<Double>> means : simulator.means.entrySet()) {
			System.out.println("Servidor "+means.getKey()+": "+ConfidenceInterval.getConfidenceInterval(means.getValue()));			
		}
		
		new SimulatorView(simulator.data);
		System.out.println(simulator.routerRate*1000*1000000l/time);
		System.out.println();
	}

	private static Server getEventServer(Event event) {
		if (event.getSender().getClass().equals(Server.class))
			return (Server) event.getSender();
		else if (event.getSender().getClass().equals(Receiver.class))
			return ((Receiver) event.getSender()).getServer();
		else 
			return null;
	}

	private static void initSimulator() {
		Router router = new Router(SimulatorProperties.bufferLength, SimulatorProperties.routerBroadcastRate, SimulatorProperties.routerPolicy);
		new BackgroundTraffic(SimulatorProperties.averageGustLength, SimulatorProperties.averageGustInterval);
		
		for (int i = 0; i < SimulatorProperties.serverGroupsNumber; i++) {
			ServerGroup serverGroup = new ServerGroup(SimulatorProperties.serverGroupDelay[i]);
			for (int j = 0; j < SimulatorProperties.serverGroupQuantity[i]; j++) {
				Server server = new Server(SimulatorProperties.MSS, serverGroup, SimulatorProperties.serverBroadcastRate);
				Receiver receiver = new Receiver(server);
				server.startServer(receiver);
				Simulator.getInstance().servers.add(server);
			}
		}
	}

	private  void updatePlot(Long time, Server server) {
		if (data.get(server) == null) {
			data.put(server, new HashMap<Long, Integer>());
		}
		data.get(server).put(time, (int) (Math.floor(server.getCwnd()/SimulatorProperties.MSS)));
	}

	public void registerListener(Listener listener, EventType eventType) {
		List<Listener> eventListeners = listeners.get(eventType);
		if (eventListeners == null) {
			eventListeners = new ArrayList<Listener>();
			listeners.put(eventType, eventListeners);
		}

		eventListeners.add(listener);
	}

	public List<Event> getEventBuffer() {
		return eventBuffer;
	}

	public void setEventBuffer(List<Event> eventBuffer) {
		this.eventBuffer = eventBuffer;
	}

	public void shotEvent(Object sender, long time, long leaveServerTime,
			EventType type, PackageModel packageModel) {
		Event event = new Event(packageModel, sender, time, leaveServerTime,
				type);
		if (event.getSender().getClass().equals(Server.class) && event.getType().equals(EventType.PACKAGE_SENT)) {			
//			System.out.println(event);
		}
		eventBuffer.add(event);
	}
}
