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

/**
 * 
 * Classe responsável por controlar e inicializar toda a simulação das sessões TCP.
 *<p>
 * Realiza a leitura do arquivo de entrada, inicia os 4 modelos que interagem entre si 
 * e gerencia a comunicação entre eles.
 * <p> 
 * Os 4 modelos que iremos simular são: A estação transmissora. representada pela classe <code>Server</code>, 
 * o roteador, representado pela classe <code>Router</code>, receptor, <code>Receptor</code>, e o tráfego de fundo, <code>BackgroundTraffic</code>.
 * 
 * @see Server, Receptor, Router, BackgroundTraffic
 */
public class Simulator {
	/**
	 * Mapa para registrar os objetos que escutarão um determinado evento.
	 * <p>
	 * Uma chave no map é um enumerável do tipo <code>EventType</code>,
	 * cujo valor armazena um conjunto com referências para todos os objetos 
	 * que se registraram para escutar este tipo de evento.
	 */
	private Map<EventType, List<Listener>> listeners;
	
	/**
	 * Pontos de cada servidor para plotar no gráfico
	 */
	private Map<Server, Map<Long, Integer>> data;
	
	/**
	 * Canal de eventos para comunicação interna entre as classes.
	 */
	private List<Event> eventBuffer;
	
	/**
	 * Lista de servidores existentes na simulação
	 */
	private List<Server> servers;
	
	/**
	 * Tráfego de fundo que será usado para congestionar o tráfego das sessões TCP.
	 */
	private BackgroundTraffic backgroundTraffic;
	
	private static Simulator instance;
	
	/** Map com as médias das taxas de transmissão das rodadas de simulação para cada servidor 
	 * 
	 * */
	private Map<Server, List<Double>> means;
	
	/** Map com as taxas de transmissão de cada servidor 
	 * 
	 * */
	private Map<Server, Integer> serversRate;
	
	/** Taxa de atendimento do roteador. Guarda a soma dos atendimentos até o momento
	 * 
	 * */
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
		
		//Leitura das variáveis de entrada
		PropertiesReader.readProperties();
		
		initSimulator();
		
		//Inicialização de variáveis===========
		//=====================================
		Event event = null;
		//Tempo de
		Long time  = 0l;
		//Tempo total da simulação, que é igual ao tempo de uma rodada
		Long roundDuration = SimulatorProperties.totalSimulationTime;
		//Indica se esta será a última rodada da simulação
		Boolean lastRound = false;
		//Indica que é a primeira rodada, ou seja, a fase transiente 
		Boolean firstRound = true;
		//Guarda o tempo de término da rodada atual
		Long currentRoundEndTime = SimulatorProperties.transientTime;

		simulator.routerRate = 0;
		Collections.sort(simulator.eventBuffer);
		//=====================================
		//=====================================

