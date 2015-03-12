package com.grierforensics.danesmimeatoolset.rest

import java.net.URI
import javax.ws.rs._
import javax.ws.rs.core.Response.Status
import javax.ws.rs.core.{MediaType, Response}

import com.grierforensics.danesmimeatoolset.model.{ClickType, Workflow}
import com.grierforensics.danesmimeatoolset.service.Context

import scala.beans.BeanProperty

/** WebService resource exposing workflow functionality */
@Path("/workflow")
@Produces(Array(MediaType.APPLICATION_JSON))
class WorkflowResource {
  val emailPattern: String = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"
  val context = Context

  /** Creates and returns a new workflow as JSON. */
  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def createWorkflow(email: String): Workflow = {
    if (!email.matches(emailPattern))
      throw new WebApplicationException("Bad email:" + email, Status.INTERNAL_SERVER_ERROR)

    val result: Workflow = Workflow(email)
    context.workflowDao.persist(result)

    result.sendEmailAsync()

    result
  }


  /** Returns a workflow as JSON. */
  @GET
  @Path("{id}")
  def getWorkflow(@PathParam("id") id: String): Workflow = {
    val w: Option[Workflow] = context.workflowDao.fetch(id)
    w.getOrElse(
      throw new WebApplicationException("Workflow id not found:" + id, Status.NOT_FOUND))
  }


  /** Adds a click event and returns the workflow as JSON.
    *
    *
    * */
  @GET
  @Path("{id}/click/{clickType}")
  def click(@PathParam("id") id: String, @PathParam("clickType") token: ClickType, @QueryParam("uiRedirect") uiRedirect: Boolean): Workflow = {
    val w: Workflow = getWorkflow(id)

    token match {
      case ClickType.receivedSignedOk => w.clickedReceivedSignedOk()
      case ClickType.receivedSignedBad => w.clickedReceivedSignedBad()
      case t => throw new WebApplicationException("Unknown token " + t, Status.NOT_FOUND)
    }

    if (uiRedirect) {
      throw new WebApplicationException(Response.seeOther(new URI("/#/workflow/" + id)).build());
    }
    w
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