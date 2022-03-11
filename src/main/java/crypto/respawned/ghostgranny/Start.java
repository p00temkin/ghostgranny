package crypto.respawned.ghostgranny;

import java.util.*;  
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import com.netflix.graphql.dgs.client.GraphQLResponse;
import crypto.forestfish.enums.WalletOrigin;
import crypto.forestfish.objects.evm.EVMBlockChain;
import crypto.forestfish.objects.evm.EVMLocalWallet;
import crypto.forestfish.objects.evm.EVMWalletBalance;
import crypto.forestfish.utils.EVMUtils;
import crypto.forestfish.utils.FormatUtils;
import crypto.forestfish.utils.GraphQLUtils;
import crypto.forestfish.utils.NumUtils;
import crypto.forestfish.utils.SystemUtils;
import reactor.core.publisher.Mono;

public class Start {

	private static final Logger LOGGER = LoggerFactory.getLogger(Start.class);

	public static void main(String[] args) {
		LOGGER.info("ghostGranny init()");

		boolean debug = false;
		int theGraphPollFrequencyInSeconds = 60;
		int confirmTimeInSecondsBeforeRetry = 20;
		int txRetryThreshold = 3;

		// Initialize pet settings
		GotchiSettings settings = null;
		if (debug) {
			settings = new GotchiSettings();
			settings.setWalletAddress("0xxxxxxxx");
			settings.setProviderURL("https://polygon-mainnet.infura.io/v3/xxxxxxx");
		} else {
			settings = parseCliArgs(args);
			settings.sanityCheck();
		}

		HashMap<Integer, Integer> kinshipTracker = new HashMap<>();
		HashMap<Integer, Gotchi> gotchiTracker = new HashMap<>();

		/**
		 *  Initialize connection to MATIC network
		 */
		EVMBlockChain maticBlockChain = new EVMBlockChain("Matic/Polygon", "MATIC", 137, settings.getProviderURL(), "https://polygonscan.com/");
		Web3j maticWeb3j = Web3j.build(new HttpService(maticBlockChain.getNodeURL()));

		// Wallet setup + make sure MATIC balance is above 0
		EVMLocalWallet maticWallet = null;
		if (!"N/A".equals(settings.getWalletMnemonic())) maticWallet = new EVMLocalWallet("maticwallet", WalletOrigin.RECOVERY_MNEMONIC, "nopassword", settings.getWalletMnemonic());
		if (!"N/A".equals(settings.getWalletPrivKey())) maticWallet = new EVMLocalWallet("maticwallet", WalletOrigin.PRIVATEKEY, "nopassword", settings.getWalletPrivKey());
		if (null == maticWallet) maticWallet = new EVMLocalWallet("maticwallet", WalletOrigin.EXISTING_LOCALWALLETFILE, "nopassword", settings.getWalletMnemonic());
		
		EVMWalletBalance walletBalance = EVMUtils.getWalletBalanceMain(maticWeb3j, maticBlockChain, maticWallet);
		if (walletBalance.getBalanceInWEI().intValue() == 0) {
			LOGGER.error("wallet " + maticWallet.getCredentials().getAddress() + " has no funds! We gotta spend to pet.");
			SystemUtils.halt();
		}
		LOGGER.info("Ready to move with granny wallet " + maticWallet.getCredentials().getAddress());

		while (true) {

			/**
			 *  Poll the graph until we know all owned/specified gotchis needs a pet
			 */
			boolean allGotchisNeedsLove = false;
			int allGotchisNeedsLoveCounter = 0;
			while (!allGotchisNeedsLove) {

				// lets start by assuming all are in need
				allGotchisNeedsLove = true;

				// https://thegraph.com/hosted-service/subgraph/aavegotchi/aavegotchi-core-matic
				String graphqlQuery = "{"
						+ "user(id:\"" + settings.getWalletAddress().toLowerCase() + "\") {gotchisOwned{id,name,kinship,lastInteracted},id}"
								+ "}";
				String url = settings.getTheGraphQueryEndpointURI();
				HashMap<String, Object> queryArgs = new HashMap<>();

				Mono<GraphQLResponse> graphQLResponse = GraphQLUtils.executeQuery(url, graphqlQuery, queryArgs, 180, 10); // retry for 30 minutes
				if (null == graphQLResponse) {
					LOGGER.error("Unable to get a valid response from the GraphQL query towards " + url);
					SystemUtils.halt();
				}

				LOGGER.debug("graphQLResponse: " + graphQLResponse.block().toString());

				ArrayList<Integer> tokenIDsFromTheGraph = new ArrayList<Integer>();
				GotchiResponse gr = graphQLResponse.map(r -> r.extractValueAsObject("user", GotchiResponse.class)).block();
				Long minTimeUntilPet = 9999999L;
				int gotchisNeedHugCount = 0;
				int gotchisAlreadyHuggedCount = 0;

				if (gr.getGotchisOwned().isEmpty()) {
					LOGGER.warn("You aint got no gotchis for grandma to pet! Find a waifu and get some at https://aavegotchi.com/baazaar/aavegotchis?sort=latest");
					LOGGER.warn("Make me proud and allow me to pet a cute gotchi dear. H1 gotchis are prettier you know ..");
				} else {

					for (Gotchi gotchi: gr.getGotchisOwned()) {

							int gotchiID = Integer.parseInt("" + gotchi.getId());
							tokenIDsFromTheGraph.add(gotchiID);

							Integer previousKinship = kinshipTracker.get(gotchiID);
							if (null == previousKinship) {
								previousKinship = gotchi.getKinship();
							}
							if (!previousKinship.equals(gotchi.getKinship())) {
								if (settings.isHaMode()) {
									LOGGER.info("The kinship got updated without grannys touch..");
								} else {
									LOGGER.warn("Strange, the kinship got updated without grannys touch.. enable haMode if you want this warning to go away");
									LOGGER.warn(gotchi.getName() + " previousKinship: " + previousKinship + " gotchi.getKinship(): " + gotchi.getKinship());
								}
							}

							kinshipTracker.put(gotchiID, gotchi.getKinship());
							gotchiTracker.put(gotchiID, gotchi);

							Long diffInSeconds = System.currentTimeMillis()/1000L - Long.parseLong("" + gotchi.getLastInteracted());
							Long timeUntilPet = (12L*3600L) - diffInSeconds;
							if (minTimeUntilPet > timeUntilPet) minTimeUntilPet = timeUntilPet;
							LOGGER.info("gotchi " + gotchi.getName() + " has kinship=" + gotchi.getKinship() + ", and needs love in " + timeUntilPet + " seconds");

							if (timeUntilPet < 0) {
								LOGGER.info("The gotchi " + gotchi.getName() + " is ready for a hug!");
								gotchisNeedHugCount++;
							} else {
								if (settings.isForcepet()) {
									LOGGER.info("The gotchi " + gotchi.getName() + " is getting FORCED hugged");
									gotchisNeedHugCount++;
								} else {
									allGotchisNeedsLove = false;
									gotchisAlreadyHuggedCount++;
								}

							}
					}
					settings.setTokenIDs(tokenIDsFromTheGraph);

					// If a single/few gotchi(s) trail the rest, let them catchup and hug the majority
					// TODO: reconsider if the minority gotchi has epic kinship?
					if (!allGotchisNeedsLove && (gotchisNeedHugCount >= gotchisAlreadyHuggedCount)) {
						if (settings.getExtraDelay() == 0) {
							allGotchisNeedsLove = true;
							allGotchisNeedsLoveCounter++;
						} else {
							if (allGotchisNeedsLoveCounter>1) {
								allGotchisNeedsLove = true;
							} else {
								LOGGER.info("All gotchi need love, but im holdin that love for an additional " + settings.getExtraDelay() + " seconds");
								SystemUtils.sleepInSeconds(settings.getExtraDelay());
							}
							allGotchisNeedsLoveCounter++;
						}
					}

					if ( (minTimeUntilPet <= 60L) || settings.isForcepet()) {
						LOGGER.info(".... micro sleeping 5 seconds");
						SystemUtils.sleepInSeconds(5);
					} else {
						LOGGER.info(".... sleeping " + theGraphPollFrequencyInSeconds + " seconds");
						SystemUtils.sleepInSeconds(theGraphPollFrequencyInSeconds);
					}

				}
			}
			
			int randSleep = NumUtils.randomNumWithinRangeAsInt(5, 20);
			LOGGER.info("In " + randSleep + " seconds granny will cuddle with the following gotchis: " + settings.getTokenIDs().toString());
			SystemUtils.sleepInSeconds(randSleep);

			/**
			 * Prepare the request hex data
			 */
			// gotchi pet request (Function: interact(uint256[] _tokenIds) ***)
			String petRequest_hexData = settings.getPetMethodID()                           // methodID for PET action (default 0x22c67519)
					+ FormatUtils.makeUINT256WithDec2Hex(32)                                // uint256 param1, 32 = 0x20
					+ FormatUtils.makeUINT256WithDec2Hex(settings.getTokenIDs().size());    // uint256 param2, 5 = 0x5 (nr of gotchis to pet)
			for (Integer tokenID: settings.getTokenIDs()) {
				petRequest_hexData = petRequest_hexData + FormatUtils.makeUINT256WithDec2Hex(tokenID);      // uint256 param3, tokenID 5972 = 0x1754
			}
			
			/**
			 *  Perform the pet action
			 */
			boolean txAttemptsCompleted = EVMUtils.makeRequest(petRequest_hexData, txRetryThreshold, confirmTimeInSecondsBeforeRetry, maticWeb3j, maticBlockChain, maticWallet, settings.getAavegotchiContractAddress(), settings.getGasLimit());
			
			/**
			 * Check the graph again for kinship increase
			 */
			boolean grannyTriedSleepin = false;
			if (txAttemptsCompleted) {
				String graphqlQuery = "{user(id:\"" + settings.getWalletAddress().toLowerCase() + "\") {gotchisOwned{id,name,kinship,lastInteracted},id}}";
				String url = settings.getTheGraphQueryEndpointURI();
				HashMap<String, Object> queryArgs = new HashMap<>();

				int kinshipBumpRetryCounter = 0;
				boolean kinshipBumpVerified = false;
				while (!kinshipBumpVerified) {
					Mono<GraphQLResponse> graphQLResponse = GraphQLUtils.executeQuery(url, graphqlQuery, queryArgs, 180, 10); // retry for 30 minutes
					if (null == graphQLResponse) {
						LOGGER.error("Unable to get a valid response from the GraphQL query towards " + url);
						SystemUtils.halt();
					}

					LOGGER.debug("graphQLResponse: " + graphQLResponse.block().toString());

					GotchiResponse gr = graphQLResponse.map(r -> r.extractValueAsObject("user", GotchiResponse.class)).block();

					int gotchisBumped = 0;
					int gotchisNotBumped = 0;
					if (gr.getGotchisOwned().isEmpty()) {
						LOGGER.error("We started out with baby gotchies and now they are gone??!!");
						SystemUtils.halt();
					} else {

						// Finally verify kinship has bumped for all gotchis
						for (Gotchi gotchi: gr.getGotchisOwned()) {

							Integer gotchiID = Integer.parseInt("" + gotchi.getId());
							Integer previousKinship = kinshipTracker.get(gotchiID);
							System.out.println("gotchi " + gotchi.getName() + " kinship " + previousKinship + " -> " + gotchi.getKinship());

							if (previousKinship.equals(gotchi.getKinship())) {
								gotchisNotBumped++;
							} else {
								gotchisBumped++;
							}

						}

						if (gotchisBumped > gotchisNotBumped) {
							LOGGER.info("The majority of gotchis has higher kinship, granny needs to rest for 10 seconds ..");
							SystemUtils.sleepInSeconds(10);
							kinshipBumpVerified = true;
						} else {

							if (kinshipBumpRetryCounter > 200) {
								if (!txAttemptsCompleted) {
									LOGGER.warn("We were not sure if the transaction went through, and now seems it didnt since kinship is not bumped. Granny needs help.");
									LOGGER.warn("Granny still needs to keep trying though .. loopin");
								} else {
									if (grannyTriedSleepin) {
										LOGGER.error("So transaction went through ... but the graph does not reflect our love and we checked " + kinshipBumpRetryCounter + " times. Subgraph might be out of sync. Grannys goin to bed for 60 minutes.");
										grannyTriedSleepin = true;
										SystemUtils.sleepInSeconds(3600);
									} else {
										LOGGER.error("So transaction went through ... but the graph does not reflect our love and we checked " + kinshipBumpRetryCounter + " times. Subgraph might be out of sync. Grannys goin to bed .. giving up until cpr.");
										SystemUtils.halt();
									}
								}
							} else {
								LOGGER.warn("The bump didnt work?? .. perhaps we just need to wait for the graph to sync .. retrying in 10 seconds");
								kinshipBumpRetryCounter++;
								SystemUtils.sleepInSeconds(10);
							}
						}
					}
				}
			}
		}
	}


