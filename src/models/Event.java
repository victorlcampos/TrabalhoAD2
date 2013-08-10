package models;

import Enum.EventType;
/**
 * 
 * Implementação da classe que será responsável por representar um evento dentro da simulação.
 * 
 * @see EventType
 *
 */
public class Event implements Comparable<Event> {
	/**
	 * Pacote enviado no caso de PACKAGE_SENT e PACKAGE_DELIVERED
	 * Próximo pacote esperado, no caso de ser um evento do tipo ACK
	 * Pacote a ser reenviado no caso de TIME_OUT
	 */
	private PackageModel packageModel;
	/**
	 * Referência para quem enviou esse evento
	 */
	private Object sender;
	/**
	 * Tempo na simulação em que esse evento ocorreu
	 */
	private Long time;
	/**
	 * Tempo na simulação em que o pacote foi deixou o servidor
	 */
	private Long leaveServerTime;
	/**
	 * Tipo do evento que está sendo representado
	 */
	private EventType type;

	/**
	 * 
	 * Construtor padrão que recebe todas os atributos necessários para representar um evento, inicializando as variáveis correspondentes.
	 * 
	 * @param eventType tipo do evento sendo representado
	 * @param time tempo em que o evento ocorreu
	 * @param sender quem enviou o evento
	 * @param packageModel
	 */	
	public Event(PackageModel packageModel, Object sender, Long time,
			Long leaveServerTime, EventType type) {
		super();
		this.packageModel = packageModel;
		this.sender = sender;
		this.time = time;
		this.leaveServerTime = leaveServerTime;
		this.type = type;
	}

	/**
	 * Retorna uma referência para o pacote do evento
	 * @return packageModel
	 */
	public PackageModel getPackageModel() {
		return packageModel;
	}

	/**
	 * Substitui o campo packageModel
	 * @param packageModel
	 */
	public void setPackageModel(PackageModel packageModel) {
		this.packageModel = packageModel;
	}

	/**
	 * Retorna o objeto responsável que enviou o evento.
	 * @return objeto que enviou o evento.
	 */
	public Object getSender() {
		return sender;
	}

	/**
	 * Substitui o objeto responsável que enviou o evento.
	 * @param sender objeto que enviou o evento
	 */
	public void setSender(Object sender) {
		this.sender = sender;
	}

	/**
	 * Retorna o tempo em que este evento o ocorreu na simulação.
	 * @return tempo em que o evento ocorreu.
	 */
	public Long getTime() {
		return time;
	}

	/**
	 * Substitui o tempo armazenado em que o evento ocorreu pelo novo tempo especificado.
	 * @param time tempo em que o evento ocorreu.
	 */
	public void setTime(Long time) {
		this.time = time;
	}

	/**
	 * Retorna o tempo em que o pacote deixou o servidor
	 * @return leaveServerTime
	 */	public Long leaveServerTime() {
		return leaveServerTime;
	}

	/**
	 * Substitui o tempo em que o pacote deixou o servidor
	 * @param leaveServerTime
	 */
	 public void leaveServerTime(Long leaveServerTime) {
		this.leaveServerTime = leaveServerTime;
	}

	/**
	 * Retorna o tipo do evento que está sendo representado.
	 * @return tipo do evento
	 */
	public EventType getType() {
		return type;
	}

	/**
	 * Registra o tipo de evento que está sendo simulado.
	 * @param type tipo do evento.
	 */
	public void setType(EventType type) {
		this.type = type;
	}

	/**
	 * Realiza uma comparação entre este evento e um outro evento.
	 * Neste caso, a comparação está sendo feita pelo tempo em que o evento ocorreu.
	 */
	@Override
	public int compareTo(Event arg0) {
		return this.time.compareTo(arg0.time);
	}
	
	/**
	 * Retorna uma representação legível do evento em uma String.
	 * Nesta representação informados o tipo do evento, o tempo em que ele ocorreu, quem enviou o evento e o pacote.
	 */
	@Override
	public String toString() {
		return type+" - "+time+" - "+sender+" - "+packageModel;
	}
}
