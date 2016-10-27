package com.gavagai.logonaut;

import java.io.Serializable;
import java.util.Arrays;

public class TernaryCompactShortVector implements Serializable {

	private static final long serialVersionUID = 1L;
	private short[] array;
	private int size;

	public TernaryCompactShortVector() {

	}

	public TernaryCompactShortVector(int size, short[] array) {
		this.size = size;
		this.array = array;
	}

	public short[] getArray() {
		return array;
	}

	public void setArray(short[] array) {
		this.array = array;
	}

	public short[] toFullArray() {
		short[] fullArray = new short[size];
		for (int index : array) {
			fullArray[Math.abs(index) - 1] = (short) ((index < 0) ? -1 : 1);
		}
		return fullArray;

	}

	@Override
	public String toString() {
		return "TernaryCompactShortVector [array=" + Arrays.toString(array) + ", size=" + size
				+ "]";
	}
}