/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011 - 2015 OpenWorm.
 * http://openworm.org
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the MIT License
 * which accompanies this distribution, and is available at
 * http://opensource.org/licenses/MIT
 *
 * Contributors:
 *     	OpenWorm - http://openworm.org/people.html
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights 
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell 
 * copies of the Software, and to permit persons to whom the Software is 
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in 
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. 
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE 
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package org.geppetto.simulation;

import java.util.ArrayList;
import java.util.List;

import org.geppetto.core.conversion.IConversion;
import org.geppetto.core.data.IGeppettoDataManager;
import org.geppetto.core.data.model.IAspectConfiguration;
import org.geppetto.core.data.model.IExperiment;
import org.geppetto.core.data.model.ISimulatorConfiguration;
import org.geppetto.core.simulator.ISimulator;

public class ExperimentRun
{

	private IGeppettoDataManager dataManager;

	private IExperiment experiment;

	private List<ISimulator> simulatorServices = new ArrayList<>();

	private List<IConversion> conversionServices = new ArrayList<>();

	public ExperimentRun(IGeppettoDataManager dataManager, IExperiment experiment)
	{
		this.dataManager = dataManager;
		this.experiment = experiment;
		init(experiment);
	}

	private void init(IExperiment experiment)
	{
		List<? extends IAspectConfiguration> aspectConfigs = experiment.getAspectConfigurations();
		for(IAspectConfiguration aspectConfig : aspectConfigs)
		{
			ISimulatorConfiguration simConfig = aspectConfig.getSimulatorConfiguration();
			// TODO: copy from CreateSimulationServices.visit()
			// TODO: copy from LoadSimulationVisitor.visit()
			// TODO: IModel,... see the diagram

		}
	}

	protected void run()
	{

	}

}
