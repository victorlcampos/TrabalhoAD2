package models;

import java.util.ArrayList;
import java.util.List;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;
import Enum.RouterType;

public class Router implements Listener {
	private List<Event> eventBuffer;
	private Integer bufferSize;
	/** Taxa com que a fila é esvaziada em bytes */
	private Long broadcastRate;

	private RouterType type;
	private Boolean onService;
	
	private Simulator simulator;
	private Long lastTimeDelivered;

	public Router(Integer bufferSize, Long broadcastRate, RouterType type) {
		super();
		this.bufferSize = bufferSize;
		this.broadcastRate = broadcastRate;
		this.type = type;
		onService = false;

		eventBuffer = new ArrayList<Event>();

		simulator = Simulator.getInstance();

		simulator.registerListener(this, EventType.PACKAGE_SENT);
		simulator.registerListener(this, EventType.PACKAGE_DELIVERED);
		lastTimeDelivered = 0l;
	}

	@Override
	public void Listen(Event event) {
		switch (event.getType()) {
		case PACKAGE_SENT:
			if (type.equals(RouterType.FIFO)) {
				if(onService) {
					if (eventBuffer.size() < bufferSize) {
						eventBuffer.add(event);	//Caso o buffer esteja cheio, o pacote é descartado.	
					}
				} else {
					//Caso o buffer esteja vazio, inicia o atendimento imediatamente.
					deliverPackage(event);
				}
			} else {
				// TODO - Fazer o Red
			}
			break;
		case PACKAGE_DELIVERED:
			if (type.equals(RouterType.FIFO)) {
				if (eventBuffer.size() == 0) {
					onService = false;
				} else {
					deliverPackage(eventBuffer.remove(0));//Já que pacote acabou de ser servido, inicia serviço do outro.
				}
			} else {
				// TODO - Fazer o Red
			}
			break;
		default:
			break;
		}
	}

	private void deliverPackage(Event event) {
		Long initialTime = Math.max(lastTimeDelivered, event.getTime());
		
		PackageModel packageModel = event.getPackageModel();
		Long serviceTime = 1000l*1000000l*simulator.getMss()/broadcastRate;  
		
		onService = true;
		lastTimeDelivered = initialTime+serviceTime;
		simulator.shotEvent(event.getSender(), lastTimeDelivered, event.getGoOutServerTime(), EventType.PACKAGE_DELIVERED, packageModel);
	}
}
