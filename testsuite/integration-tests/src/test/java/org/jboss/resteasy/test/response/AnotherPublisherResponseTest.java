package org.jboss.resteasy.test.response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.sse.SseEventSource;


import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.resteasy.category.Jaxrs21;
import org.jboss.resteasy.test.response.resource.AsyncResponseCallback;
import org.jboss.resteasy.test.response.resource.AsyncResponseException;
import org.jboss.resteasy.test.response.resource.AsyncResponseExceptionMapper;
import org.jboss.resteasy.test.response.resource.PublisherResponseResource;
import org.jboss.resteasy.utils.PortProviderUtil;
import org.jboss.resteasy.utils.TestUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @tpSubChapter Publisher response type
 * @tpChapter Integration tests
 * @tpSince RESTEasy 3.5
 */
@RunWith(Arquillian.class)
@Category(Jaxrs21.class)
public class AnotherPublisherResponseTest {
   private final static Logger logger = Logger.getLogger(AnotherPublisherResponseTest.class);

   @Deployment
   public static Archive<?> deploy() {
      WebArchive war = TestUtil.prepareArchive(AnotherPublisherResponseTest.class.getSimpleName());
      war.addClass(AnotherPublisherResponseTest.class);
      war.addClass(Jaxrs21.class);
      war.addAsLibrary(TestUtil.resolveDependency("io.reactivex.rxjava2:rxjava:2.1.3"));
      war.setManifest(new StringAsset("Manifest-Version: 1.0\n"
              + "Dependencies: org.reactivestreams\n"));
      return TestUtil.finishContainerPrepare(war, null, PublisherResponseResource.class,
            AsyncResponseCallback.class, AsyncResponseExceptionMapper.class, AsyncResponseException.class, PortProviderUtil.class);
   }

   private String generateURL(String path) {
      return PortProviderUtil.generateURL(path, AnotherPublisherResponseTest.class.getSimpleName());
   }

   /**
    * @tpTestDetails Resource method returns Publisher<String>.
    * @tpSince RESTEasy 3.5
    */
   @Test
   public void testSse() throws Exception
   {
      for (int i=0; i < 40; i++) {
         internalTestSse(i);
      }
   }
   public void internalTestSse(int i) throws Exception
   {
      Client client = ClientBuilder.newClient();
      WebTarget target = client.target(generateURL("/sse"));
      List<String> collector = new ArrayList<>();
      List<Throwable> errors = new ArrayList<>();
      CompletableFuture<Void> future = new CompletableFuture<Void>();
      try (SseEventSource source = SseEventSource.target(target).build())
      {
         source.register(evt -> {
            String data = evt.readData(String.class);
            collector.add(data);
            if (collector.size() >= 2)
            {
               future.complete(null);
            }
         }, t -> {
            logger.error(t.getMessage(), t);
            errors.add(t);
         }, () -> {
            // bah, never called
            future.complete(null);
         });
         source.open();
         future.get(5000, TimeUnit.SECONDS);
         Assert.assertEquals(2, collector.size());
         Assert.assertEquals(0, errors.size());
         Assert.assertTrue(collector.contains("one"));
         Assert.assertTrue(collector.contains("two"));
      }
      client.close();
   }
}