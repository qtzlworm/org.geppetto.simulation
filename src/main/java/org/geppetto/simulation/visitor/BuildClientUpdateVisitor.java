/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (c) 2011, 2013 OpenWorm.
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
package org.geppetto.simulation.visitor;

import org.geppetto.core.common.GeppettoErrorCodes;
import org.geppetto.core.common.GeppettoInitializationException;
import org.geppetto.core.model.IModelInterpreter;
import org.geppetto.core.model.ModelInterpreterException;
import org.geppetto.core.model.runtime.ACompositeNode;
import org.geppetto.core.model.runtime.ASimpleStateNode;
import org.geppetto.core.model.runtime.AspectNode;
import org.geppetto.core.model.runtime.AspectTreeNode;
import org.geppetto.core.model.runtime.CompositeVariableNode;
import org.geppetto.core.model.runtime.EntityNode;
import org.geppetto.core.model.runtime.RuntimeTreeRoot;
import org.geppetto.core.model.runtime.StateVariableNode;
import org.geppetto.core.model.runtime.AspectTreeNode.ASPECTTREE;
import org.geppetto.core.model.simulation.Aspect;
import org.geppetto.core.model.simulation.Entity;
import org.geppetto.core.model.simulation.Model;
import org.geppetto.core.model.simulation.Simulator;
import org.geppetto.core.model.state.visitors.SerializeTreeVisitor;
import org.geppetto.core.model.values.AValue;
import org.geppetto.core.simulation.ISimulationCallbackListener;
import org.geppetto.core.visualisation.model.Point;
import org.geppetto.simulation.CustomSerializer;
import org.geppetto.simulation.SessionContext;
import org.geppetto.simulation.SimulatorRuntime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.massfords.humantask.BaseVisitor;
import com.massfords.humantask.TraversingVisitor;

/**
 * @author matteocantarelli
 * 
 */
public class BuildClientUpdateVisitor extends TraversingVisitor
{

	private SessionContext _sessionContext;

	private RuntimeTreeRoot _scene = new RuntimeTreeRoot();
	
	private CompositeVariableNode _simulationStateTreeRoot = new CompositeVariableNode("variable_watch");

	private EntityNode _currentClientEntity = null;

	private ISimulationCallbackListener _simulationCallback;

