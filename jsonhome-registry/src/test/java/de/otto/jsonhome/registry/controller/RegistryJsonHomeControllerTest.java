package de.otto.jsonhome.registry.controller;

import de.otto.jsonhome.model.JsonHome;
import de.otto.jsonhome.registry.store.RegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static de.otto.jsonhome.model.DirectLink.directLink;
import static de.otto.jsonhome.model.Hints.emptyHints;
import static de.otto.jsonhome.model.JsonHome.jsonHome;
import static de.otto.jsonhome.registry.fixture.RegistryFixture.registryLiveWithSingleLinkTo;
import static java.net.URI.create;
import static org.testng.Assert.*;

/**
 * @author Guido Steinacker
 * @since 04.01.13
 */
@ContextConfiguration(locations = "classpath:/testSpringContext.xml")
public class RegistryJsonHomeControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private RegistriesController registriesController;

    @Autowired
    private RegistryRepository registries;

    @BeforeMethod
    public void beforeMethod() {
        registries.clear();
    }

    @Test
    public void shouldReturnJsonHome() throws IOException {
        // given:
        final RegistryJsonHomeController jsonHomeController = new RegistryJsonHomeController();
        jsonHomeController.setJsonHomeSource(getJsonHomeSource());
        registriesController.putRegistry("test", registryLiveWithSingleLinkTo("foo"), new MockHttpServletResponse());
        // when:
        final Map<String, ?> json = jsonHomeController.getAsApplicationJson("test", new MockHttpServletResponse());
        // then:
        assertNotNull(json);
        assertTrue(json.containsKey("resources"));
        final Object resources = getFrom(json, "resources");
        assertEquals(((Map)resources).size(), 1);
        final Object resource = getFrom(resources, "http://example.org/rel/foo");
        assertNotNull(resource);
        final Object href = getFrom(resource, "href");
        assertEquals(href, "http://example.org/fooResource");
    }

    private JsonHomeEnvSource getJsonHomeSource() {
        return new JsonHomeEnvSource() {
            @Override
            public JsonHome getJsonHome(final String environment) {
                return jsonHome(
                        directLink(
                                create("http://example.org/rel/foo"),
                                create("http://example.org/fooResource"),
                                emptyHints()
                        )
                );
            }
        };
    }

    private Object getFrom(final Object map, final String key) {
        return ((Map)map).get(key);
    }
}
