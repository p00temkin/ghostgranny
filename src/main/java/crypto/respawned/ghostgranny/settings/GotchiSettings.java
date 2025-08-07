package crypto.respawned.ghostgranny.settings;

import java.util.ArrayList;

import crypto.forestfish.utils.EVMUtils;
import crypto.forestfish.utils.SystemUtils;

public class GotchiSettings {

    // Aavegotchi Specific
    private String petMethodID = "0x22c67519"; // same ABI on polygon/base
    private ArrayList<Integer> tokenIDs = new ArrayList<Integer>();
    private boolean haMode = false;
    private int extraDelay = 0;
    private int thresholdForGotchiToCatchUpInSeconds = 2*3600;
	private int theGraphPollFrequencyInSeconds = 60;
	private int confirmTimeInSecondsBeforeRetry = 20;
	private int txRetryThreshold = 3;
	private boolean haltOnUnconfirmedTX = true;
	private boolean forcePetAll = false; // DEBUG purposes only
	
	private boolean sadNotification = false;
	private String sadURL = "";

	// Blockchain
    private String aavegotchiContractAddress = "0xA99c4B08201F2913Db8D28e71d020c4298F29dBF"; // AavegotchiGameFacet.sol, function interact(uint256[] calldata _tokenIds), polygon: 0x86935f11c86623dec8a25696e1c19a8659cbf95d
    private String providerURL = "";
    
    // TheGraph
    //private String theGraphQueryEndpointURI = "https://api.thegraph.com/subgraphs/name/aavegotchi/aavegotchi-core-matic";
    private String theGraphQueryEndpointURI = "https://subgraph.satsuma-prod.com/tWYl5n5y04oz/aavegotchi/aavegotchi-core-base/api"; // polygon: https://subgraph.satsuma-prod.com/tWYl5n5y04oz/aavegotchi/aavegotchi-core-matic/api
    
    // Gotchi owner wallet
    private String ownerWalletAddress = "";
    
    // GhostGranny wallet
    private String walletMnemonic = "N/A"; // only needed once to create the wallet if it does not exist
    private String walletPrivKey = "N/A"; // only needed once to create the wallet if it does not exist
    
    public GotchiSettings() {
        super();
    }

    public String getProviderURL() {
        return providerURL;
    }

    public void setProviderURL(String providerURL) {
        this.providerURL = providerURL;
    }

    public String getPetMethodID() {
        return petMethodID;
    }

    public void setPetMethodID(String petMethodID) {
        this.petMethodID = petMethodID;
    }

    public String getAavegotchiContractAddress() {
        return aavegotchiContractAddress;
    }

    public void setAavegotchiContractAddress(String aavegotchiContractAddress) {
        this.aavegotchiContractAddress = aavegotchiContractAddress;
    }

    public ArrayList<Integer> getTokenIDs() {
        return tokenIDs;
    }

    public void setTokenIDs(ArrayList<Integer> tokenIDs) {
        this.tokenIDs = tokenIDs;
    }

    public void addTokenID(int i) {
        this.tokenIDs.add(i);
    }

    public String getWalletMnemonic() {
        return walletMnemonic;
    }

    public void setWalletMnemonic(String walletMnemonic) {
        this.walletMnemonic = walletMnemonic;
    }

    public String getTheGraphQueryEndpointURI() {
        return theGraphQueryEndpointURI;
    }

    public void setTheGraphQueryEndpointURI(String theGraphQueryEndpointURI) {
        this.theGraphQueryEndpointURI = theGraphQueryEndpointURI;
    }

    public String getOwnerWalletAddress() {
		return ownerWalletAddress;
	}

	public void setOwnerWalletAddress(String ownerWalletAddress) {
		this.ownerWalletAddress = ownerWalletAddress;
	}

	public boolean isHaMode() {
        return haMode;
    }

    public void setHaMode(boolean haMode) {
        this.haMode = haMode;
    }

    public String getWalletPrivKey() {
		return walletPrivKey;
	}

	public void setWalletPrivKey(String walletPrivKey) {
		this.walletPrivKey = walletPrivKey;
	}

	public int getExtraDelay() {
		return extraDelay;
	}

	public void setExtraDelay(int extraDelay) {
		this.extraDelay = extraDelay;
	}
	
