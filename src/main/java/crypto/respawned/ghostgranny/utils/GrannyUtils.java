package crypto.respawned.ghostgranny.utils;

import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.graphql.dgs.client.GraphQLResponse;

import crypto.forestfish.utils.DateUtils;
import crypto.forestfish.utils.GraphQLUtils;
import crypto.forestfish.utils.SADUtils;
import crypto.forestfish.utils.StringsUtils;
import crypto.forestfish.utils.SystemUtils;
import crypto.respawned.ghostgranny.objects.Gotchi;
import crypto.respawned.ghostgranny.objects.GotchiGraphQLState;
import crypto.respawned.ghostgranny.objects.GotchiOwnershipResponse;
import crypto.respawned.ghostgranny.settings.GotchiSettings;
import reactor.core.publisher.Mono;

public class GrannyUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(GrannyUtils.class);



	public static GotchiOwnershipResponse getTheGraphGotchiInfo(GotchiSettings settings, boolean firstAttempt) {

		GotchiOwnershipResponse gr = new GotchiOwnershipResponse();

		// https://thegraph.com/hosted-service/subgraph/aavegotchi/aavegotchi-core-matic
		String graphqlQuery = "{"
				+ "user(id:\"" + settings.getOwnerWalletAddress().toLowerCase() + "\") {gotchisOwned{id,name,kinship,lastInteracted},id}"
				+ "}";
		String url = settings.getTheGraphQueryEndpointURI();
		HashMap<String, Object> queryArgs = new HashMap<>();

		LOGGER.info("firstAttempt: " + firstAttempt);
		int retryLimit = 180;
		if (firstAttempt) retryLimit = Integer.MAX_VALUE;
		Mono<GraphQLResponse> graphQLResponse = GraphQLUtils.executeQuery(url, graphqlQuery, queryArgs, retryLimit, 10); // retry for 30 minutes, infinite if first launch
		if (null == graphQLResponse) {
			LOGGER.error("Unable to get a valid response from the GraphQL query towards " + url);
			SystemUtils.halt();
		}

		if (graphQLResponse.block().getJson().toString().contains("null")) {
			LOGGER.warn("You aint got no gotchis for grandma to pet! Find a waifu and get some at https://aavegotchi.com/baazaar/aavegotchis?sort=latest");
			LOGGER.warn("Make me proud and allow me to pet a cute gotchi dear. H1 gotchis are prettier you know ..");
			SystemUtils.sleepInSeconds(60);
		} else {
			gr = graphQLResponse.map(r -> r.extractValueAsObject("user", GotchiOwnershipResponse.class)).block();
			return gr;
		}

		return null;
	}

	public static String createGotchiLogSummary(Gotchi gotchi) {
		String logSTR = StringsUtils.cutAndPadStringToN("Seconds left until hug: " + gotchi.getTimeUntilPet(), 32) + 
				StringsUtils.cutAndPadStringToN("gotchi=" + gotchi.getName(), 30) + 
				StringsUtils.cutAndPadStringToN("kinship=" + gotchi.getKinship(), 30);
		return logSTR;
	}

	public static GotchiGraphQLState blockUntilGotchisToHug(final GotchiSettings settings, boolean firstAttempt) {

		GotchiOwnershipResponse gr = null;
		ArrayList<Integer> tokenIDsFromTheGraphToHug = new ArrayList<Integer>();
		HashMap<Integer, Integer> kinshipTracker = new HashMap<>();
		HashMap<Integer, Gotchi> gotchiTracker = new HashMap<>();

		boolean continueWithHugs = false;
		while (!continueWithHugs) {

			// reset the gotchi batch to hug
			tokenIDsFromTheGraphToHug = new ArrayList<Integer>();
			
			// Poll the graph to get all owned gotchis
			gr = GrannyUtils.getTheGraphGotchiInfo(settings, firstAttempt);
			if (null != gr) {
				LOGGER.info("Turns out we have " + gr.getGotchisOwned().size() + " gotchis to take care of ..");

				ArrayList<Gotchi> summaryHugsNeeded = new ArrayList<Gotchi>();
				ArrayList<Gotchi> summaryHugsSoonNeeded = new ArrayList<Gotchi>();
				ArrayList<Gotchi> summaryHugsNotNeeded = new ArrayList<Gotchi>();
				Long minTimeUntilCatchUpPet = Long.MAX_VALUE;

				if (gr.getGotchisOwned().isEmpty()) {
					LOGGER.info("No gotchis to take care of .. getting back to sleep ..");
					LOGGER.info(".... sleeping " + settings.getTheGraphPollFrequencyInSeconds() + " seconds");
					SystemUtils.sleepInSeconds(settings.getTheGraphPollFrequencyInSeconds());
				} else {

					Long minTimeUntilPet = Long.MAX_VALUE;
					
					Long maxtimeUntilPet = Long.MIN_VALUE;
					for (Gotchi gotchi: gr.getGotchisOwned()) {
						int gotchiID = Integer.parseInt("" + gotchi.getId());
						Integer previousKinship = kinshipTracker.get(gotchiID);
						if (null == previousKinship) {
							previousKinship = gotchi.getKinship();
						}
						if (!previousKinship.equals(gotchi.getKinship())) {
							if (settings.isHaMode()) {
								if (settings.isSadNotification()) {
									SADUtils.blindUpdate(settings.getSadURL(), "GhostGRANNY", "OK: The kinship got updated without grannys touch ..");
									LOGGER.info("Pushing status update to SAD");
								}
								LOGGER.info("The kinship got updated without grannys touch..");
							} else {
								LOGGER.warn("Strange, the kinship got updated without grannys touch.. enable haMode if you want this warning to go away");
								LOGGER.warn(gotchi.getName() + " previousKinship: " + previousKinship + " gotchi.getKinship(): " + gotchi.getKinship());
								if (settings.isSadNotification()) {
									SADUtils.blindUpdate(settings.getSadURL(), "GhostGRANNY", "WARNING: Strange, the kinship got updated without grannys touch.. enable haMode if you want this warning to go away");
									LOGGER.info("Pushing status update to SAD");
								}
							}
						}
						kinshipTracker.put(gotchiID, gotchi.getKinship());
						gotchiTracker.put(gotchiID, gotchi);

						Long diffInSeconds = System.currentTimeMillis()/1000L - Long.parseLong("" + gotchi.getLastInteracted());
						Long timeUntilPet = (12L*3600L) - diffInSeconds;
						if (timeUntilPet > maxtimeUntilPet) maxtimeUntilPet = timeUntilPet;
						gotchi.setTimeUntilPet(timeUntilPet);
						if (minTimeUntilPet > timeUntilPet) minTimeUntilPet = timeUntilPet;

						if (settings.isForcePetAll()) {
							summaryHugsNeeded.add(gotchi);
							tokenIDsFromTheGraphToHug.add(gotchiID);
						} else {
							if (timeUntilPet < 0) {
								summaryHugsNeeded.add(gotchi);
								tokenIDsFromTheGraphToHug.add(gotchiID);
							} else if (timeUntilPet <= settings.getThresholdForGotchiToCatchUpInSeconds()) {
								if (timeUntilPet < minTimeUntilCatchUpPet) minTimeUntilCatchUpPet = timeUntilPet;
								summaryHugsSoonNeeded.add(gotchi);
							} else {
								summaryHugsNotNeeded.add(gotchi);
							}
						}

					}

					// Print summary
					LOGGER.info("ghostgranny notes: ");
					LOGGER.info("----------------------------");
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis in need of hugs", 35) + ": " + summaryHugsNeeded.size());
					for (Gotchi g: summaryHugsNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis catchups (" + settings.getThresholdForGotchiToCatchUpInSeconds() + " sec range)", 35) + ": " + summaryHugsSoonNeeded.size());
					for (Gotchi g: summaryHugsSoonNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis in no need of hugs", 35) + ": " + summaryHugsNotNeeded.size());
					for (Gotchi g: summaryHugsNotNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(" => tokenIDsFromTheGraphToHug: " + tokenIDsFromTheGraphToHug);
					LOGGER.info("----------------------------");

					if ((summaryHugsNeeded.size()>0) && (summaryHugsSoonNeeded.isEmpty())) {
						continueWithHugs = true;
					} else {
						if (!summaryHugsSoonNeeded.isEmpty()) {
							LOGGER.info(".... mini sleeping 30 seconds to let gotchis catchup");
							SystemUtils.sleepInSeconds(30);
						} else if (minTimeUntilPet <= 60L) {
							LOGGER.info(".... micro sleeping 5 seconds since we are about to pet");
							SystemUtils.sleepInSeconds(5);
						} else {
							if (settings.isSadNotification()) {
								SADUtils.blindUpdate(settings.getSadURL(), "GhostGRANNY", "HB: We still need to wait " + DateUtils.secondsToHours(maxtimeUntilPet) + " h (" + maxtimeUntilPet + " seconds) before next pet");
								LOGGER.info("Pushing status update to SAD");
							}
							LOGGER.info(".... sleeping " + settings.getTheGraphPollFrequencyInSeconds() + " seconds");
							SystemUtils.sleepInSeconds(settings.getTheGraphPollFrequencyInSeconds());
						}
					}
				}
			} else {
				LOGGER.warn("Unable to get theGraph info about owned gotchis, sleeping 10 seconds and trying again");
				SystemUtils.sleepInSeconds(10);
			}
		}
		return new GotchiGraphQLState(gr, tokenIDsFromTheGraphToHug, kinshipTracker, gotchiTracker);
	}
	
	public static GotchiGraphQLState getGotchiState(final GotchiSettings settings, boolean firstAttempt) {

		GotchiOwnershipResponse gr = null;
		ArrayList<Integer> tokenIDsFromTheGraphToHug = new ArrayList<Integer>();
		HashMap<Integer, Integer> kinshipTracker = new HashMap<>();
		HashMap<Integer, Gotchi> gotchiTracker = new HashMap<>();

		Long minTimeUntilPet = Long.MAX_VALUE;
		boolean gotGotchiState = false;
		while (!gotGotchiState) {

			// Poll the graph to get all owned gotchis
			gr = GrannyUtils.getTheGraphGotchiInfo(settings, firstAttempt);
			if (null != gr) {
				LOGGER.info("Turns out we have " + gr.getGotchisOwned().size() + " gotchis to take care of ..");

				ArrayList<Gotchi> summaryHugsNeeded = new ArrayList<Gotchi>();
				ArrayList<Gotchi> summaryHugsSoonNeeded = new ArrayList<Gotchi>();
				ArrayList<Gotchi> summaryHugsNotNeeded = new ArrayList<Gotchi>();
				Long minTimeUntilCatchUpPet = Long.MAX_VALUE;

				if (gr.getGotchisOwned().isEmpty()) {
					LOGGER.info("No gotchis to take care of .. getting back to sleep ..");
					LOGGER.info(".... sleeping " + settings.getTheGraphPollFrequencyInSeconds() + " seconds");
					SystemUtils.sleepInSeconds(settings.getTheGraphPollFrequencyInSeconds());
				} else {

					for (Gotchi gotchi: gr.getGotchisOwned()) {
						int gotchiID = Integer.parseInt("" + gotchi.getId());
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
						gotchi.setTimeUntilPet(timeUntilPet);
						if (minTimeUntilPet > timeUntilPet) minTimeUntilPet = timeUntilPet;

						if (timeUntilPet < 0) {
							summaryHugsNeeded.add(gotchi);
							tokenIDsFromTheGraphToHug.add(gotchiID);
						} else if (timeUntilPet <= settings.getThresholdForGotchiToCatchUpInSeconds()) {
							if (timeUntilPet < minTimeUntilCatchUpPet) minTimeUntilCatchUpPet = timeUntilPet;
							summaryHugsSoonNeeded.add(gotchi);
						} else {
							summaryHugsNotNeeded.add(gotchi);
						}
					}

					// Print summary
					LOGGER.info("ghostgranny notes: ");
					LOGGER.info("----------------------------");
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis in need of hugs", 35) + ": " + summaryHugsNeeded.size());
					for (Gotchi g: summaryHugsNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis catchups (" + settings.getThresholdForGotchiToCatchUpInSeconds() + " sec range)", 35) + ": " + summaryHugsSoonNeeded.size());
					for (Gotchi g: summaryHugsSoonNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(StringsUtils.cutAndPadStringToN("Gotchis in no need of hugs", 35) + ": " + summaryHugsNotNeeded.size());
					for (Gotchi g: summaryHugsNotNeeded) {
						LOGGER.info(" * " + GrannyUtils.createGotchiLogSummary(g));
					}
					LOGGER.info(" => tokenIDsFromTheGraphToHug: " + tokenIDsFromTheGraphToHug);
					LOGGER.info("----------------------------");

					gotGotchiState = true;
				}
			} else {
				LOGGER.warn("Unable to get theGraph info about owned gotchis, sleeping 10 seconds and trying again");
				SystemUtils.sleepInSeconds(10);
			}
		}
		return new GotchiGraphQLState(gr, tokenIDsFromTheGraphToHug, kinshipTracker, gotchiTracker);
	}


}
