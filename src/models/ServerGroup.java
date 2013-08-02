package models;

public class ServerGroup {
	private Long delay;

	public ServerGroup(Long delay) {
		super();
		this.delay = delay;
	}

	public Long getDelay() {
		return delay;
	}

	public void setDelay(Long delay) {
		this.delay = delay;
	}
}
