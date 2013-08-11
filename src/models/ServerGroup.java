package models;

/**
 * 
 * Implementação de um grupo de servidores de um mesmo tipo.
 * <p>
 * Esta classe irá representar um tipo de grupo dos servidores sendo estudados.
 * Seu objetivo é agrupar servidores de um mesmo tipo, armazenando para este tipo a
 * informação acerca do atraso o qual um pacote sofre, quando é enviado por um servidor deste tipo.
 *
 */
public class ServerGroup {
	
	/**
	 * Tempo que um pacote demora desde o momento em que ele sai do servidor, até chegar ao roteador.
	 */
	private Long delay;

	/**
	 * Inicializa um grupo de servidores que compartilham o mesmo atraso informado. 
	 * 
	 * @param delay atraso que um pacote sofre para ir do servidor até o roteador.
	 */
	public ServerGroup(Long delay) {
		super();
		this.delay = delay;
	}

	/**
	 * Retorna o atraso que um pacote sofre para ir do servidor até o roteador, quando ele for enviado por um servidor deste grupo.
	 * 
	 * @return tempo que um pacote demora desde o momento em que ele sai do servidor, até chegar ao roteador.
	 */
	public Long getDelay() {
		return delay;
	}

	/**
	 * Retorna uma string que representa e identifica o grupo, através de seu delay
	 */
	@Override
	public String toString() {
		return "Grupo "+delay;
	}
}