	private static GotchiSettings parseCliArgs(String[] args) {

		GotchiSettings settings = new GotchiSettings();
		Options options = new Options();

		// pet function address
		Option petfuncaddr = new Option("f", "petmethodid", true, "Aavegotchi pet method id/function address (default: 0x22c67519)");
		options.addOption(petfuncaddr);

		// gotchi token id(s), used unless wallet specified
		Option tokenids = new Option("t", "tokenids", true, "csv decimal list of gotchi token ids (used unless wallet specified)");
		options.addOption(tokenids);

		// MATIC/Polygon provider URL
		Option providerURL = new Option("p", "providerurl", true, "MATIC/Polygon Provider URL (infura etc)");
		providerURL.setRequired(true);
		options.addOption(providerURL);

		// wallet address
		Option walletAdress = new Option("w", "wallet", true, "Wallet address");
		options.addOption(walletAdress);

		// wallet mnemonic
		Option walletMnemonic = new Option("m", "walletmnemonic", true, "Wallet mnemonic");
		options.addOption(walletMnemonic);
		
		// wallet mnemonic
		Option walletPrivatekey = new Option("k", "walletprivkey", true, "Wallet private key");
		options.addOption(walletPrivatekey);

		// gas limit
		Option gasLimit = new Option("l", "gaslimit", true, "gaslimit");
		options.addOption(gasLimit);

		// hamode
		Option haMode = new Option("x", "hamode", false, "High Availability mode (removes warning messages caused by running multiple grannies)");
		options.addOption(haMode);
		
		// forcepet
		Option forcepet = new Option("o", "forcepet", false, "Override and force pet gotchis");
		options.addOption(forcepet);

		// extradelay
		Option extradelay = new Option("e", "extradelay", true, "Extra delay in seconds");
		options.addOption(extradelay);
		
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);
			if (cmd.hasOption("f")) settings.setPetMethodID(cmd.getOptionValue("petfuncaddr"));
			if (cmd.hasOption("p")) settings.setProviderURL(cmd.getOptionValue("providerurl"));
			if (cmd.hasOption("m")) settings.setWalletMnemonic(cmd.getOptionValue("walletmnemonic"));
			if (cmd.hasOption("k")) settings.setWalletPrivKey(cmd.getOptionValue("walletprivkey"));
			if (cmd.hasOption("w")) settings.setWalletAddress(cmd.getOptionValue("wallet"));
			if (cmd.hasOption("g")) settings.setGasLimit(cmd.getOptionValue("gaslimit"));
			if (cmd.hasOption("x")) settings.setHaMode(true);
			if (cmd.hasOption("o")) settings.setForcepet(true);
			if (cmd.hasOption("e")) settings.setExtraDelay(Integer.parseInt(cmd.getOptionValue("extradelay")));

			if (cmd.hasOption("t")) {
				for (String tokenID: cmd.getOptionValue("tokenids").split(",")) {
					settings.addTokenID(Integer.parseInt(tokenID));
				}
			}

			if (!cmd.hasOption("t") && !cmd.hasOption("w") && !cmd.hasOption("m") && !cmd.hasOption("k")) {
				LOGGER.error("You must specify either -t, -w, -k or -m to make granny take action");
				SystemUtils.halt();
			}

			settings.print();

		} catch (ParseException e) {
			LOGGER.error("ParseException: " + e.getMessage());
			formatter.printHelp(" ", options);
			SystemUtils.halt();
		}

		return settings;
	}

}
