/*
 * Copyright (c) 2014, Adel Noureddine.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License v3.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Author : Adel Noureddine
 */

package jolinar.formulas.memory;

import jolinar.sensors.memory.MemorySensorsInterface;

public class MemoryFormulaProc implements MemoryFormulasInterface {

	/**
	 * Memory read and write power
	 * Data from configuration file (from hardware specifications)
	 */
	private Double memoryReadPower, memoryWritePower, memoryPower;

	/**
	 * Memory sensor
	 */
	private MemorySensorsInterface memorySensor;

	/**
	 * Constructor
	 * @param memoryReadPower Memory read power
	 * @param memoryWritePower Memory write power
	 * @param memorySensor Memory sensor
	 */
	public MemoryFormulaProc(Double memoryReadPower, Double memoryWritePower, MemorySensorsInterface memorySensor) {
		this.memoryReadPower = memoryReadPower;
		this.memoryWritePower = memoryWritePower;
		this.memoryPower = (memoryReadPower + memoryWritePower) / 2;
		this.memorySensor = memorySensor;
	}

	@Override
	public double getMemoryPower() {
		return (this.memoryPower * this.memorySensor.getProcesMemoryPercentage());
	}

}
