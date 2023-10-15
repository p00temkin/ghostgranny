package crypto.respawned.ghostgranny.objects;

import java.util.ArrayList;

public class GotchiOwnershipResponse {

    private ArrayList<Gotchi> gotchisOwned;
    private String id; // address
    
    public GotchiOwnershipResponse() {
        super();
    }

    public String getId() {
        return id;
    }

    public ArrayList<Gotchi> getGotchisOwned() {
        return gotchisOwned;
    }

    public void setGotchisOwned(ArrayList<Gotchi> gotchisOwned) {
        this.gotchisOwned = gotchisOwned;
    }

    public void setId(String id) {
        this.id = id;
    }

}
