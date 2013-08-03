package Controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import views.SimulatorView;

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
	private Long mss;

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

		this.mss = 1500l;
	}

	public static void main(String[] args) {
		Simulator simulator = Simulator.getInstance();

		ServerGroup serverGroup = new ServerGroup(100 * 1000l*1000l);
		Server server = new Server(simulator.getMss(), serverGroup, 1000l*1000l*1000l/8);
		new Router(40, 10l*1000l*1000l/8, RouterType.FIFO);

		Receiver receiver = new Receiver(server);
		server.startServer(receiver);
		Event event = null;
		Long time  = 0l;
		Long finalTime = 10*1000*1000000l;
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
			case ACK:
				simulator.updatePlot(time, server);
				break;
			case TIME_OUT:
				simulator.updatePlot(time, server);
				break;
			default:
				break;
			}
			
			Collections.sort(simulator.eventBuffer);
			if(finalTime < time) {
				break;
			}
		}
		new SimulatorView(simulator.data);
	}

	private  void updatePlot(Long time, Server server) {
		data.put(time, (int) (Math.floor(server.getCwnd()/mss)));
	}

	public void registerListener(Listener listener, EventType eventType) {
		List<Listener> eventListeners = listeners.get(eventType);
		if (eventListeners == null) {
			eventListeners = new ArrayList<Listener>();
			listeners.put(eventType, eventListeners);
		}

		eventListeners.add(listener);
	}

	public Long getMss() {
		return mss;
	}

	public void setMss(Long mss) {
		this.mss = mss;
	}

	public List<Event> getEventBuffer() {
		return eventBuffer;
	}

	public void setEventBuffer(List<Event> eventBuffer) {
		this.eventBuffer = eventBuffer;
	}

	public void shotEvent(Object sender, long time, long goOutServerTime,
			EventType type, PackageModel packageModel) {
		Event event = new Event(packageModel, sender, time, goOutServerTime,
				type);
		eventBuffer.add(event);
	}
}
