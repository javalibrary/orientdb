/*
      *
      *  *  Copyright 2014 Orient Technologies LTD (info(at)orientechnologies.com)
      *  *
      *  *  Licensed under the Apache License, Version 2.0 (the "License");
      *  *  you may not use this file except in compliance with the License.
      *  *  You may obtain a copy of the License at
      *  *
      *  *       http://www.apache.org/licenses/LICENSE-2.0
      *  *
      *  *  Unless required by applicable law or agreed to in writing, software
      *  *  distributed under the License is distributed on an "AS IS" BASIS,
      *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      *  *  See the License for the specific language governing permissions and
      *  *  limitations under the License.
      *  *
      *  * For more information: http://www.orientechnologies.com
      *
      */
package com.orientechnologies.orient.server.distributed;

import java.io.Externalizable;

import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.server.distributed.task.ORemoteTask;

/**
 *
 * @author Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 */
public interface ODistributedRequest extends Externalizable {
  enum EXECUTION_MODE {
    RESPONSE, NO_RESPONSE
  }

  long getId();

  void setId(long iId);

  EXECUTION_MODE getExecutionMode();

  String getDatabaseName();

  ODistributedRequest setDatabaseName(final String databaseName);

  int getSenderNodeId();

  ODistributedRequest setSenderNodeId(int localNodeId);

  ORemoteTask getTask();

  ODistributedRequest setTask(final ORemoteTask payload);

  ORID getUserRID();

  void setUserRID(ORID iUserRID);
}
