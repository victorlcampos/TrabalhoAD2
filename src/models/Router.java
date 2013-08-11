package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;
import Enum.RouterType;
import Utils.SimulatorProperties;

/**
 * 
 * Implementação da classe que irá simular o roteador.
 * <p>
 * Sua função será representar o buffer por onde passam nossas sessões TCP,
 * sendo ele o gargalo na rota IP destas sessões.
 *
 */
public class Router implements Listener {

	/**
	 * Lista que representa a fila de espera.
	 */
	private List<Event> eventBuffer;

	/** 
	 * Tamanho do buffer em pacotes 
	 */
	private Integer bufferSize;
	
	/** 
	 * Taxa com que a fila é esvaziada em bytes 
	 */
	private Long broadcastRate;

	/**
	 * Política de fila que está sendo usada.
	 */
	private RouterType type;

	/**
	 * Flag para indicar se o roteador está em serviço.
	 */
	private Boolean onService;
	
	/**
	 * Referencia para instancia única do simulator
	 */
	private Simulator simulator;
	
	/**
	 * Armazena o tempo da última vez que entregou um pacote ao receptor
	 */
	private Long lastTimeDelivered;
	
	/**
	 * Valor de wq usado pela política RED.
	 */
	private Float wq;
	
	/**
	 * Valor de minth usado pela política RED.
	 */
	private Integer minth;
	
	/**
	 * Valor de maxth usado pela política RED.
	 */
	private Integer maxth;
	
	/**
	 * Valor de maxp usado pela política RED.
	 */
	private Float maxp;
	
	/**
	 * Ocupação média da fila, usado somente pela política RED.
	 */
	private Float avg;
	
	/**
	 * Representa o número de pacotes não descartados desde o último descarte, utilizado pela política RED.
	 */
	private Integer count; 
	
	/**
	 * Gerador de número aleatórios.
	 */
	private Random rand;
	
	/**
	 * Armazena o tempo em que se iniciou o último período ocioso.
	 */
	private Long lastBusyPeriodTime;

	/**
	 * Constrói um roteador com a taxa fornecida.
	 * Ele irá escutar os eventos do tipo <code>EventType.PACKAGE_SENT</code> e <code>EventType.PACKAGE_DELIVERED</code>.
	 * 
	 * @param broadcastRate taxa de saída do roteador. 
	 * @param type tipo de politica de descarte
	 * @param bufferSize tamanho do buffer
	 */
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
		
		rand = new Random();
		wq = 0.002f;
		minth = 5;
		maxth = 15;
		maxp = 0.02f;
		avg = 0f;
		count = 0; 
		lastBusyPeriodTime =  0l;
	}

	/**
	 * Implementação do método responsável por escutar os eventos.
	 * <p>
	 * Ele irá identificar o tipo de evento em questão, 
	 * e então executar as tarefas correspondentes ao tipo de evento e também à política de descarte.
	 * 
	 *  @param evento que será escutado.
	 */
	@Override
	public void Listen(Event event) {
		switch (event.getType()) {
		case PACKAGE_SENT:
			if (type.equals(RouterType.FIFO)) {
				acceptPackage(event);
			} else if (type.equals(RouterType.RED)) {
				
				if (onService)
					avg = (1 - wq)*avg + wq*eventBuffer.size(); 
				else
					avg = (float) (Math.pow((1 - wq), event.getTime() - lastBusyPeriodTime) * avg);
				
				if (eventBuffer.size() >= bufferSize || avg > maxth) {
					// pacote é perdido
					count = 0;
				}else if (avg < minth) {
					acceptPackage(event);
				} else {
					Float pb = maxp*(avg - minth) / (maxth - minth);
					Float pa = pb / (1 - count*pb);
					
					
					if (rand.nextFloat() < pa) {
						acceptPackage(event);
						count++;
					} else {
						// pacote é perdido
						count = 0;
					}
				}
				
			}
			break;
		case PACKAGE_DELIVERED:
			if (eventBuffer.size() == 0) {
				onService = false;
				lastBusyPeriodTime = event.getTime();
			} else {
				deliverPackage(eventBuffer.remove(0));//Já que pacote acabou de ser servido, inicia serviço do outro.
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Recebe o pacote ao chegar na fila, caso não tenha que ser descartado devido à política de descarte.
	 * <p>
	 * Se houverem pacotes na fila, coloca o novo evento na fila, senão, inicia o atendimento imediatamente, chamando o método deliverPackage()
	 * 
	 * @param event evento com pacote que acabou de chegar na fila
	 */
	private void acceptPackage(Event event) {
		if(onService) {
			if (eventBuffer.size() < bufferSize) {
				eventBuffer.add(event);	//Caso o buffer esteja cheio, o pacote é descartado.	
			}
		} else {
			//Caso o buffer esteja vazio, inicia o atendimento imediatamente.
			deliverPackage(event);
		}
	}

	/**
	 * Dispara evento de entrega do pacote ao receptor para acontecer após o tempo de serviço
	 * @param event
	 */
	private void deliverPackage(Event event) {
		Long initialTime = Math.max(lastTimeDelivered, event.getTime());
		
		PackageModel packageModel = event.getPackageModel();
		Long serviceTime = 1000l*1000000l*SimulatorProperties.MSS/broadcastRate;  
		
		onService = true;
		lastTimeDelivered = initialTime+serviceTime;
		simulator.shotEvent(event.getSender(), lastTimeDelivered, event.leaveServerTime(), EventType.PACKAGE_DELIVERED, new PackageModel(packageModel.getValue()));
	}
}
