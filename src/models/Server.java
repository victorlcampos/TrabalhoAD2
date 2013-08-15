package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;
import Enum.ServerStatus;
import Utils.SimulatorProperties;

/**
 * 
 * Implementação da classe que representa uma estação TCP transmissora.
 * <p>
 * Sua função é simular uma estação transmissora de uma sessão TCP aberta, tendo, portanto, seu par receptor.
 * O transmissor irá simular o envio de pacotes, obedecendo todo o protocolo TCP.
 *
 */
public class Server implements Listener {
	/**
	 * Variável que controla o id do próximo Server a ser criado
	 */
	private static int id = 1;

	/**
	 * Armazena a id do server.
	 */	
	private Integer myId;
	
	/**
	 * Taxa com que um servidor envia pacotes para um roteador em bytes por segundo em bytes.
	 */
	private Long broadcastRate;
	
	/**
	 * Tempo real de retorno do pacote ao servidor
	 */
	private Long rtt;

	/**
	 * Threshold com a qual o TCP está operando em bytes.
	 */
	private Double threshold;

	/**
	 * Janela móvel pela qual a estação transmissora TCP controla quais bytes podem ser transmitidos.
	 * Esta janela inclui todos os bytes que em um dado momento podem estar em transmissão, pendente de recebimento de ACK.
	 * ACK é um pacote enviado pelo RxTCP que indica qual o próximo byte esperado para recebimento em ordem.
	 */
	private Double cwnd;
	
	/**
	 * Estação TCP receptora que irá receber os pacotes transmitidos por esta estação transmissora.
	 */	
	private Receiver receiver;
	
	/**
	 * Modo de transmissão do servidor, podendo ser Slow Start, Congestion Avoidance ou Fast Retransmit
	 */
	private ServerStatus status;
	
	/**
	 * Grupo a qual pertence este servidor. 
	 */
	private ServerGroup group;

	/**
	 * Referência para a instância única do Simulator
	 */
	private Simulator simulator;
	
	/**
	 * Número de pacotes a serem enviados nesta janela
	 */
	private Integer numOfPackages;
	
	/**
	 * Número de acks duplicados recebidos
	 */
	private Integer duplicatedAcks;

	/**
	 * Armazena o pacote do último ACK recebido.
	 */	
	private PackageModel lastAck;
	
	/**
	 * Registra qual é o próximo pacote a ser enviado.
	 */
	private PackageModel nextPackageToSend;
	
	/**
	 * Armazena os pacotes enviados que estão pendentes de recebimento de ACK.
	 */	
	private Set<PackageModel> sentPackages;
	
	/**
	 * Conjunto dos pacotes que o receptor recebeu fora de ordem.
	 */
	private Set<PackageModel> receivedAckPackages;
	
	/**
	 * Lista de pacotes a serem reenviados no fast retransmit
	 */
	private Set<PackageModel> waitingPackages;

	/**
	 * Tempo esperado para se receber um ACK de um pacote enviado.
	 * Recalculado a cada timeout.
	 */	
	private long expectedReturnTime;
	
	/**
	 * Desvio padrão do tempo esperado para se receber um ACK de um pacote enviado.
	 */
	private long deviationReturnTime;

	/**
	 * Constrói uma estação TCP transmissora, que irá escutar os eventos do tipo 
	 * <code>EventType.TIME_OUT</code> e <code>EventType.ACK</code>.
	 * 
	 * @param broadcastRate taxa com que o servidor envia seus pacotes em bytes por segundo.
	 * @param group grupo a qual pertence o servidor.
	 * @param cwnd tamanho da janela de transmissão em bytes
	 */	
	public Server(Long cwnd, ServerGroup group, Long broadcastRate) {
		super();
		this.cwnd = (double) cwnd;
		this.group = group;
		this.broadcastRate = broadcastRate;
		
		//Inicializa threshold com valor padrão
		threshold = 65535d;
		//Servidor inicia transmissão no modo slow start
		status = ServerStatus.SLOW_START;
		
		myId = id++;
		//número de pacotes a serem enviados inicia em 1
		numOfPackages = 1;
		
		sentPackages = new TreeSet<PackageModel>();
		receivedAckPackages = new TreeSet<PackageModel>();
		waitingPackages = new TreeSet<PackageModel>();
		
		expectedReturnTime = 4*group.getDelay();
		rtt = expectedReturnTime;
		deviationReturnTime = 0;
		
		simulator = Simulator.getInstance();
		simulator.registerListener(this, EventType.TIME_OUT);
		simulator.registerListener(this, EventType.ACK);
	}
	/**
	 * Seleciona a semente do rand, e envia o primeiro pacote num tempo aleatório
	 */	
	public void startServer(Receiver receiver) {
		this.receiver = receiver;
		//Inicializado com primeiro pacote, com id 0
		lastAck = new PackageModel(0);
		
		Random rand = new Random(System.nanoTime());
		
		sendPackage(rand.nextInt(1000)*1000000l, lastAck);
	}

