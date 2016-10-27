package com.gavagai.jussisandbox;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;

import com.gavagai.mockrabbit.Utterance;
import com.gavagai.rabbit.api.monitoring.domain.Pole;
//	import com.gavagai.rabbit.business.cache.CacheName;
//	import com.gavagai.rabbit.domain.dto.PolarizationStatistics;
//	import com.gavagai.rabbit.domain.dto.Utterance;
//import com.gavagai.rabbit.domain.entities.PolarizationResult;
//	import com.gavagai.rabbit.domain.entities.TargetMonitor;

@Stateless
public class TestaBlocker {
	// read in poles
	// read in output log
	// polarise differently
	// classify using previous classifier
	// output
	
	private final int SKIP_STEPS = 3;
	private final int AMPLIFY_STEPS = 3;


//	public List<String> generatePolarizationResults(List<String> utterances,
//			String targetMonitor, Map<String, String> context,
//			HashMap statistics) {
//
//		List<String> polarizationResults = new ArrayList<String>();
//		Date now = new Date();
//		Set<String> negations = new HashSet<String>();
//		Set<String> amplifiers = new HashSet<String>();

//		Map<Long, Pole> antonymPairsMap = new HashMap<Long, Pole>();
//		for (AntonymPair antonymPair : targetMonitor.getPoleGroup().getAntonymPairs()) {
//			antonymPairsMap.put(antonymPair.getFirstPole().getId(), antonymPair.getSecondPole());
//			antonymPairsMap.put(antonymPair.getSecondPole().getId(), antonymPair.getFirstPole());
//		}

//		List<String> biGrams = generateBiGrams(utterances);

//		for (Pole pole : targetMonitor.getPoleGroup().getPoles()) {
//
//			if (logger.isTraceEnabled()) {
//				logger.trace("Starting calculation for pole " + pole);
//			}
//
//			float poleProximity = 0;
//			int skip = 0;
//			int amplify = 0;
//
//			Set<String> poleMembers = new HashSet<String>((Set<String>) poleMembersCache.get(
//					pole.getId()).getValue());
//
//			List<String> poleBiGrams = new ArrayList<String>();
//			poleBiGrams.addAll(biGrams);
//
//			if (antonymPairsMap.get(pole.getId()) != null) {
//				Pole antonymPole = antonymPairsMap.get(pole.getId());
//				Set<String> antonymPoleMembers = (Set<String>) poleMembersCache.get(
//						antonymPole.getId()).getValue();
//				Set<String> pairPoleMembers = new HashSet<String>();
//				pairPoleMembers.addAll(poleMembers);
//				pairPoleMembers.addAll(antonymPoleMembers);
//				poleBiGrams.retainAll(pairPoleMembers);
//			} else {
//				poleBiGrams.retainAll(poleMembers);
//			}
//
//			if (!targetMonitor.isTargetTermsPolarization()) {
//				removeTargetPartsFromPoleMembers(poleMembers, targetMonitor.getTarget()
//						.getTargetPartsAsSet());
//			}
//
//			for (Utterance utterance : utterances) {
//				List<String> utteranceTermsAltered = alterSequence(utterance.getTokenizedTerms(),
//						poleBiGrams);
//				for (String utteranceTerm : utteranceTermsAltered) {
//
//					if (skip > 0) {
//						skip--;
//						if (amplifiers.contains(utteranceTerm)) {
//							amplify = AMPLIFY_STEPS;
//							skip = skip + 2;
//						}
//						continue;
//					}
//					// start negation skip if the term is a negation but only
//					// if the next bigram (including the negation) does not
//					// exist in the pole members
//					if (negations.contains(utteranceTerm)) {
//						skip = SKIP_STEPS;
//						amplify = 0;
//					} else {
//						if (amplify > 0) {
//							amplify--;
//						}
//						if (amplifiers.contains(utteranceTerm)) {
//							amplify = AMPLIFY_STEPS;
//						}
//						double utteranceTermPoleProximity = getPoleProximity(poleMembers,
//								utteranceTerm) * (float) (amplify > 0 ? 2 : 1);
//
//						poleProximity = poleProximity + (float) utteranceTermPoleProximity;
//
//						if (statistics != null) {
//							statistics.getResults().get(pole)
//							.put(utteranceTerm, (float) utteranceTermPoleProximity);
//						}
//					}
//				}
//			}
//
//			if (statistics != null) {
//				statistics.getScores().put(pole, poleProximity);
//			}
//
//			PolarizationResult polarizationResult = new PolarizationResult((float) poleProximity,
//					pole);
//			polarizationResult.setDate(now);
//			polarizationResults.add(polarizationResult);
//
//		}
//		return polarizationResults;
//	}
	protected List<String> generateBiGrams(List<Utterance> utterances) {
		List<String> expandedTerms = new ArrayList<String>();

		for (Utterance utterance : utterances) {
			String previousTerm = null;
			for (String utteranceTerm : utterance.getTokenizedTerms()) {
				if (previousTerm != null) {
					// bigram
					expandedTerms.add(previousTerm + " " + utteranceTerm);
				}
				previousTerm = utteranceTerm;
			}
		}
		return expandedTerms;
	}

	/*
	 * Return 1 if the utteranceTerm is in the poleMembers Set and not in the blockTerms Set
	 */
	private double getPoleProximity(Set<String> poleMembers, String utteranceTerm) {
		return poleMembers.contains(utteranceTerm) ? 1 : 0;
	}
}
