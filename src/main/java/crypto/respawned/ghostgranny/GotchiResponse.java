package crypto.respawned.ghostgranny;

import java.util.ArrayList;

public class GotchiResponse {

    private ArrayList<Gotchi> gotchisOwned;
    private String id; // address
    
    public GotchiResponse() {
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