		//Execução do simulador
		while (simulator.eventBuffer.size() > 0) {
			
			//Pega primeiro evento que deve acontecer da lista de eventos, já removendo-o da lista.
			event = simulator.eventBuffer.remove(0);
			
			if (event.getTime() < time) {
				throw new RuntimeException("Evento no passado");
			}
			
			//Notifica todos os objetos que escutam eventos desse tipo
			for (Listener listener : simulator.listeners.get(event.getType())) {
				listener.Listen(event);
			}
				
			//Tempo atual passa a ser o tempo do evento atual, ou seja, simulador pula no tempo.
			time = event.getTime();
			
			//Atualização dos dados estatísticos
			switch (event.getType()) {
			case PACKAGE_DELIVERED:
				//Atualiza a taxa do roteador nessa rodada - Incrementa a quantidade de pacotes servidos neste milissegundo
				simulator.routerRate++;
				break;
				
			case PACKAGE_SENT:
				if (event.getSender().getClass().equals(Server.class)) {
					Server server = getEventServer(event);
					if (simulator.serversRate.get(server) == null) {
						simulator.serversRate.put(server, 0);
					}
					//Atualiza a taxa do servidor atual nessa rodada - Incrementa a quantidade de pacotes enviados do servidor que enviou o pacote atual
					simulator.serversRate.put(server, simulator.serversRate.get(server) + 1);
				}
				break;
				
			case ACK:
				if (lastRound) {		
					//Quando um ack é recebido, é adicionado um ponto no gráfico
					simulator.updatePlot(time, getEventServer(event));
				}
				break;
				
			case TIME_OUT:
				if (lastRound) {
					//Quando um time_out acontece, é adicionado um ponto no gráfico
					simulator.updatePlot(time, getEventServer(event));					
				}
				break;
			default:
				break;
			}
			
			//Reordenação da lista de eventos conforme sua ordem de acontecimento.
			Collections.sort(simulator.eventBuffer);	
			
			
			if(currentRoundEndTime < time) { //Se terminou a rodada
				if (lastRound) {				
					break;
				}else {
					simulator.backgroundTraffic.reseed();
					//Atualiza as taxas se não for fase transiente
					if (!firstRound) {
						lastRound = true;
						for (Entry<Server, Integer> serverRate : simulator.serversRate.entrySet()) {
							Server server = serverRate.getKey();
							if (simulator.means.get(server) == null) {								
								simulator.means.put(server, new ArrayList<Double>());
							}
							//Adiciona a média desta rodada para este servidor no map de médias
							simulator.means.get(server).add(serverRate.getValue()*1000*1000000d/roundDuration);
							//Não deixa simulação terminar até que precisão seja pelo menos 10%
							if (ConfidenceInterval.getPrecision(simulator.means.get(server)) > 10) {
								lastRound = false;
							}
						}																		
					}
					//Acrescenta uma rodada na execução do simulador, aumentando o tempo do fim da simulação
					currentRoundEndTime += roundDuration;
					
					//Reinicia as taxas dos servidores
					for (Server server : simulator.serversRate.keySet()) {
						simulator.serversRate.put(server, 0);
					}
					firstRound = false;
				}
			}
		}
		//======================================
		//FIM DA SIMULAÇÃO
		//======================================
		
		System.out.println(simulator.means);
		//Map com medias das taxas por grupo e servidor
		Map<ServerGroup, List<Double>> groupMeans = new HashMap<ServerGroup, List<Double>>();
		
		//Imprime intervalo de confiança de cada servidor, e preenche medias dos grupos
		for (Entry<Server, List<Double>> means : simulator.means.entrySet()) {
			ServerGroup group = means.getKey().getGroup();
			if (groupMeans.get(group) == null) {
				groupMeans.put(group, new ArrayList<Double>());
			}
			groupMeans.get(group).addAll(means.getValue());
			
			System.out.println("Servidor "+means.getKey()+": "+ConfidenceInterval.getConfidenceInterval(means.getValue()));			
		}
		
		//Imprime intervalos de confiança por grupo
		for (Entry<ServerGroup, List<Double>> groupMean : groupMeans.entrySet()) {
			System.out.println(groupMean.getKey() +": "+ConfidenceInterval.getConfidenceInterval(groupMean.getValue()));
		}
		
		//Plota gráfico
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

	/** Inicializa o roteador, o tráfego de fundo, e os servidores em tempo aleatório */
	private static void initSimulator() {
		Router router = new Router(SimulatorProperties.bufferLength, SimulatorProperties.routerBroadcastRate, SimulatorProperties.routerPolicy);
		Simulator.getInstance().backgroundTraffic = new BackgroundTraffic(SimulatorProperties.averageGustLength, SimulatorProperties.averageGustInterval);
		
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

	/** Adiciona às informações que o gráfico vai plotar, o tamanho da janela, no tempo atual.  
	 * 
	 * @param time
	 * @param server
	 */
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

	public void shotEvent(Object sender, long time, long leaveServerTime, EventType type, PackageModel packageModel) {
		Event event = new Event(packageModel, sender, time, leaveServerTime,
				type);
		if (event.getSender().getClass().equals(Server.class) && event.getType().equals(EventType.PACKAGE_SENT)) {			
//			System.out.println(event);
		}
		eventBuffer.add(event);
	}
}
