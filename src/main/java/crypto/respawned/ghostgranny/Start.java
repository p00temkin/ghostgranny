package crypto.respawned.ghostgranny;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esaulpaugh.headlong.abi.Single;
import com.esaulpaugh.headlong.abi.Tuple;

import crypto.forestfish.enums.evm.EVMChain;
import crypto.forestfish.objects.embedded.evm.ABI;
import crypto.forestfish.objects.evm.EVMLocalWallet;
import crypto.forestfish.objects.evm.connector.EVMBlockChainConnector;
import crypto.forestfish.utils.CryptUtils;
import crypto.forestfish.utils.EVMUtils;
import crypto.forestfish.utils.FormatUtils;
import crypto.forestfish.utils.JSONUtils;
import crypto.forestfish.utils.NumUtils;
import crypto.forestfish.utils.SystemUtils;
import crypto.respawned.ghostgranny.objects.Gotchi;
import crypto.respawned.ghostgranny.objects.GotchiGraphQLState;
import crypto.respawned.ghostgranny.settings.GotchiSettings;
import crypto.respawned.ghostgranny.utils.GrannyUtils;

public class Start {

	private static final Logger LOGGER = LoggerFactory.getLogger(Start.class);

	public static void main(String[] args) {
		LOGGER.info("ghostGranny init()");

		/**
		 * Settings init
		 */
		GotchiSettings settings = null;
		settings = parseCliArgs(args);

		/**
		 *  Initialize granny wallet
		 */
		EVMLocalWallet grannywallet = EVMUtils.initializeWallet("grannywallet", settings.getWalletMnemonic(), settings.getWalletPrivKey());
		settings.sanityCheck(grannywallet.getCredentials().getAddress());
		settings.print(grannywallet.getCredentials().getAddress());

		/**
		 *  Initialize connection to POLYGON network
		 */
		EVMBlockChainConnector connector = null;
		if (settings.getProviderURL().startsWith("htt")) {
			connector = new EVMBlockChainConnector(EVMChain.BASE, settings.getProviderURL(), true);
		} else {
			connector = new EVMBlockChainConnector(EVMChain.BASE, true);
		}
		
		/**
		 *  Make sure granny wallet has funds
		 */
		EVMUtils.ensureWalletHasFunds(connector, grannywallet, 0.0001d);
		LOGGER.info("Ready to move with granny wallet " + grannywallet.getCredentials().getAddress());

		/**
		 * Initial nap
		 */
		if (settings.getExtraDelay() != 0) {
			LOGGER.info("Napping an extra " + settings.getExtraDelay() + " seconds");
			SystemUtils.sleepInSeconds(settings.getExtraDelay());
		}

		/**
		 * Infinite pet loop init
		 */
		boolean firstAttempt = true;
		int tx_buy_still_fail_counter = 0;
		while (true) {

			/**
			 * Block loop to get tokenids of hugs to give
			 */
			GotchiGraphQLState gotchiGraphQLStatePRE = GrannyUtils.blockUntilGotchisToHug(settings, firstAttempt);
			settings.setTokenIDs(gotchiGraphQLStatePRE.getTokenIDsFromTheGraphToHug());
			int randSleep = NumUtils.randomNumWithinRangeAsInt(5, 20);
			LOGGER.info("In " + randSleep + " seconds granny will cuddle with the following gotchis: " + settings.getTokenIDs().toString());
			SystemUtils.sleepInSeconds(randSleep);

			/**
			 * Prepare the request hex data
			 * - gotchi pet request (Function: interact(uint256[] _tokenIds) ***)
			 */
			String petRequest_hexData = "";

			// Manual method
			String petRequest_hexDataMAN = settings.getPetMethodID()                        // methodID for PET action (default 0x22c67519)
					+ FormatUtils.makeUINT256WithDec2Hex(32)                                // uint256 param1, 32 = 0x20
					+ FormatUtils.makeUINT256WithDec2Hex(settings.getTokenIDs().size());    // uint256 param2, 5 = 0x5 (nr of gotchis to pet)
			for (Integer tokenID: settings.getTokenIDs()) {
				petRequest_hexDataMAN = petRequest_hexDataMAN + FormatUtils.makeUINT256WithDec2Hex(tokenID);      // uint256 param3, tokenID 5972 = 0x1754
			}

			// ABI method
			String funcName = "interact";
			String abiAavegotchiDiamond = ABI.abiAavegotchiDiamond;
			String funcJSON = JSONUtils.getFunctionJSONUsingABI(abiAavegotchiDiamond, funcName);
			if ("".equals(funcJSON)) LOGGER.error("Unable to find function " + funcName + "in ABI JSON");
			com.esaulpaugh.headlong.abi.Function func = com.esaulpaugh.headlong.abi.Function.fromJson(funcJSON);
			BigInteger[] bigints = new BigInteger[settings.getTokenIDs().size()];
			int tokenoffset = 0;
			for (Integer tokenID: settings.getTokenIDs()) {
				bigints[tokenoffset] = BigInteger.valueOf(tokenID);
				tokenoffset++;
			}
			Tuple function_args = Single.of(bigints);
			ByteBuffer bb = func.encodeCall(function_args);
			String petRequest_hexDataABI = "0x" + CryptUtils.encodeHexString(bb.array());


			if (!petRequest_hexDataABI.equals(petRequest_hexDataMAN)) {
				LOGGER.warn("Function parameter calculation mismatch, a custom pet method ID might have been called, using the manual hexdata");
				LOGGER.warn("petRequest_hexDataMAN: " + petRequest_hexDataMAN);
				LOGGER.warn("petRequest_hexDataABI: " + petRequest_hexDataABI);
				petRequest_hexData = petRequest_hexDataMAN;
			} else {
				petRequest_hexData = petRequest_hexDataABI;
			}
			
			/**
			 * Roughly adjust gaslimit to your gotchi count (varies due to multiple factors, rented/owned etc)
			 */
			Integer gasLimit = Integer.parseInt(connector.getChaininfo().getFallbackGasLimitInUnits());
			Integer extraGas = 2000 * NumUtils.getNearestMultipleOf10(settings.getTokenIDs().size());
			if (extraGas > 0) {
				gasLimit = gasLimit + extraGas;
				LOGGER.info("We have a bunch of gotchis so bumping the gaslimit with an extra " + extraGas);
			}
			LOGGER.info("Moving forward with gasLimit " + gasLimit);
			
			/**
			 *  Perform the hug (pet interact() call)
			 */
			System.out.println("petRequest_hexData: " + petRequest_hexData);
			String txHASH = EVMUtils.makeSignedRequest(petRequest_hexData, settings.getTxRetryThreshold(), settings.getConfirmTimeInSecondsBeforeRetry(), connector, grannywallet.getCredentials(), settings.getAavegotchiContractAddress(), gasLimit.toString(), settings.isHaltOnUnconfirmedTX());
			
			/**
			 * Check the graph again for kinship increase
			 */
			LOGGER.info("Lets wait 20 seconds for theGraph to get updated..");
			SystemUtils.sleepInSeconds(20);
			int gotchisBumped = 0;
			if (null != txHASH) {
				GotchiGraphQLState gotchiGraphQLStatePOST = GrannyUtils.getGotchiState(settings, firstAttempt);

				// Finally verify kinship has bumped for at least 1 gotchi
				for (Gotchi gotchi: gotchiGraphQLStatePOST.getGotchiOwnershipResponse().getGotchisOwned()) {

					Integer gotchiID = Integer.parseInt("" + gotchi.getId());
					Integer previousKinship = gotchiGraphQLStatePRE.getKinshipTracker().get(gotchiID);
					Integer newKinship = gotchiGraphQLStatePOST.getKinshipTracker().get(gotchiID);
					LOGGER.info("gotchi " + gotchi.getName() + " kinship " + previousKinship + " -> " + newKinship);

					if (!previousKinship.equals(newKinship)) {
						gotchisBumped++;
					}
				}
				if (gotchisBumped == 0) tx_buy_still_fail_counter++;
			}
			
			// Sanity check
			if (gotchisBumped != settings.getTokenIDs().size()) {
				LOGGER.warn("Mismatch on bumped kinship.");
				if (settings.isHaMode()) {
					LOGGER.warn("May be caused by multiple ghostgrannys .. will keep going after a quick 30s nap");
					SystemUtils.sleepInSeconds(30);
				} else {
					LOGGER.warn("gotchisBumped: " + gotchisBumped);
					LOGGER.warn("settings.getTokenIDs().size(): " + settings.getTokenIDs().size());
					SystemUtils.halt();
				}
			}
			
			// Debug early exit
			if (settings.isForcePetAll()) {
				LOGGER.warn("We just performed a forcedpet so grinding to a halt to protect our wallet");
				SystemUtils.halt();
			}
			
			// Infinite TX Loop protection
			if (tx_buy_still_fail_counter > 10) {
				LOGGER.error("We have made 10 successful tx but the gotchis doesnt seem to get pet. Are you allowed to pet??!");
				SystemUtils.halt();
			}
			
			firstAttempt = false;
		}
	}


