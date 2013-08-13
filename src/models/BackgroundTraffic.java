package models;

import java.util.Random;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.stat.descriptive.moment.GeometricMean;

import Utils.SimulatorProperties;

import models.interfaces.Listener;
import Controller.Simulator;
import Enum.EventType;

/**
 * 
 * Implementação da classe responsável por simular o tráfego de fundo.
 * <p>
 * Para minimizar o esforço de programação, além de evitar duplicação de código, 
 * esta classe extende a classe <code>Server</code>, pelo fato dela ser uma espécie de estação transmissora
 * que não recebe acks. Esta irá servir para congestionar o tráfego no <code>Router</code>.
 * 
 * @see Server
 *
 */
public class BackgroundTraffic implements Listener {
	
	/**
	 * Tamanho médio das rajadas
	 */
	private Integer avgGustLength;
	
	/**
	 * Número de pacotes a serem enviados. Gerado aleatoriamente para cada rajada
	 */
	private Integer numPackagesToSend;
	
	/**
	 * Número de pacotes enviados numa rajada. 
	 * Também pode significar o número de pacotes que foram enviados e ainda faltam ser escutados pelo listen()
	 */
	private Integer numPackagesSent;
	
	/**
	 * Armazena o próximo pacote a ser enviado.
	 */
	private PackageModel nextPackageToSend;
	
	/**
	 * Objeto da classe ExponentialDistribuition; usado para gerar o tempo de chegada da rajada no roteador
	 */
	private ExponentialDistribution exponentialDistribution;
	
	/**
	 * Objeto da classe Random; usado para gerar aleatoriamente o número de pacotes de uma rajada
	 */
	private Random randomNumber;

	
	/**
	 * Constrói um tráfego de fundo com as informações sobre as rajadas que transmitirá. 
	 * <p>
	 * Este tráfego irá escutar os eventos do tipo <code>EventType.PACKAGE_SENT</code>.
	 * 
	 * @param avgGustLegth 		Tamanho médio das rajadas
	 * @param avgGustInterval	Intervalo médio das rajadas
	 */	
	public BackgroundTraffic(Integer avgGustLength, Double avgGustInterval) {
		Simulator.getInstance().registerListener(this, EventType.PACKAGE_SENT);
		this.nextPackageToSend = new PackageModel(0);
		this.avgGustLength = avgGustLength;
						
		this.exponentialDistribution = new ExponentialDistribution(avgGustInterval);
		randomNumber = new Random(System.nanoTime());
		sendGust(0l);
	}	
	
	/**
	 * Chamado no construtor, e sempre que necessário, inicializa o tráfego de fu <code>EventType.PACKAGE_SENT</code>.
	 * <p>
	 * @param time tempo atual
	 */	
	private void sendGust(Long time) {
		
		//Inicializa aleatoreamente, atraves do método round do Java, o número de pacotes da rajada
		numPackagesToSend = (int) Math.round(gustLength());
		numPackagesSent = 0;
		
		//Gera, atraves de uma amostra distribuída exponencialmente, o tempo restante para o início da rajada
		Long gustTime = (long) exponentialDistribution.sample();
		if (numPackagesToSend == 0) {
			//Caso o numero de pacotes da rajada gerado aleatoriamente seja 0, chama novamente sendGust, para o tempo da próxima rajada
			sendGust(time + gustTime);
		} else {
			//Envia todos os pacotes da rajada
			for (int i = 0; i < numPackagesToSend; i++) {
				//Dispara evento de chegada de pacote na fila, com o próximo pacote, para o tempo atual + gustTime
				Simulator.getInstance().shotEvent(this, time+gustTime, time+gustTime, EventType.PACKAGE_SENT, nextPackageToSend);
				//Cria próximo pacote a ser enviado
				nextPackageToSend = new PackageModel(nextPackageToSend.getValue() + SimulatorProperties.MSS);
				numPackagesSent++;
			}
		}
	}

	/**
	 * Escuta os eventos enviados pelo tráfego de fundo, cujo tipo corresponde a <code>EventType.PACKAGE_SENT</code>.
	 * <p>
	 * O evento simula o envio de vários pacotes pelo tráfego de fundo, que ocorrem a uma determinada taxa.<br>
	 * Como, além do Router, o BackgroundTraffic também escuta eventos do tipo <code>EventType.PACKAGE_SENT</code>,
	 * a cada evento escutado o numero de pacotes enviados é decrementado, até que todos os eventos de pacotes
	 * enviados nesta rajada sejam escutados, e assim será chamado novamente sendGust() para enviar a próxima rajada.
	 * 
	 * @param event evento do tipo <code>EventType.PACKAGE_SENT</code>. <br>
	 * Caso o evento não tenha sido disparado pelo tráfego de fundo, nada será feito.
	 */
	@Override
	public void Listen(Event event) {
		if (event.getSender().equals(this)) {
			if (numPackagesSent == 1) {
				sendGust(event.getTime());
			}else {
				numPackagesSent--;
			}
		}
	}
	
	/**
	 * Gera e retorna o tamanho da rajada baseado num número aleatorio gerado pela classe Random, e na largura média das rajadas
	 * 
	 */
	public double gustLength(){		
		return Math.log(randomNumber.nextDouble()) / Math.log(1 - 1d/avgGustLength);
	}
	
	/**
	 * Seleciona nova semente para o número aleatório, para melhorar a aleatoriedade num longo tempo
	 */
	public void reseed() {
		randomNumber = new Random(System.nanoTime());
	}
}
