package com.grierforensics.danesmimeatoolset.rest

import javax.ws.rs._
import javax.ws.rs.core.MediaType

import com.grierforensics.danesmimeatoolset.model.{WorkflowDao, Workflow}

import scala.beans.BeanProperty

@Path("/workflow")
@Produces(Array(MediaType.APPLICATION_JSON))
@Consumes(Array(MediaType.APPLICATION_JSON))
class WorkflowResource {

//  @POST
//  def createWorkflow(@QueryParam("email") email: String): Workflow = {
//    val result: Workflow = Workflow(email)
//    result.sendEmail()
//    WorkflowDao.persist(result)
//    result
//  }
//
//  @GET
//  @Path("{id}")
//  def workflowStatus(@PathParam("id") id: String): Workflow = {
//    WorkflowDao.fetch(id)
//  }
  
  @GET
  @Path("echo/{echo}")
  def getEcho(@PathParam("echo") echo: String): Echo = {
    count = count + 1
    new Echo(echo, count)
  }

  var count = 0
}

class Echo(@BeanProperty val echo: String, @BeanProperty val count: Int)