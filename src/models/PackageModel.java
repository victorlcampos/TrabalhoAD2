package models;

import java.util.Set;
import java.util.TreeSet;

/**
 * Classe que representa o pacote enviado do servidor ao roteador e do roteador ao receptor
 *
 */
public class PackageModel implements Comparable<PackageModel> {
	/** 
	 * Valor inteiro que identifica o pacote
	 */
	private Long value;
	
	/**
	 * Lista de pacotes recebidos fora de ordem pelo receptor que enviou o ack que contem este pacote<br>
	 * Prenchido apenas quando criado e colocado dentro de um ACK, na classe Receiver; nos outros casos fica vazia.
	 */
	private Set<PackageModel> sackOption;

	/**
	 * Construtor que cria um pacote com identificador id, e inicializa a lista sackOption
	 * @param id
	 */
	public PackageModel(long id) {
		super();
		sackOption = new TreeSet<PackageModel>();
		this.value = id;
	}

	/** 
	 * Retorna identificador do pacote
	 * @return value
	 */
	public Long getValue() {
		return value;
	}
	
	/**
	 * Retorna a lista de pacotes recebidos fora de ordem pelo receptor que enviou o ack no qual este pacote está contido
	 * @return
	 */
	public Set<PackageModel> getSackOption() {
		return sackOption;
	}

	/** 
	 * Substitui valor da lista sackOption
	 * @param sackOption
	 */
	public void setSackOption(Set<PackageModel> sackOption) {
		this.sackOption = sackOption;
	}

	/**
	 * Compara pelo identificador do pacote, que é o campo value
	 */
	@Override
	public boolean equals(Object obj) {
		return this.value.equals(((PackageModel) obj).getValue());
	}
	
	/** 
	 * Retorna uma String com uma representação mais legível do objeto.
	 * @return value do pacote e a sequencia de pacotes recebidos contidos na opcao sack
	 */
	@Override
	public String toString() {
		if (sackOption.size() > 0) {
			return value.toString()+"("+sackOption+")";			
		} else {
			return value.toString();
		}
	}

	/**
	 * Compara dois PackageModel pelo seu identificador
	 * @return 	- 0 se o PackageModel passado for igual ao que chamou a função;<br>
	 * 			- positivo se quem chamou a função é um pacote posterior ao pacote passado;<br>
	 * 			- negativo se quem chamou a função é um pacote anterior ao pacote passado.
	 */
	@Override
	public int compareTo(PackageModel o) {
		return value.compareTo(o.getValue());
	}	
}