	private static GotchiSettings parseCliArgs(String[] args) {

		GotchiSettings settings = new GotchiSettings();
		Options options = new Options();

		// ghostgranny mnemonic
		Option walletMnemonic = new Option("m", "ggmnemonic", true, "Create ghostGranny account using mnemonic");
		options.addOption(walletMnemonic);

		// ghostgranny privkey
		Option walletPrivatekey = new Option("k", "ggprivkey", true, "Create ghostGranny account using private key");
		options.addOption(walletPrivatekey);

		// gotchi owner account address
		Option walletAdress = new Option("g", "gotchiowner", true, "Gotchi owner account address");
		options.addOption(walletAdress);

		// pet function address
		Option petfuncaddr = new Option("i", "petmethodid", true, "Aavegotchi interact() method id (default: 0x22c67519)");
		options.addOption(petfuncaddr);

		// MATIC/Polygon provider URL
		Option providerURL = new Option("p", "providerurl", true, "MATIC/Polygon Provider URL (infura etc)");
		options.addOption(providerURL);

		// hamode
		Option haMode = new Option("x", "hamode", false, "High Availability mode (removes warning messages caused by running multiple grannies)");
		options.addOption(haMode);

		// forcepet
		Option forcepet = new Option("o", "forcepet", false, "Override and force pet gotchis");
		options.addOption(forcepet);

		// extradelay
		Option extradelay = new Option("e", "extradelay", true, "Extra delay in seconds");
		options.addOption(extradelay);

		// gotchicatchupthreshold
		Option gotchicatchupthreshold = new Option("c", "gotchicatchupthreshold", true, "Gotchi catchup threshold in seconds");
		options.addOption(gotchicatchupthreshold);

		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd;
		try {
			cmd = parser.parse(options, args);

			if (cmd.hasOption("m")) settings.setWalletMnemonic(cmd.getOptionValue("ggmnemonic"));
			if (cmd.hasOption("k")) settings.setWalletPrivKey(cmd.getOptionValue("ggprivkey"));
			if (cmd.hasOption("g")) settings.setOwnerWalletAddress(cmd.getOptionValue("gotchiowner"));
			if (cmd.hasOption("i")) settings.setPetMethodID(cmd.getOptionValue("petfuncaddr"));
			if (cmd.hasOption("p")) settings.setProviderURL(cmd.getOptionValue("providerurl"));
			if (cmd.hasOption("x")) settings.setHaMode(true);
			if (cmd.hasOption("e")) settings.setExtraDelay(Integer.parseInt(cmd.getOptionValue("extradelay")));
			if (cmd.hasOption("c")) settings.setThresholdForGotchiToCatchUpInSeconds(Integer.parseInt(cmd.getOptionValue("gotchicatchupthreshold")));

			if (!cmd.hasOption("g") && !cmd.hasOption("m") && !cmd.hasOption("k")) {
				LOGGER.error("You must specify either -g, -k or -m to make granny take action");
				formatter.printHelp(" ", options);
				SystemUtils.halt();
			}

			if (settings.isHaMode()) {
				// race condition when running multiple grandma's can give us 'replacement transaction underpriced' naturally
				settings.setHaltOnUnconfirmedTX(false);
			}

		} catch (ParseException e) {
			LOGGER.error("ParseException: " + e.getMessage());
			formatter.printHelp(" ", options);
			SystemUtils.halt();
		}

		return settings;
	}

}
