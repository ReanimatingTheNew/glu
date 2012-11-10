/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.agent.rest.common

import org.linkedin.groovy.util.rest.RestException
import org.restlet.data.Status
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.groovy.utils.rest.GluGroovyRestUtils
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils

/**
 * @author yan@pongasoft.com */
public class AgentRestUtils
{
  public static final String MODULE = AgentRestUtils.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  public static AgentException throwAgentException(Status status, RestException restException)
  {
    Throwable exception = rebuildAgentException(restException)

    if(exception instanceof AgentException)
    {
      throw exception
    }
    else
    {
      throw new AgentException(status.toString(), restException)
    }
  }

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  public static Throwable rebuildAgentException(RestException restException)
  {
    GluGroovyRestUtils.rebuildException(restException)
  }

  /**
   * Demultiplexes the exec stream as generated by
   * {@link org.linkedin.glu.agent.api.Shell#exec(Map)} when <code>args.res</code> is
   * <code>stream</code>. The following is equivalent:
   *
   * OutputStream myStdout = ...
   * OutputStream myStderr = ...
   *
   * exec(command: xxx, stdout: myStdout, stderr: myStderr, res: exitValue)
   *
   * is 100% equivalent to:
   *
   * demultiplexExecStream(exec(command: xxx, res: stream), myStdout, myStderr)
   *
   * @param execStream the stream as generated by {@link org.linkedin.glu.agent.api.Shell#exec(Map)}
   * @param stdout the stream to write the output (optional, can be <code>null</code>)
   * @param stderr the stream to write the error (optional, can be <code>null</code>)
   * @return the value returned by the executed subprocess
   * @throws org.linkedin.glu.agent.api.ShellExecException when there was an error executing the
   *         shell script and <code>args.failOnError</code> was set to <code>true</code>
   */
  public static def demultiplexExecStream(InputStream execStream,
                                          OutputStream stdout,
                                          OutputStream stderr)
  {
    def exitValueStream = new ByteArrayOutputStream()
    def exitErrorStream = new ByteArrayOutputStream()

    def streams = [:]

    streams[StreamType.STDOUT.multiplexName] = stdout ?: NullOutputStream.INSTANCE
    streams[StreamType.STDERR.multiplexName] = stderr ?: NullOutputStream.INSTANCE
    streams[StreamType.EXIT_VALUE.multiplexName] = exitValueStream
    streams[StreamType.EXIT_ERROR.multiplexName] = exitErrorStream

    // we demultiplex the stream
    MultiplexedInputStream.demultiplex(execStream, streams)

    // it means we got an exception, we throw it back
    if(exitErrorStream.size() > 0)
    {
      throw GluGroovyJsonUtils.rebuildException(new String(exitErrorStream.toByteArray(), "UTF-8"))
    }
    else
    {
      if(exitValueStream.size() == 0)
        return null

      String exitValueAsString = new String(exitValueStream.toByteArray(), "UTF-8")
      try
      {
        return Integer.valueOf(exitValueAsString)
      }
      catch(NumberFormatException e)
      {
        // this should not really happen but just in case...
        return exitValueAsString
      }
    }
  }
}