	public int getThresholdForGotchiToCatchUpInSeconds() {
		return thresholdForGotchiToCatchUpInSeconds;
	}

	public void setThresholdForGotchiToCatchUpInSeconds(int thresholdForGotchiToCatchUpInSeconds) {
		this.thresholdForGotchiToCatchUpInSeconds = thresholdForGotchiToCatchUpInSeconds;
	}

	public void print(String ghostGrannyPublicAddress) {
		System.out.println("");
        System.out.println("GhostGranny wallet settings: ");
        System.out.println(" - Public address: " + ghostGrannyPublicAddress);
        System.out.println(" - Local wallet exists: " + true);
        System.out.println("");
        
        System.out.println("Gotchi owner settings: ");
        System.out.println(" - Public address: " + this.getOwnerWalletAddress());
        System.out.println("");
        
        System.out.println("Misc settings: ");
        if (!this.getTokenIDs().isEmpty()) System.out.println(" - tokenIDs override: " + this.getTokenIDs().toString());
        System.out.println(" - haMode: " + this.isHaMode());
        System.out.println(" - extradelay: " + this.getExtraDelay());
        if (!"".equals(this.getProviderURL())) System.out.println(" - providerURL override: " + this.getProviderURL());
        System.out.println(" - aavegotchiContractAddress: " + this.getAavegotchiContractAddress());
        System.out.println(" - theGraphQueryEndpointURI: " + this.getTheGraphQueryEndpointURI());
        System.out.println(" - thresholdForGotchiToCatchUpInSeconds: " + this.getThresholdForGotchiToCatchUpInSeconds());
        System.out.println(" - petMethodID: " + this.getPetMethodID());
        System.out.println(" - theGraphPollFrequencyInSeconds: " + theGraphPollFrequencyInSeconds);
        System.out.println(" - sadURL: " + this.getSadURL());
        System.out.println(" - sadNotification: " + this.isSadNotification());
        System.out.println("");
    }

    public void sanityCheck(String ghostGrannyPublicAddress) {
        if (EVMUtils.isValidEthereumAddress(this.getAavegotchiContractAddress())) {
            if (!"".equals(this.getOwnerWalletAddress())) {
                if (!EVMUtils.isValidEthereumAddress(this.getOwnerWalletAddress().toLowerCase())) {
                    System.out.println(" - owner wallet address is not valid: " + this.getOwnerWalletAddress());
                    SystemUtils.halt();
                }
            }
            // all good
        } else {
            System.out.println(" - aavegotchiContractAddress is not valid: " + this.getAavegotchiContractAddress());
            print(ghostGrannyPublicAddress);
            SystemUtils.halt();
        }
        
    }

	public int getTheGraphPollFrequencyInSeconds() {
		return theGraphPollFrequencyInSeconds;
	}

	public void setTheGraphPollFrequencyInSeconds(int theGraphPollFrequencyInSeconds) {
		this.theGraphPollFrequencyInSeconds = theGraphPollFrequencyInSeconds;
	}

	public int getConfirmTimeInSecondsBeforeRetry() {
		return confirmTimeInSecondsBeforeRetry;
	}

	public void setConfirmTimeInSecondsBeforeRetry(int confirmTimeInSecondsBeforeRetry) {
		this.confirmTimeInSecondsBeforeRetry = confirmTimeInSecondsBeforeRetry;
	}

	public int getTxRetryThreshold() {
		return txRetryThreshold;
	}

	public void setTxRetryThreshold(int txRetryThreshold) {
		this.txRetryThreshold = txRetryThreshold;
	}

	public boolean isHaltOnUnconfirmedTX() {
		return haltOnUnconfirmedTX;
	}

	public void setHaltOnUnconfirmedTX(boolean haltOnUnconfirmedTX) {
		this.haltOnUnconfirmedTX = haltOnUnconfirmedTX;
	}

	public boolean isForcePetAll() {
		return forcePetAll;
	}

	public void setForcePetAll(boolean forcePetAll) {
		this.forcePetAll = forcePetAll;
	}

	public boolean isSadNotification() {
		return sadNotification;
	}

	public void setSadNotification(boolean sadNotification) {
		this.sadNotification = sadNotification;
	}

	public String getSadURL() {
		return sadURL;
	}

	public void setSadURL(String sadURL) {
		this.sadURL = sadURL;
	}

    
}
