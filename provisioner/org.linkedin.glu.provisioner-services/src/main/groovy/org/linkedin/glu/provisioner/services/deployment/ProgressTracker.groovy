/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.linkedin.glu.provisioner.services.deployment

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.FilteredPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IPlanExecution
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus

/**
 * @author yan@pongasoft.com */
class ProgressTracker<T> extends FilteredPlanExecutionProgressTracker<T>
{
  final DeploymentStorage _deploymentStorage
  def final _deploymentId
  private IPlanExecution _planExecution
  private final SystemModel _system

  def ProgressTracker(DeploymentStorage deploymentStorage,
                      tracker,
                      deploymentId,
                      SystemModel system)
  {
    super(tracker)
    _deploymentStorage = deploymentStorage
    _deploymentId = deploymentId
    _system = system
  }

  public void onPlanStart(IPlanExecution<T> planExecution)
  {
    super.onPlanStart(planExecution)
    _planExecution = planExecution
  }


  public void onPlanEnd(IStepCompletionStatus<T> status)
  {
    super.onPlanEnd(status)
    String details = _planExecution.toXml([fabric: _system.fabric, systemId: _system.id])
    _deploymentStorage.endDeployment(_deploymentId, status, details)
  }
}
