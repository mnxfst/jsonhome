/**
 Copyright 2012 Guido Steinacker

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package de.otto.jsonhome.registry.controller;

import de.otto.jsonhome.annotation.Doc;
import de.otto.jsonhome.annotation.Docs;
import de.otto.jsonhome.annotation.Rel;
import de.otto.jsonhome.registry.store.Registry;
import de.otto.jsonhome.registry.store.RegistryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static de.otto.jsonhome.registry.controller.RegistryConverter.*;
import static java.util.UUID.randomUUID;
import static javax.servlet.http.HttpServletResponse.*;

/**
 * Controller responsible for requests to the <code>/registry</code> resource.
 *
 * @author Guido Steinacker
 * @since 15.09.12
 */
@Controller
@RequestMapping("/registry")
@Docs({
        @Doc(rel = "/rel/jsonhome/registry",
             value = "A registry of json-home documents"),
        @Doc(rel = "/rel/jsonhome/registry-entry",
             value = "A single registry entry, referring to a json-home document.")
})
public class RegistryController {

    private static final Logger LOG = LoggerFactory.getLogger(RegistryController.class);

    private Registry registry;
    private URI applicationBaseUri;

    @Value("${jsonhome.applicationBaseUri}")
    public void setApplicationBaseUri(final String baseUri) {
        this.applicationBaseUri = URI.create(baseUri);
        LOG.info("ApplicationbaseUri is {}", applicationBaseUri.toString());
    }

    /**
     * Injects the registry implementation used to store registry entries.
     *
     * @param registry the Registry used by the controller.
     */
    @Autowired
    public void setRegistry(final Registry registry) {
        this.registry = registry;
    }

    /**
     * Returns the contents of the registry in application/json format. The returned document will look like this:
     * <code><pre>
     *     {
     *         "registry" : [
     *              {
     *                  "item" : "&lt;uri of the entry>",
     *                  "title" : "Some title",
     *                  "href" : "&lt;uri of the json-home>",
     *              },
     *              {
     *                  ...
     *              }
     *         ]
     *     }
     * </pre></code>
     * @param response HttpServletResponse with cache-control header and application/json in body.
     * @return application/json
     */
    @Rel("/rel/jsonhome/registry")
    @RequestMapping(
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Map<String, ?> getRegistry(@RequestParam(defaultValue = "")
                                      @Doc("Optionally selects an environment.")
                                      final String environment,
                                      final HttpServletResponse response) {
        LOG.info("Returning registry containing {} entries.", registry.getAllFrom(environment).size());
        response.setHeader("Cache-Control", "max-age=3600");
        return registryEntriesToMap(registry.getAllFrom(environment));
    }

    /**
     * Returns a single registry entry in application/json format. The returned document will look like this:
     * <code><pre>
     *     {
     *          "title" : "Some title",
     *          "href" : "&lt;uri of the json-home>",
     *     }
     * </pre></code>
     * @param response HttpServletResponse with cache-control header and application/json in body.
     * @return application/json
     */
    @Rel("/rel/jsonhome/registry-entry")
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.GET,
            produces = "application/json")
    @ResponseBody
    public Map<String, String> getEntry(@PathVariable
                                        @Doc("Identifier of the registry entry.")
                                        final String id,
                                        @RequestParam(defaultValue = "")
                                        @Doc("Optionally selects an environment.")
                                        final String environment,
                                        final HttpServletResponse response) {
        response.setHeader("Cache-Control", "max-age=3600");
        final URI uri = locationUri(id, environment);
        final RegistryEntry entry = registry.findBy(uri);
        if (entry == null) {
            LOG.info("Entry {} not found in registry.", uri);
            response.setStatus(SC_NOT_FOUND);
            return null;
        } else {
            LOG.info("Returning entry {}", uri);
            final Map<String, String> map = registryEntryToMap(entry);
            map.put("collection", applicationBaseUri.toString());
            return map;
        }
    }