	/**
	 * @param sessionContext
	 * @param _simulationCallback 
	 */
	public BuildClientUpdateVisitor(SessionContext sessionContext, ISimulationCallbackListener simulationListener)
	{
		super(new DepthFirstTraverserEntitiesFirst(), new BaseVisitor());
		_sessionContext = sessionContext;
		_simulationCallback=simulationListener;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Aspect)
	 */
	@Override
	public void visit(Aspect aspect)
	{
		Model model = aspect.getModel();
		EntityNode visualEntity = null;
		AspectNode clientAspect = new AspectNode();
		clientAspect.setId(aspect.getId());
		clientAspect.setInstancePath(aspect.getInstancePath());

		if(model != null)
		{
			try
			{
				IModelInterpreter modelInterpreter = _sessionContext.getModelInterpreter(model);
				Simulator simulator = _sessionContext.getSimulatorFromModel(model);
				AspectTreeNode stateTree = null;
				
				if(simulator!=null)
				{
					stateTree = _sessionContext.getSimulatorRuntime(simulator).getStateTree();	
				}
				

				visualEntity = modelInterpreter.getVisualEntity(_sessionContext.getIModel(model.getInstancePath()), aspect, stateTree);

				if(visualEntity.getAspects().size() == 1)
				{
					// there is only going to be one aspect inside the entity that was returned by the model interpreter
					clientAspect.getVisualModel().addAll(visualEntity.getAspects().get(0).getVisualModel());

					// we add all the entities that were added by the model interpreter
					// the model specified for an entity in the Geppetto configuration file
					// could wrap inside multiple entities
					_currentClientEntity.getChildren().addAll(visualEntity.getChildren());
				}
				else
				{
					_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "The visual entity returned by the model interpreter has more than one aspect",null);
				}

			}
			catch(GeppettoInitializationException e)
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
			}
			catch(ModelInterpreterException e)
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
			}
		}

		_currentClientEntity.getAspects().add(clientAspect);

		// Add the watch tree of this simulator to the root
		Simulator simulator = aspect.getSimulator();
		if(simulator != null)
		{
			SimulatorRuntime simulatorRuntime = _sessionContext.getSimulatorRuntime(simulator);
			simulatorRuntime.incrementStepsConsumed();
			_simulationStateTreeRoot.addChildren(simulatorRuntime.getStateTree().getSubTree(ASPECTTREE.WATCH_TREE).getChildren());

			StateVariableNode time = new StateVariableNode("time step");
			ACompositeNode timeNode = new CompositeVariableNode("time tree");
			if(!timeNode.getChildren().isEmpty())
			{
				ASimpleStateNode timeValueNode=((ASimpleStateNode) timeNode.getChildren().get(0));
				AValue timeValue = timeValueNode.consumeFirstValue();
				time.setScalingFactor(timeValueNode.getScalingFactor());
				time.setUnit(timeValueNode.getUnit());
				time.addValue(timeValue);
				clientAspect.setTime(time);
				//Note: The line below will stop working in case there will be a simulation in which the update to the frontend
				//for all the simulators will not happen at the same time step for all of them (note this doesn't 
				//mean all simulators are required to have the same time step, this is only about the moment
				//in which the updates are sent to the server
				_scene.setTime(time);

			}
			else
			{
				_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), "The simulator for " + simulator.getInstancePath() + " has no timestep information",null);
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.massfords.humantask.TraversingVisitor#visit(org.geppetto.simulation.model.Entity)
	 */
	@Override
	public void visit(Entity entity)
	{
		EntityNode beforeEntity = _currentClientEntity;

		EntityNode visualEntity = new EntityNode();
		visualEntity.setId(entity.getId());
		visualEntity.setInstancePath(entity.getInstancePath());
		if(entity.getPosition()!=null){
			Point position = new Point();
			position.setX(new Double(entity.getPosition().getX()));
			position.setY(new Double(entity.getPosition().getY()));
			position.setZ(new Double(entity.getPosition().getZ()));
			visualEntity.setPosition(position);
		}
		if(entity.getParentEntity() == null)
		{
			// this is an entity in the root of the simulation
			_scene.addChild(visualEntity);
		}
		else
		{
			_currentClientEntity.getChildren().add(visualEntity);
		}

		_currentClientEntity = visualEntity;
		super.visit(entity);
		_currentClientEntity = beforeEntity;
	}

	/**
	 * @return
	 */
	public String getSerializedScene()
	{
		// a custom serializer is used to change what precision is used when serializing doubles in the scene
		ObjectMapper mapper = new ObjectMapper();
		SimpleModule customSerializationModule = new SimpleModule("customSerializationModule");
		customSerializationModule.addSerializer(new CustomSerializer(Double.class)); // assuming serializer declares correct class to bind to
		mapper.registerModule(customSerializationModule);

		try
		{
			return mapper.writer().writeValueAsString(_scene);
		}
		catch(JsonProcessingException e)
		{
			_simulationCallback.error(GeppettoErrorCodes.SIMULATION, this.getClass().getName(), null,e);
		}
		
		return null;
	}


	/**
	 * NOTE: Currently the scene and the watch tree are separated. Theoretically the entire update could be part of the same tree. This split is an heritage of a previous implementation, not changing
	 * it yet as it's not clear if there would be a performance benefit or not
	 * 
	 * @return
	 */
	public String getSerializedWatchTree()
	{
		// serialize state tree for variable watch and store in a string
		SerializeTreeVisitor visitor = new SerializeTreeVisitor();
		_simulationStateTreeRoot.apply(visitor);
		return visitor.getSerializedTree();
	}

}
