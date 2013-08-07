package models;

import Enum.EventType;

public class Event implements Comparable<Event> {
	private PackageModel packageModel;
	private Object sender;
	private Long time;
	private Long leaveServerTime;
	private EventType type;
	
	public Event(PackageModel packageModel, Object sender, Long time,
			Long leaveServerTime, EventType type) {
		super();
		this.packageModel = packageModel;
		this.sender = sender;
		this.time = time;
		this.leaveServerTime = leaveServerTime;
		this.type = type;
	}

	public PackageModel getPackageModel() {
		return packageModel;
	}

	public void setPackageModel(PackageModel packageModel) {
		this.packageModel = packageModel;
	}

	public Object getSender() {
		return sender;
	}

	public void setSender(Object sender) {
		this.sender = sender;
	}

	public Long getTime() {
		return time;
	}

	public void setTime(Long time) {
		this.time = time;
	}

	public Long leaveServerTime() {
		return leaveServerTime;
	}

	public void leaveServerTime(Long leaveServerTime) {
		this.leaveServerTime = leaveServerTime;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

	@Override
	public int compareTo(Event arg0) {
		return this.time.compareTo(arg0.time);
	}
	
	@Override
	public String toString() {
		return type+" - "+time+" - "+sender+" - "+packageModel;
	}
}
