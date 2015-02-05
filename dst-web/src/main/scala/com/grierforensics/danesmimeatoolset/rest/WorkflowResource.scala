package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.core.MediaType

import com.grierforensics.danesmimeatoolset.model.{Workflow, WorkflowDao}

import scala.beans.BeanProperty

@Path("/workflow")
@Produces(Array(MediaType.APPLICATION_JSON))
class WorkflowResource {

  @POST
  def createWorkflow(@FormParam("email") email: String): Workflow = {
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