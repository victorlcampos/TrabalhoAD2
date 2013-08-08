package Controller;

import java.net.StandardProtocolFamily;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Utils.ConfidenceInterval;
import Utils.PropertiesReader;
import Utils.SimulatorProperties;

import views.SimulatorView;

import models.BackgroundTraffic;
import models.Event;
import models.PackageModel;
import models.Receiver;
import models.Router;
import models.Server;
import models.ServerGroup;
import models.interfaces.Listener;
import Enum.EventType;
import Enum.RouterType;

public class Simulator {
	private Map<EventType, List<Listener>> listeners;
	private Map<Long, Integer> data;
	private List<Event> eventBuffer;
	private List<Server> servers;
	private static Simulator instance;
	private List<Double> means;
	private Integer routerRate;
	private Integer serverRate;

	public static Simulator getInstance() {
		if (instance == null) {
			instance = new Simulator();
		}
		return instance;
	}

	private Simulator() {
		listeners = new HashMap<EventType, List<Listener>>();
		data = new HashMap<Long, Integer>();
		servers = new ArrayList<Server>();
		eventBuffer = new ArrayList<Event>();
		means = new ArrayList<Double>();
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
		simulator.serverRate = 0;
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
					simulator.serverRate++;
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
						simulator.means.add(simulator.serverRate*1000*1000000d/(totalTime));
						if (ConfidenceInterval.getPrecision(simulator.means) <= 10) {
							lastTime = true;
						}
					}
					
					finalTime += totalTime;
					simulator.serverRate = 0;
					firstTime = false;
				}
			}
		}
		
		System.out.println(simulator.means);
		System.out.println(ConfidenceInterval.getConfidenceInterval(simulator.means));
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
			}
		}
	}

	private  void updatePlot(Long time, Server server) {
		data.put(time, (int) (Math.floor(server.getCwnd()/SimulatorProperties.MSS)));
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
