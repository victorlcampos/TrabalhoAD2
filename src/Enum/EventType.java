package Enum;
/**
 * 
 * Conjunto dos tipos de eventos que serão simulados ao longo da simulação.
 * <p>
 * Segue uma explicação detalha do que representa cada evento:
 * <p>
 * Evento				Sender		Receiver			Descrição
 * <p>
 * TIME_OUT				Server		Server				Evento que representa o timeout
 * <p>
 * PACKAGE_SENT			Server		Router				Evento que representa a chegada no roteador
 * <p>
 * PACKAGE_DELIVERED	Server		Receiver			Evento que representa a entrega do pacote ao receptor
 * <p>
 * ACK					Receiver	Server				Evento que representa um ACK
 * 
 */
public enum EventType {
	PACKAGE_SENT, PACKAGE_DELIVERED, ACK, TIME_OUT
}