	/**
	 * Implementação do método responsável por escutar os eventos.
	 * <p>
	 * Ele irá escutar os eventos do tipo  
	 * <code>EventType.TIME_OUT</code> e <code>EventType.SACK</code>, 
	 * delegando cada tipo de evento a um método diferente, para ser tratado especificamente.
	 * 
	 * @param event evento que será escutado. Caso o tipo do evento não corresponda a nenhum tipo de evento tratado pelo servidor, nada será feito.
	 */
	@Override
	public void Listen(Event event) {
		System.out.println(event);
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

	/**
	 * Envia o pacote, através de um evento que será escutado pelo roteador. E também programa o timeout
	 * <p>
	 * Se ainda houver pacote para ser enviado, envia o próximo, chamando outra vez o sendPackage()
	 * 
	 * @param initialTime
	 * @param packageModel
	 */
	private void sendPackage(Long initialTime, PackageModel packageModel) {
		
		//Cancela time out se houver, no caso de estar reenviando pacote.
		cancelTimeout(packageModel);
		
		//Calcula tempo de serviço através da taxa de transmissão
		Long serviceTime = (long) (1000l*1000l*1000l*SimulatorProperties.MSS/broadcastRate);
		//Tempo que pacote deve sair do servidor
		Long finishedServiceTime = initialTime+serviceTime;
		
		//Dispara evento para ocorrer no tempo que sai do servidor + o tempo para chegar no roteador
		simulator.shotEvent(this, finishedServiceTime+group.getDelay(), initialTime, EventType.PACKAGE_SENT, new PackageModel(packageModel.getValue()));
		
		//Calcula o timeout para este tempo de saída do servidor
		Long timeoutTime = getTimeoutTime(finishedServiceTime);
		//Dispara evento de timeout para o tempo calculado, e para um pacote de mesmo id(value) que pacote enviado
		simulator.shotEvent(this, timeoutTime, initialTime, EventType.TIME_OUT, new PackageModel(packageModel.getValue()));
		
		if (sentPackages.contains(packageModel)) {
			throw new RuntimeException("Enviando pacote já enviado");
		} else {	
			//Adiciona o pacote enviado à lista de pacotes enviados
			sentPackages.add(packageModel);
			verifyTimeOut();
		}
		
		//Uma vez que acabou de enviar um pacote, decrementa o número de pacotes a enviar nesta janela
		numOfPackages--;
		
		if (numOfPackages > 0) {
			getNextPackage();
			//Chama sendPackage para enviar o próximo pacote
			sendPackage(finishedServiceTime, nextPackageToSend);
		}
	}

	/**
	 * Calcula o timeout para o envio de um pacote.
	 * 
	 * @param event evento do tipo <code>EventType.SENDING_PACKAGE</code>.
	 */
	private Long getTimeoutTime(Long finishedServiceTime) {
		Long timeOutTime = expectedReturnTime + 4l*deviationReturnTime;
		
		if (timeOutTime < 2*group.getDelay()) {
			throw new RuntimeException("Tempo do timeout calculado errado");
		}
		
		return finishedServiceTime + timeOutTime;
	}

	/**
	 * Método que irá tratar os eventos do tipo <code>EventType.TIME_OUT</code>, enviados pelo próprio servidor.
	 * <p>
	 * Este tipo de evento representa o timeout de um envio de um pacote. 
	 * Ou seja, o ACK esperado pelo envio de um pacote não chegou dentro do tempo limite, indicando um congestionamento severo.
	 * <p>
	 * Sendo assim, faz-se:
	 * <p>
	 * <code>
	 * threshold = txwnd/2 //(txwnd que valia no momento do time-out)
	 * cwnd = 1 MSS
	 * </code>
	 * <p>
	 * e entra-se em slow start.
	 * <p>
	 * O pacote então é reenviado pelo servidor.
	 * 
	 * @param event evento do tipo <code>EventType.TIME_OUT</code>, Caso o <code>Sender</code> do evento não tenha sido o próprio servidor, nada será feito.
	 */
	private void listenTimeOut(Event event) {
		if (event.getSender().equals(this)) {						
			threshold = Math.max(cwnd/2, SimulatorProperties.MSS);
			
			cwnd = (double) SimulatorProperties.MSS;
			status = ServerStatus.SLOW_START;
			duplicatedAcks = 0;
			
			//Substitui valor do próximo pacote a ser enviado pelo pacote do timeout
			nextPackageToSend = event.getPackageModel();
			
			//Remove pacote que sofreu timeout da lista de pacotes enviados
			sentPackages.remove(nextPackageToSend);
			verifyTimeOut();
			
			//Inicia reenvio de pacote perdido
			resendPackages(event.getTime());
		}		
	}
	
	/**
	 * Método que irá tratar os eventos do tipo <code>EventType.ACK</code>, 
	 * enviados pela estação receptora conectada a esta estação transmissora.
	 * <p>
	 * Este tipo de evento confirma o recebimento de um pacote por parte da estação receptora.
	 * <p>
	 * Neste método, o conjunto receivedAckPackages recebe os elementos da lista de pacotes recebidos contidos no sackOption
	 * 
	 * @param event evento do tipo <code>EventType.ACK</code>, Caso o <code>Sender</code> do evento não tenha sido o <code>Receptor</code> conectado a este <code>Server</code>, nada será feito.
	 */
	private void listenAck(Event event) {
		if (event.getSender().equals(receiver)) {
			
			PackageModel eventPackage = event.getPackageModel();
			//Pega na opção sack, os pacotes recebidos fora de ordem
			receivedAckPackages = eventPackage.getSackOption();
			
			//Cancela todos os timeouts dos pacotes recebidos pelo receptor. Mesmo que a maioria já tenha sido cancelada no último recebimento, tenta cancelar todos
			cancelReceivedAcksTimeOut();
			
			//Calcula variáveis do tempo de timeout
			estimateTimeOutCalc(event);
			
			if (status.equals(ServerStatus.FAST_RETRANSMIT)) {
				fastRetransmitAck(event);				
			}else if (eventPackage.equals(lastAck)) {	//Se é um ack duplicado, ou seja, espera mesmo pacote que o último ack.	
				//Providencia entrada no modo fast retransmit e inica recuperação
				duplicatedAck(event);
			}else {
				//Trata recebimento de ack que indica pacote novo recebido na ordem
				rigthAck(event, eventPackage);	
			}
		}
	}

	/**
	 * Cancela o timeout de todos os pacotes ja recebidos pelo receptor, usando a lista receivedAckPackages
	 */
	private void cancelReceivedAcksTimeOut() {
		for (PackageModel packageModel : receivedAckPackages) {
			cancelTimeout(packageModel);
		}
	}

	/**
	 * Chamado no listenAck() para tratar recebimento de ack quando se está em fast retransmit.
	 * <p>
	 * Se for ack duplicado, incrementa tamanho da janela,
	 * se for ack indicando recebimento de pacote novo, remove este da lista dos acks esperados.<br>
	 * E se ao remover, a lista ficar vazia, passa para Congestion Avoidance.
	 * <p>
	 * Neste último caso, ao final, desloca-se a janela.
	 * <p>
	 * Sempre é requisitado ao sendPackage() o envio do próximo pacote
	 * @param event
	 */
	private void fastRetransmitAck(Event event) {
		
		PackageModel eventPackage = event.getPackageModel();
		
		if (eventPackage.equals(lastAck)) {//Se for ack duplicado, aumenta a janela
			cwnd += SimulatorProperties.MSS;
			numOfPackages = getNumOfPackages();				
		} else {
			//Se ack indica recebimento de pacote novo, remove ele da lista de pacotes a recuperar
			Integer waitingPackageSize = waitingPackages.size();
			waitingPackages.remove(lastAck);
			waitingPackages.removeAll(receivedAckPackages);
						
			//Se já tiver recebido ack de todos os pacotes perdidos, passa para congestion avoidance
			if (waitingPackages.size() == 0) {
				duplicatedAcks = 0;
				status = ServerStatus.CONGESTION_AVOIDANCE;
				System.out.println(ServerStatus.CONGESTION_AVOIDANCE);
				cwnd = threshold;
			} else {
				cwnd += SimulatorProperties.MSS;
				cwnd -= (waitingPackageSize - waitingPackages.size());
			}
			
			//Desloca janela de transmissão
			walkWithWindow(event);
		}
		
		getNextPackage();
		sendPackage(event.getTime(), nextPackageToSend);
	}

	/**
	 * Calcula o tempo de retorno do pacote, e o desvio padrão e tempo de retorno esperado através da seguinte fórmula:
	 * <p>
	 * <code>
	 * Y = M - A  <br>
	 * D ← D + 0,25 * (|Y| - D) <br>
	 * A ← A + 0,125 * Y
	 * </code>
	 * <p>
	 * onde M é o tempo entre o envio do pacote pelo TxTCP e o recebimento do ACK correspondete, nosso <code>rtt</code>,
	 * A é o valor estimado do RTT, nosso <code>expectedReturnTime</code>,
	 * Y é o nosso <code>differenceBetweenRealAndExpectation</code> e
	 * D é o valor estimado do desvio médio, nosso <code>deviationReturnTime</code>.
	 *  
	 * @param event evento do tipo ACK
	 */
	private void estimateTimeOutCalc(Event event) {
		rtt = event.getTime() - event.leaveServerTime();					
		Long differenceBetweenRealAndExpectation = rtt - expectedReturnTime;
		deviationReturnTime += (long) (Math.abs(differenceBetweenRealAndExpectation) - deviationReturnTime)/4;
		expectedReturnTime += (long) differenceBetweenRealAndExpectation/8;
	}

	/**
	 * Trata recebimento de pacote novo recebido na ordem correta.
	 * <p>
	 * Anda com a janela, e envia próximo pacote
	 * @param event
	 * @param eventPackage
	 */
	private void rigthAck(Event event, PackageModel eventPackage) {
		duplicatedAcks = 0;
		setStatus();
				
		if (this.status.equals(ServerStatus.SLOW_START)) {
			cwnd += SimulatorProperties.MSS;
		}else if(this.status.equals(ServerStatus.CONGESTION_AVOIDANCE)) {
			Double numOfAcks = cwnd/SimulatorProperties.MSS;
			if (numOfAcks == 0) {
				System.out.println(numOfAcks);
			}
			cwnd += SimulatorProperties.MSS/numOfAcks;
		}		
		walkWithWindow(event);
						
		getNextPackage();
		
		sendPackage(event.getTime(), nextPackageToSend);
	}

	/**
	 * Desloca janela de transmissão
	 * @param event
	 */
	private void walkWithWindow(Event event) {
		cancelTimeout(lastAck);
		
		lastAck = event.getPackageModel();
		
		Set<PackageModel> removeSentPackages = new TreeSet<PackageModel>();

		for (PackageModel packageModel : sentPackages) {
			if (packageModel.compareTo(lastAck) == -1) {
				cancelTimeout(packageModel);
				removeSentPackages.add(packageModel);
			}
		}
		
		sentPackages.removeAll(removeSentPackages);
		verifyTimeOut();
		numOfPackages = getNumOfPackages();

		nextPackageToSend = lastAck;
	}

	/**
	 * Trata o recebimento de um ack duplicado quando não se está já em fast retransmit. Chamado pelo listenAck()
	 * <p>
	 * Passa para Fast Retransmit, e inicia o reenvio dos pacotes perdidos
	 * @param event
	 */
	private void duplicatedAck(Event event) {
		duplicatedAcks++;
		if (duplicatedAcks == 3) {
			System.out.println(ServerStatus.FAST_RETRANSMIT);	
			//Ao receber o terceiro ack duplicado, reinicia a contagem
			duplicatedAcks = 0;
			//Diminui threshold pela metade
			threshold = Math.max(cwnd/2, SimulatorProperties.MSS);
			cwnd = threshold + 3*SimulatorProperties.MSS;
			//Remove pacote do ack esperado da lista de pacotes enviados
			removeSentPackage(lastAck);
			//O pacote esperado pelo ack será o primeiro a ser reenviado
			nextPackageToSend = lastAck;
			//Muda o modo de transmissão desse servidor para fast retransmit
			status = ServerStatus.FAST_RETRANSMIT;
			
			//Inicia reenvido dos pacotes perdidos
			resendPackages(event.getTime());			
		}
	}

	/**
	 * Reenvia pacotes, tratando o caso de ter sofrido timeout, ou de fast retransmit
	 * <p>
	 * Realiza-se uma verificação dos pacotes que já foram enviados e ainda não receberam
	 * a confirmação correspondente ao recebimento do pacote, para que esses também possam
	 * ser reenviados.
	 * <p>
	 * O próximo pacote a ser enviado então passa a ser o pacote que sofreu timeout,
	 * atualizando também o número de pacotes que ainda faltam ser enviados.
	 * <p>
	 * É enviado apenas o primeiro pacote, que irá disparar o envio dos próximos
	 * 
	 * @param nextPackage pacote a ser reenviado.
	 * @param time instante de tempo na simulação que o pacote será reenviado.
	 */	
	private void resendPackages(Long time) {
		
		//Reinicia contagem dos acks duplicados
		duplicatedAcks = 0;
		//Reinicia lista de pacotes esperando ack
		waitingPackages.clear();
				
		//Caso tenha sido chamado após receber o terceiro ack duplicado, já vai estar no modo fast retransmit - 
		if (status.equals(ServerStatus.FAST_RETRANSMIT)) {
			List<PackageModel> eventsToRemove = new ArrayList<PackageModel>();
			//Adiciona na lista dos perdidos o pacote esperado do ack recebido. Este pacote já foi removido da lista de pacotes enviados no duplicatedAck()
			waitingPackages.add(nextPackageToSend);
			for (PackageModel packageModel : sentPackages) {
				//Se receptor não recebeu pacote que está iterando
				if (!receivedAckPackages.contains(packageModel)) {
					//Vai adicionando todos os pacotes enviados que não foram recebidos na lista dos eventos a remover
					eventsToRemove.add(packageModel);
					//Também cancela o timeout deste pacote
					cancelTimeout(packageModel);
					//Adiciona na lista dos pacotes a recuperar
					waitingPackages.add(packageModel);
				}
			}
			//Remove todos os pacotes não recebidos da lista de pacotes enviados, pois vão ser enviados novamente
			sentPackages.removeAll(eventsToRemove);
			verifyTimeOut();
			//Cancela todos os envios pacotes deste servidor
			cancelAllSentEventsEvent();
		} 
		//Reenvio por timeout
		else {
			//Lista de eventos a serem removidos - Envio de pacotes 
			List<Event> eventsToRemove = new ArrayList<Event>();
			List<Event> eventBuffer = simulator.getEventBuffer();
			
			//Itera sobre eventos de envio ao roteador de pacotes posteriores ao que sofreu timeout
			for (int i = 0; i < eventBuffer.size(); i++) {
				Event event = eventBuffer.get(i);
				if (nextPackageToSend.compareTo(event.getPackageModel()) == -1) {						
					if (event.getSender().equals(this) && event.getType().equals(EventType.PACKAGE_SENT)) {
						//Adiciona o evento de envio de pacote à lista de eventos para serem removidos do buffer
						eventsToRemove.add(event);
						//Remove pacote da lista de pacotes enviados
						removeSentPackage(event.getPackageModel());
						//Cancela o timeout do pacote que esta iterando
						cancelTimeout(event.getPackageModel());
					}
				}
			}	
			//Remove eventos da lista de eventos do Simulator
			eventBuffer.removeAll(eventsToRemove);
		}
		
		//Atualiza número de pacotes que restam para enviar
		numOfPackages = getNumOfPackages();
		//Envia o próximo pacote
		sendPackage(time, nextPackageToSend);		

	}
	
	private void verifyTimeOut() {
		for (PackageModel sentPackage : sentPackages) {
			Boolean constansTimeoutPackage = false;
			
			if (receivedAckPackages.contains(sentPackage)) {
				continue;
			}
			
			List<Event> eventBuffer = simulator.getEventBuffer();
			
			for (int i = 0; i < eventBuffer.size(); i++) {
				Event event = eventBuffer.get(i);
				if (event.getSender().equals(this) && event.getType().equals(EventType.TIME_OUT) && event.getPackageModel().equals(sentPackage)) {
					constansTimeoutPackage = true;
					break;
				}
			}
			
			if (!constansTimeoutPackage) {
				throw new RuntimeException("Pacote "+sentPackage+" enviado sem timeout");
			}
		}		
	}

	/**
	 * Cancela todos os timeouts do pacote passado como parâmetro
	 * @param packageModel 
	 * 
	 */
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
	
	/** 
	 * Cancela os eventos de todos os pacotes enviados por este servidor.
	 * <p>
	 * Remove eventos do buffer do Simulator, e também da lista de pacotes enviados, assim como o timeout
	 */
	private void cancelAllSentEventsEvent() {
		List<Event> removedEvents = new ArrayList<Event>();
		List<Event> eventBuffer = simulator.getEventBuffer();
		
		for (int i = 0; i < eventBuffer.size(); i++) {
			Event event = eventBuffer.get(i);
			if (event.getSender().equals(this) && event.getType().equals(EventType.PACKAGE_SENT)) {
				removedEvents.add(event);
				removeSentPackage(event.getPackageModel());
				cancelTimeout(event.getPackageModel());
			}
		}	
		eventBuffer.removeAll(removedEvents);
	}

	/**
	 * Remove pacote da lista de pacotes enviados
	 * @param packageModel pacote a ser removido
	 */
	private void removeSentPackage(PackageModel packageModel) {
		sentPackages.remove(packageModel);
	}

	/**
	 * Atualiza o valor da variável nextPackageToSend com o próximo pacote a ser enviado
	 *
	 */
	private void getNextPackage() {
		
		//Itera até chegar no primeiro pacote da sequencia que não foi enviado ainda
		while(sentThisPackage(nextPackageToSend)) {
			//Cria uma referencia para o próximo pacote, na ordem de envio
			nextPackageToSend = new PackageModel(nextPackageToSend.getValue() + SimulatorProperties.MSS);
		}
	}

	/**
	 * Retorna tamanho da janela de transmissão em bytes
	 * @return cwnd
	 */
	public Double getCwnd() {
		return cwnd;
	}

	/**
	 * Substitui tamanho da janela de transmissão
	 * @param cwnd
	 */
	public void setCwnd(Double cwnd) {
		this.cwnd = cwnd;
	}

	/**
	 * Retorna um boolean indicando se o pacote passado por parâmetro já foi enviado
	 */
	private boolean sentThisPackage(PackageModel nextPackageToSend) {
		return sentPackages.contains(nextPackageToSend);
	}

	/**
	 * Altera o modo de transmissão de Slow Start para Congestion Avoidance, no caso de a janela ter atingido o threshold
	 * 
	 */
	private void setStatus() {
		if (status != ServerStatus.FAST_RETRANSMIT) {
			if (cwnd < threshold) {
				status = ServerStatus.SLOW_START;
			} else {
				status = ServerStatus.CONGESTION_AVOIDANCE;
			}
		}
	}
	
	/**
	 * Calcula número de pacotes restantes a serem enviados
	 * <p>
	 * n = tamanho da janela em pacotes  -  num de pacotes já enviados + num de acks recebidos
	 * @return número de pacotes atualizado
	 */
	private Integer getNumOfPackages() {
		Integer value = (int) Math.floor(cwnd/SimulatorProperties.MSS) - sentPackages.size() + receivedAckPackages.size();
		return  Math.max(0, value);
	}

	/**
	 * Retorna o grupo no qual este servidor está inserido.
	 * 
	 * @return <code>ServerGroup</code> que caracteriza o tipo deste servidor.
	 */
	public ServerGroup getGroup() {
		return group;
	}

	/**
	 * Retorna lista de pacotes enviados por este servidor
	 * @return sentPackages
	 */
	public Set<PackageModel> getSentPackages() {
		return sentPackages;
	}

	/**
	 * Substitui valor da lista de pacotes enviados por este servidor
	 * @param sentPackages
	 */
	public void setSentPackages(Set<PackageModel> sentPackages) {
		this.sentPackages = sentPackages;
	}

	/**
	 * Representação em <code>String</code> de um servidor. 
	 */
	@Override
	public String toString() {
		return "Servidor "+myId;
	}
}
