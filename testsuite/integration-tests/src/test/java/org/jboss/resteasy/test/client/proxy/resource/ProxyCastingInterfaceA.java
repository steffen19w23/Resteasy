package org.jboss.resteasy.test.client.proxy.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

public interface ProxyCastingInterfaceA {
   @GET
   @Path("foo")
   @Produces("text/plain")
   String getFoo();
}