    /**
     * Registers a new json-home document.
     * <p/>
     * If registration is successful, the 'Location' header is used to return the URI of the generated registry entry.
     * In this case, HTTP 201 created is returned.
     * <p/>
     * If the URI of the json-home document is already registered, HTTP 409 conflict is returned.
     * In this case the registry is not changed.
     * <p/>
     * The body of the request is expected to have the following attributes in json format:
     * <code><pre>
     *     {
     *          "title" : "Some title",
     *          "href" : "&lt;uri of the json-home>",
     *     }
     * </pre></code>
     * <p/>
     * HTTP status codes returned by this method:
     * <ul>
     *     <li>201 created: If the entry was successfully registered.</li>
     *     <li>400 bad request: The body of the request is not conforming to the described format.</li>
     *     <li>409 conflict: The URI of the json-home document is already registered.</li>
     * </ul>
     * @param entry application/json document, like { href="http://example.org", title="Example" }
     * @param response HttpServletResponse
     */
    @Rel("/rel/jsonhome/registry")
    @RequestMapping(
            method = RequestMethod.POST,
            consumes = "application/json")
    public void register(@RequestParam(defaultValue = "")
                         @Doc("Optionally selects an environment.")
                         final String environment,
                         @RequestBody
                         final Map<String, String> entry,
                         final HttpServletResponse response) throws IOException {
        if (isValid(entry)) {
            final URI location = locationUri(randomUUID().toString(), environment);
            entry.put("self", location.toString());
            try {
                LOG.info("Registering new registry-entry {}", location);
                registry.put(registryEntryFromMap(entry));
                response.setHeader("Location", location.toString());
                response.setStatus(SC_CREATED);
            } catch (final IllegalArgumentException e) {
                LOG.info("Entry {} is already registered with a different id", location);
                // href is already registered under different URI
                response.sendError(SC_CONFLICT, "The referred json-home is already registered with a different id.");
            }
        } else {
            LOG.info("Entry {} is not valid.", entry);
            response.sendError(SC_BAD_REQUEST, "The request does not contain a valid entry.");
        }
    }

    /**
     * Registers or updates a json-home document.
     * <p/>
     * If the home document is registered, <code>HTTP 201 created</code> is returned,
     * if updated <code>HTTP 204 no content</code>.
     * <p/>
     * If the URI of the json-home document is already registered, <code>HTTP 409 conflict</code> is returned.
     * In this case the registry is not changed.
     * <p/>
     * The body of the request is expected to have the following attributes in json format:
     * <code><pre>
     *     {
     *          "title" : "Some title",
     *          "href" : "&lt;uri of the json-home>",
     *     }
     * </pre></code>
     * Attribute <code>self</code> inside the document will be ignored.
     * <p/>
     * HTTP status codes returned by this method:
     * <ul>
     *     <li>201 created: If the entry was registered.</li>
     *     <li>204 no content: The entry was updated.</li>
     *     <li>400 bad request: The body of the request is not conforming to the described format.</li>
     *     <li>409 conflict: The URI of the json-home document is already registered.</li>
     * </ul>
     * @param entry application/json document, like { href="http://example.org", title="Example" }
     * @param response HttpServletResponse
     */
    @Rel("/rel/jsonhome/registry-entry")
    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.PUT,
            consumes = "application/json")
    public void registerOrUpdate(@PathVariable @Doc("Identifier of the registry entry.")
                                 final String id,
                                 @RequestParam(defaultValue = "")
                                 @Doc("Optionally selects an environment.")
                                 final String environment,
                                 @RequestBody
                                 final Map<String, String> entry,
                                 final HttpServletResponse response) throws IOException {
        if (isValid(entry)) {
            final URI location = locationUri(id, environment);
            entry.put("self", location.toString());
            try {
                if (registry.put(registryEntryFromMap(entry))) {
                    response.setStatus(SC_CREATED);
                } else {
                    response.setStatus(SC_NO_CONTENT);
                }

            } catch (final IllegalArgumentException e) {
                // href is already registered under different URI
                response.sendError(SC_CONFLICT, "The referred json-home is already registered with a different id.");
            }
        } else {
            response.sendError(SC_BAD_REQUEST, "The request does not contain a valid entry.");
        }
    }

    @RequestMapping(
            value = "/{id}",
            method = RequestMethod.DELETE)
    public void unregister(@PathVariable
                           @Doc("Identifier of the registry entry.")
                           final String id,
                           @RequestParam(defaultValue = "")
                           @Doc("The (optional) environment to unregister")
                           final String environment,
                           final HttpServletResponse response) {
        registry.remove(locationUri(id, environment));
        response.setStatus(SC_NO_CONTENT);
    }

    private boolean isValid(Map<String, ?> entry) {
        try {
            final String title = entry.get("title").toString();
            final URI href = URI.create(entry.get("href").toString());
            return !title.isEmpty() && href.isAbsolute();
        } catch (final Exception e) {
            return false;
        }
    }

    private URI locationUri(final String id, final String environment) {
        if (environment == null || environment.isEmpty()) {
            return URI.create(applicationBaseUri + "/registry/" + id);
        } else {
            return URI.create(applicationBaseUri + "/registry/" + id + "?environment=" + environment);
        }
    }

}