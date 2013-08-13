package Enum;

/** 
 * Conjunto que assume os valores dos modos de transmissão de pacotes que um servidor pode assumir.
 *
 */
public enum ServerStatus {
	/**
	 * Fase de transmissão inicial que dura até que a janela cwnd atinja o mesmo valor do threshold
	 */
	SLOW_START, 
	
	/**
	 * Fase de transmissão que aumenta a janela numa velocidade menor que slow start, e que sempre segue a Slow Start quando atinge o threshold
	 */
	CONGESTION_AVOIDANCE, 
	
	/**
	 * Fase de recuperação de pacotes, que é iniciada ao receber um terceiro ack duplicado.
	 * <p>
	 * Nela, o servidor reenvia todos os pacotes que ainda não chegaram (contidos na opção sack), 
	 * e permanece até que receba ack indicando que todos foram recebidos
	 */
	FAST_RETRANSMIT
}
