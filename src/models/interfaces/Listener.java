package models.interfaces;

import models.Event;

/**
 *
 * Interface que deverá ser implementada por qualquer
 * classe que queira escutar os eventos da simulação.
 *
 */
public interface Listener {

	/**
	 * Método a ser implementado que irá manipular os eventos recebidos pelo por esse <code>Listener</code>.
	 * 
	 * @param event evento a ser tratado.
	 */
	public void Listen(Event event);
}
