package crypto.respawned.ghostgranny;

import java.util.ArrayList;

import crypto.forestfish.utils.EVMUtils;
import crypto.forestfish.utils.SystemUtils;

public class GotchiSettings {

    // Specific
    private String petMethodID = "0x22c67519";
    private ArrayList<Integer> tokenIDs = new ArrayList<Integer>();
    private boolean haMode = false;
    private boolean forcepet = false;
    private int extraDelay = 0;
    
	// Generic
    private String aavegotchiContractAddress = "0x86935f11c86623dec8a25696e1c19a8659cbf95d";
    private String gasLimit = "200000";
    
    // Wallet
    private String theGraphQueryEndpointURI = "https://api.thegraph.com/subgraphs/name/aavegotchi/aavegotchi-core-matic";
    private String providerURL = "";
    private String walletAddress = "";
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

    public String getWalletAddress() {
        return walletAddress;
    }

    public void setWalletAddress(String walletAddress) {
        this.walletAddress = walletAddress;
    }

    public String getGasLimit() {
        return gasLimit;
    }

    public void setGasLimit(String gasLimit) {
        this.gasLimit = gasLimit;
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
	
	public boolean isForcepet() {
		return forcepet;
	}

	public void setForcepet(boolean forcepet) {
		this.forcepet = forcepet;
	}

	public int getExtraDelay() {
		return extraDelay;
	}

	public void setExtraDelay(int extraDelay) {
		this.extraDelay = extraDelay;
	}

	public void print() {
        System.out.println("Settings:");
        System.out.println(" - providerURL: " + this.getProviderURL());
        System.out.println(" - walletAddress: " + this.getWalletAddress());
        System.out.println(" - walletMnemonic: " + this.getWalletMnemonic());
        System.out.println(" - walletPrivkey: " + this.getWalletPrivKey());
        System.out.println(" - tokenIDs: " + this.getTokenIDs().toString());
        System.out.println(" - haMode: " + this.isHaMode());
        System.out.println(" - haMode: " + this.isHaMode());
        System.out.println(" - extradelay: " + this.getExtraDelay());
        
        System.out.println(" - aavegotchiContractAddress: " + this.getAavegotchiContractAddress());
        System.out.println(" - theGraphQueryEndpointURI: " + this.getTheGraphQueryEndpointURI());
        System.out.println(" - gasLimit: " + this.getGasLimit());
        System.out.println(" - petMethodID: " + this.getPetMethodID());
    }

    public void sanityCheck() {
        if (EVMUtils.isValidEthereumAddress(this.getAavegotchiContractAddress())) {
            if (!"".equals(this.getWalletAddress())) {
                if (!EVMUtils.isValidEthereumAddress(this.getWalletAddress().toLowerCase())) {
                    System.out.println(" - wallet address is not valid: " + this.getWalletAddress());
                    SystemUtils.halt();
                }
            }
            // all good
        } else {
            System.out.println(" - aavegotchiContractAddress is not valid: " + this.getAavegotchiContractAddress());
            SystemUtils.halt();
        }
    }
    
}
