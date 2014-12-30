/*
 * Copyright (c) 2014, Adel Noureddine.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License v3.0
 * which accompanies this distribution, and is available at
 * https://www.gnu.org/licenses/agpl-3.0.html
 *
 * Author : Adel Noureddine
 */

package jolinar.sensors.memory;

public interface MemorySensorsInterface {

	/**
	 * Get the percentage of memory resident usage used by pid
	 * @return The percentage of memory resident usage used by pid
	 */
	double getProcesMemoryPercentage();

}