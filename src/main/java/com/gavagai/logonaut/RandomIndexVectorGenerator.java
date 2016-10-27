package com.gavagai.logonaut;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;

import com.gavagai.rabbit.api.monitoring.domain.WordSpace;

/**
 * A random index vector generator for mock testing purposes. 
 * A calque of the real thing in rabbit.business.
 */

public class RandomIndexVectorGenerator {

	public static final Random RANDOM = new Random();
	private static int nonZero = 6;
	private static int dimension = 1000;
	private static int frequency = 314;

	private RandomIndexVectorGenerator() {
	}

	public static TernaryCompactShortVector generate(int numberOfNonZeroElements, int dimension) {

		Set<Integer> usedPositions = new HashSet<Integer>();
		short[] randomPositions = new short[numberOfNonZeroElements];

		for (int i = 0; i < numberOfNonZeroElements; i++) {
			int index = 0;
			do {
				index = RANDOM.nextInt(dimension) + 1;
			} while (usedPositions.contains(index));

			usedPositions.add(index);

			if (i < (numberOfNonZeroElements / 2)) {
				index = index * -1;
			}

			randomPositions[i] = (short) index;

		}
		return new TernaryCompactShortVector(dimension, randomPositions);
	}

	public static short[] generateShortPositions(String term, int nonZeroElements, int dimension) {
		byte[] bytes = DigestUtils.md5(term.getBytes());
		BigInteger md5HashedTerm = new BigInteger(1, bytes);
		int[] randomPositions = new int[nonZeroElements];

		for (int i = 0; i < nonZeroElements; i++) {
			BigInteger[] divideAndRemainder = md5HashedTerm.divideAndRemainder(BigInteger
					.valueOf(dimension - i));
			md5HashedTerm = divideAndRemainder[0];
			int remainder = divideAndRemainder[1].intValue();
			int j = 0;
			for (; j < i; j++) {
				if (randomPositions[j] <= remainder) {
					remainder++;
				} else {
					break;
				}
			}
			for (int k = i; k > j; k--) {
				randomPositions[k] = randomPositions[k - 1];
			}
			randomPositions[j] = remainder;
		}

		int signedDeterminator = md5HashedTerm.mod(BigInteger.valueOf(1 << nonZeroElements))
				.intValue();

		for (int i = 0; i < nonZeroElements; i++) {
			if ((signedDeterminator & 1) == 1) {
				randomPositions[i] = -1 * (randomPositions[i] + 1);
			} else {
				randomPositions[i] = randomPositions[i] + 1;
			}
			signedDeterminator = signedDeterminator >> 1;
		}

		short[] array = new short[randomPositions.length];
		for (int i = 0; i < randomPositions.length; i++) {
			array[i] = (short) randomPositions[i];
		}
		return array;
	}

	public static TernaryCompactShortVector generate(String term, int nonZeroElements, int dimension) {
		return new TernaryCompactShortVector(dimension, generateShortPositions(term,
				nonZeroElements, dimension));
	}

	public static short[] generateShortPositions(String word) {
		return generateShortPositions(word, nonZero, dimension);
	}

	public static TernaryCompactShortVector generate(String word) {
		return generate(word, nonZero, dimension);
	}

}
