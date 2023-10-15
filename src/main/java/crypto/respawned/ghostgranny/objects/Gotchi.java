package crypto.respawned.ghostgranny.objects;

public class Gotchi {

    private String id;
    private String name;
    private Integer kinship;
    private Integer lastInteracted;
    private Long timeUntilPet = Long.MAX_VALUE;
    
    public Gotchi() {
        super();
    }

    public Gotchi(String id, String name, Integer kinship, Integer lastInteracted) {
        super();
        this.id = id;
        this.name = name;
        this.kinship = kinship;
        this.lastInteracted = lastInteracted;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getKinship() {
        return kinship;
    }

    public void setKinship(Integer kinship) {
        this.kinship = kinship;
    }

    public Integer getLastInteracted() {
        return lastInteracted;
    }

    public void setLastInteracted(Integer lastInteracted) {
        this.lastInteracted = lastInteracted;
    }

	public Long getTimeUntilPet() {
		return timeUntilPet;
	}

	public void setTimeUntilPet(Long timeUntilPet) {
		this.timeUntilPet = timeUntilPet;
	}
    
}
