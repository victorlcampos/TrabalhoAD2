package models;

import java.util.Set;
import java.util.TreeSet;

public class PackageModel implements Comparable<PackageModel> {
	private Long value;
	private Set<PackageModel> sackOption;

	public PackageModel(long l) {
		super();
		sackOption = new TreeSet<PackageModel>();
		this.value = l;
	}

	public Long getValue() {
		return value;
	}
	
	public Set<PackageModel> getSackOption() {
		return sackOption;
	}

	public void setSackOption(Set<PackageModel> sackOption) {
		this.sackOption = sackOption;
	}

	/**Compara o próximo pacote esperado (value) */
	@Override
	public boolean equals(Object obj) {
		return this.value.equals(((PackageModel) obj).getValue());
	}
	
	@Override
	public String toString() {
		return value.toString();
	}

	@Override
	public int compareTo(PackageModel o) {
		return value.compareTo(o.getValue());
	}	
}
