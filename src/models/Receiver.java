package models;

import java.util.Set;
import java.util.TreeSet;

import Utils.SimulatorProperties;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;

/**
 * 
 * Implementação da classe que representa uma estação TCP receptora.
 * <p>
 * Sua função é simular uma estação receptora de uma sessão TCP aberta, tendo, portanto, seu par Transmissor.
 * O receptor irá simular o recebimento de pacotes e então enviar o ACK correspondente, em resposta ao servidor.
 *
 * @see Server
 */
public class Receiver implements Listener {

	/**
	 * Estação TCP transmissora ligada a esta estação receptora.
	 */
	private Server server;
	
	/**
	 * Indica o próximo pacote a ser recebido.
	 */
	private PackageModel nextPackage;
	/**
	 * Guarda instancia única do simulador 
	 */
	private Simulator simulator;
	
	/**
	 * Conjunto de pacotes recebidos.
	 */
	private Set<PackageModel> receivedPackages;

	/**
	 * Constrói uma estação TCP receptora que irá escutar os eventos do tipo <code>EventType.PACKAGE_DELIVERED</code>.
	 * Por padrão, inicializa o próximo pacote a ser recebido como value 0.
	 * @param server servidor que manda pacotes excluivamente para este receptor
	 */	
	public Receiver(Server server) {
		super();
		this.server = server;
		nextPackage = new PackageModel(0);
		
		receivedPackages = new TreeSet<PackageModel>();
		
		simulator = Simulator.getInstance();
		simulator.registerListener(this, EventType.PACKAGE_DELIVERED);
	}

	/**
	 * Implementação do método responsável por escutar os eventos.
	 * <p>
	 * Ele irá escutar os eventos do tipo <code>EventType.PACKAGE_DELIVERED</code>, enviados pela estação transmissora(roteador) que está servindo ele.
	 * <p>
	 * Caso o pacote recebido corresponda ao próximo pacote que ele estava esperando, o receptor então atualiza o próximo pacote esperado,
	 * levando em consideração os pacotes que ele já tenha recebido, enquanto esperava pelo próximo pacote esperado.
	 * <p>
	 * No caso dele receber um pacote que não seja o esperado, ele então armazena o pacote no conjunto de pacotes recebidos.
	 * <p>
	 * Após essas verificações, o ACK correspondente ao recebimento do pacote é então enviado, contendo o um pacote que guarda a lista de pacotes recebidos no sackOption.
	 *  
	 * @param event evento que será escutado. Se a estação transmissora não corresponder ao Server desta estação, nada será feito.
	 */
	@Override
	public void Listen(Event event) {
		if (event.getSender().equals(getServer())) {
			PackageModel eventPackage = event.getPackageModel();
						
			if (eventPackage.equals(nextPackage)) {
				
				//Procura próximo pacote ainda não recebido
				nextPackage = new PackageModel(nextPackage.getValue() + SimulatorProperties.MSS);
				Set<PackageModel> packagesToRemove = new TreeSet<PackageModel>();
				
				while (receivedPackages.contains(nextPackage)) {
					packagesToRemove.add(nextPackage);
					nextPackage = new PackageModel(nextPackage.getValue() + SimulatorProperties.MSS);
				}
				
				//Limpa pacotes da primeira sequencia completa da lista de recebidos.
				receivedPackages.removeAll(packagesToRemove);
				nextPackage.setSackOption(receivedPackages);
				sendAck(event);
			} else if (eventPackage.compareTo(nextPackage) == 1 && !receivedPackages.contains(eventPackage)){ //Se o pacote recebido for posterior ao esperado. Se for anterior ignora, pois já foi recebido
				receivedPackages.add(eventPackage);
				sendAck(event);
			}
			
		}				
	}

	/**
	 * Envia o ACK correspondente ao recebimento de um pacote.
	 * <p>
	 * Dispara um evento que simula o envio de um ACK, informando o próximo pacote esperado, 
	 * assim como os pacotes posteriores que já foram recebidos.
	 *  
	 * @param event evento do recebimento de um pacote
	 */
	private void sendAck(Event event) {
		Set<PackageModel> newReceivedPackages = new TreeSet<PackageModel>();
		PackageModel returnPackage = new PackageModel(nextPackage.getValue());
		for (PackageModel packageModel : receivedPackages) {
			newReceivedPackages.add(packageModel);
		}
		
		returnPackage.setSackOption(newReceivedPackages);
		
		long initialTime = event.getTime();
		simulator.shotEvent(this, initialTime + getServer().getGroup().getDelay(), event.leaveServerTime(), EventType.ACK, returnPackage);
	}

	/**
	 * Retorna a estação transmissora que está servindo esta estação receptora.
	 * @return referência para a estação transmissora.
	 */
	public Server getServer() {
		return server;
	}
}
