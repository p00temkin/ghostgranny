package crypto.respawned.ghostgranny.objects;

import java.util.ArrayList;
import java.util.HashMap;

public class GotchiGraphQLState {

	private GotchiOwnershipResponse gotchiOwnershipResponse = new GotchiOwnershipResponse();
	private ArrayList<Integer> tokenIDsFromTheGraphToHug = new ArrayList<Integer>();
	private HashMap<Integer, Integer> kinshipTracker = new HashMap<>();
	private HashMap<Integer, Gotchi> gotchiTracker = new HashMap<>();
	
	public GotchiGraphQLState() {
		super();
	}

	public GotchiGraphQLState(GotchiOwnershipResponse _gotchiOwnershipResponse, ArrayList<Integer> _tokenIDsFromTheGraphToHug, HashMap<Integer, Integer> _kinshipTracker, HashMap<Integer, Gotchi> _gotchiTracker) {
		super();
		this.gotchiOwnershipResponse = _gotchiOwnershipResponse; 
		this.tokenIDsFromTheGraphToHug = _tokenIDsFromTheGraphToHug;
		this.kinshipTracker = _kinshipTracker;
		this.gotchiTracker = _gotchiTracker;
	}

	public ArrayList<Integer> getTokenIDsFromTheGraphToHug() {
		return tokenIDsFromTheGraphToHug;
	}

	public void setTokenIDsFromTheGraphToHug(ArrayList<Integer> tokenIDsFromTheGraphToHug) {
		this.tokenIDsFromTheGraphToHug = tokenIDsFromTheGraphToHug;
	}

	public HashMap<Integer, Integer> getKinshipTracker() {
		return kinshipTracker;
	}

	public void setKinshipTracker(HashMap<Integer, Integer> kinshipTracker) {
		this.kinshipTracker = kinshipTracker;
	}

	public HashMap<Integer, Gotchi> getGotchiTracker() {
		return gotchiTracker;
	}

	public void setGotchiTracker(HashMap<Integer, Gotchi> gotchiTracker) {
		this.gotchiTracker = gotchiTracker;
	}

	public GotchiOwnershipResponse getGotchiOwnershipResponse() {
		return gotchiOwnershipResponse;
	}

	public void setGotchiOwnershipResponse(GotchiOwnershipResponse gotchiOwnershipResponse) {
		this.gotchiOwnershipResponse = gotchiOwnershipResponse;
	}
	
}
