package com.grierforensics.danesmimeatoolset.rest

import java.net.{URI, URISyntaxException}
import javax.ws.rs._
import javax.ws.rs.core.{Response, MediaType}
import javax.ws.rs.core.Response.Status

import com.grierforensics.danesmimeatoolset.model.{Workflow, WorkflowDao}

import scala.beans.BeanProperty

@Path("/workflow")
@Produces(Array(MediaType.APPLICATION_JSON))
class WorkflowResource {

  @POST
  @Consumes(Array(MediaType.APPLICATION_JSON))
  def createWorkflow(email: String): Workflow = {
    val result: Workflow = Workflow(email)
    result.sendEmail()
    WorkflowDao.persist(result)
    result
  }

  @GET
  @Path("{id}")
  def workflowStatus(@PathParam("id") id: String): Workflow = {
    WorkflowDao.fetch(id)
  }

  @GET
  @Path("{id}/click/{token}")
  def click(@PathParam("id") id: String, @PathParam("token") token: String, @QueryParam("uiRedirect") uiRedirect:Boolean): Workflow = {
    val w: Workflow = WorkflowDao.fetch(id)
    if(w==null)
      throw new WebApplicationException("",Status.NOT_FOUND)

    token match {
      case "receivedSignedOk"=> w.receivedSignedOk()
      case "receivedSignedBad"=> w.receivedSignedBad()
      case t => throw new WebApplicationException("Unknown token "+t,Status.NOT_FOUND)
    }

    if(uiRedirect){
        throw new WebApplicationException(Response.seeOther(new URI("/#/workflow/"+id)).build());
    }
    w
  }

  @GET
  @Path("echo/{echo}")
  def getEcho(@PathParam("echo") echo: String): Echo = {
    new Echo(echo, Count.next)
  }
}

object Count {
  private var count = 0

  def next = {count += 1; count}
}

class Echo(@BeanProperty val echo: String, @BeanProperty val count: Int)