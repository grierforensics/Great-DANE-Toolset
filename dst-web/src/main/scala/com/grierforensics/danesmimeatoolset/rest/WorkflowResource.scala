// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
package com.grierforensics.danesmimeatoolset.rest

import java.net.URI
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import com.grierforensics.danesmimeatoolset.model.{ClickType, Event, Workflow}
import com.grierforensics.danesmimeatoolset.service.{Context, GensonConfig}

import scala.beans.BeanProperty

/** WebService resource exposing workflow functionality */
@Path("/workflow")
@Produces(Array(MediaType.APPLICATION_JSON))
class WorkflowResource {
  val emailPattern: String = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
  val context = Context

  private def getWorkflow(id: String): Workflow =
    context.workflowDao.fetch(id).getOrElse(
      throw new WebApplicationException("Workflow id not found:" + id, Status.NOT_FOUND)
    )

  /** Issues with GensonScala serializing the Workflow class have led to this
    *
    * @param workflow Workflow
    * @return JSON-serialized workflow
    */
  private def serializeWorkflow(workflow: Workflow): String = {
    case class Foo(id: String, email: String, certData: String, events: Seq[Event], replyTo: String, replyCert: String)
    val foo = Foo(workflow.id, workflow.emailAddress, workflow.certData,
      workflow.events, workflow.replyToAddress, workflow.replyCert)
    GensonConfig.genson.serialize(foo)
  }

  /** Creates and returns a new workflow as JSON. */
  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def createWorkflow(email: String): String = {
    if (!email.matches(emailPattern))
      throw new WebApplicationException("Bad email:" + email, Status.INTERNAL_SERVER_ERROR)

    val workflow: Workflow = new Workflow(email)
    context.workflowDao.persist(workflow)

    workflow.sendEmailAsync()

    serializeWorkflow(workflow)
  }


  /** Returns a workflow as JSON. */
  @GET
  @Path("{id}")
  def retrieveWorkflow(@PathParam("id") id: String): String = {
    serializeWorkflow(getWorkflow(id))
  }


  /** Adds a click event and returns the workflow as JSON.
    *
    *
    * */
  @GET
  @Path("{id}/click/{clickType}")
  def click(@PathParam("id") id: String, @PathParam("clickType") token: ClickType, @QueryParam("uiRedirect") uiRedirect: Boolean): String = {
    val workflow: Workflow = getWorkflow(id)

    token match {
      case ClickType.receivedSignedOk => workflow.clickedReceivedSignedOk()
      case ClickType.receivedSignedBad => workflow.clickedReceivedSignedBad()
      case t => throw new WebApplicationException("Unknown token " + t, Status.NOT_FOUND)
    }

    if (uiRedirect) {
      throw new WebApplicationException(Response.seeOther(new URI("/#/workflow/" + id)).build());
    }

    serializeWorkflow(workflow)
  }


  /** Adds a click event and returns the workflow as JSON. */
  @GET
  @Path("echo/{echo}")
  def getEcho(@PathParam("echo") echo: String): Echo = {
    new Echo(echo, Count.next)
  }

}

object Count {
  private var count = 0

  def next = {
    count += 1;
    count
  }
}

class Echo(@BeanProperty val echo: String, @BeanProperty val count: Int)