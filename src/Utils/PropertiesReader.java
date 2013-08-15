package Utils;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import Enum.RouterType;

public class PropertiesReader {

	public static final String FILENAME = "simulador.txt";

	@SuppressWarnings("resource")
	public static void readProperties() {
		Scanner scanner;
		try {
			scanner = new Scanner(new FileReader(FILENAME));
		} catch (FileNotFoundException e) {
			System.out.println("=============ERRO NA LEITURA DO ARQUIVO DE ENTRADA============");
			e.printStackTrace();
			return;
		}
		
		SimulatorProperties.routerBroadcastRate = scanner.nextLong() / 8;
		SimulatorProperties.serverBroadcastRate = scanner.nextLong() / 8;
		SimulatorProperties.serverGroupsNumber = scanner.nextInt();
		
		SimulatorProperties.serverGroupDelay = new Long[SimulatorProperties.serverGroupsNumber];
		SimulatorProperties.serverGroupQuantity = new Long[SimulatorProperties.serverGroupsNumber];
		
		for (int i = 0; i < SimulatorProperties.serverGroupsNumber; i++) {
			SimulatorProperties.serverGroupDelay[i] = scanner.nextLong() * 1000l*1000l;
			SimulatorProperties.serverGroupQuantity[i] = scanner.nextLong();
		}
		
		SimulatorProperties.averageGustLength = scanner.nextInt();
		SimulatorProperties.averageGustInterval = scanner.nextDouble() * 1000l*1000l;
		
		SimulatorProperties.bufferLength = scanner.nextInt();
		SimulatorProperties.MSS = scanner.nextLong();
		
		String line = scanner.next();
		for(RouterType policy: RouterType.values()){
			if(line.equals(policy.name()) ){
				SimulatorProperties.routerPolicy = policy;
			}
		}
		
		SimulatorProperties.totalSimulationTime = scanner.nextLong() * 1000l*1000l;
		SimulatorProperties.transientTime = scanner.nextLong() * 1000l*1000l;
		
		printInputData();
	}
	
	/**
	 * Imprime no console os valores lidos do arquivo de entrada.
	 */
	private static void printInputData() {
		System.out.println("	================ LOG DADOS DO ARQUIVO =================");
		System.out.println("	=======================================================\n");
		System.out.println("		Tamanho médio das rajadas do tráfego de fundo: " + SimulatorProperties.averageGustLength);
		System.out.println("		Intervalo médio entre rajadas do tráfego de fundo: " + SimulatorProperties.averageGustInterval + " ns");
		System.out.println("		Tamanho do buffer: " + SimulatorProperties.bufferLength + " pacotes");
		System.out.println("		MSS: " + SimulatorProperties.MSS + " bytes");
		System.out.println("		Política de atendimento: " + SimulatorProperties.routerPolicy);
		System.out.println("		Taxa de transmissão do roteador: " + SimulatorProperties.routerBroadcastRate + " bpns");
		System.out.println("	   ---------------------------------------------");
		for (int i = 0; i < SimulatorProperties.serverGroupsNumber; i++) {
			System.out.println("		Delay dos Servidores do grupo " + (i+1) +  ": " + SimulatorProperties.serverGroupDelay[i] + " ns");
			System.out.println("		Quantidade de Servidores do grupo " + (i+1) +  ": " + SimulatorProperties.serverGroupQuantity[i]);
		}
		System.out.println("	   ---------------------------------------------");
		System.out.println("		Tempo total de simulação: " + SimulatorProperties.totalSimulationTime + " ns");
		System.out.println("		Estimativa da fase transiente: " + SimulatorProperties.transientTime + " ns");
		 		
		System.out.println("\n	=======================================================");
		System.out.println("	=======================================================\n");
		
	}
}
