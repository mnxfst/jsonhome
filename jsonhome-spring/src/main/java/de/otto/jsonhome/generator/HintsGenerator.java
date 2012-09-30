package de.otto.jsonhome.generator;

import de.otto.jsonhome.model.Hints;
import de.otto.jsonhome.model.HintsBuilder;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.*;

import static de.otto.jsonhome.model.HintsBuilder.hints;

public class HintsGenerator {

    public Hints hintsOf(final Method method) {
        final List<String> allows = allowedHttpMethodsOf(method);
        final HintsBuilder hintsBuilder = hints().allowing(allows);
        final List<String> supportedRepresentations = supportedRepresentationsOf(method);
        if (allows.contains("PUT")) {
            hintsBuilder.acceptingForPut(supportedRepresentations);
        }
        if (allows.contains("POST")) {
            hintsBuilder.acceptingForPost(supportedRepresentations);
        }
        if (allows.contains("GET") || allows.contains("HEAD")) {
            hintsBuilder.representedAs(supportedRepresentations);
        }
        // TODO: PATCH

        return hintsBuilder.build();
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of allowed http methods (GET, PUT, etc.).
     * <p/>
     * If the RequestMapping does not specify the allowed HTTP methods, "GET" is returned in a singleton list.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected List<String> allowedHttpMethodsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final List<String> allows = listOfStringsFrom(methodRequestMapping.method());
        if (allows.isEmpty()) {
            return Collections.singletonList("GET");
        } else {
            return allows;
        }
    }

    /**
     * Analyses the method with a RequestMapping and returns a list of supported representations.
     * <p/>
     * If the RequestMapping does not specify the produced or consumed representations,
     * "text/html" is returned in a singleton list.
     * <p/>
     * TODO: in case of a POST, text/html is not correct.
     *
     * @return list of allowed HTTP methods.
     * @throws NullPointerException if method is not annotated with @RequestMapping.
     */
    protected List<String> supportedRepresentationsOf(final Method method) {
        final RequestMapping methodRequestMapping = method.getAnnotation(RequestMapping.class);
        final LinkedHashSet<String> representations = new LinkedHashSet<String>();
        final String[] produces = methodRequestMapping.produces();
        if (produces != null) {
            representations.addAll(Arrays.asList(produces));
        }
        final String[] consumes = methodRequestMapping.consumes();
        if (consumes != null && consumes.length > 0) {

            // preserve order from methodRequestMapping:
            for (final String consumesRepresentation : consumes) {
                if (!representations.contains(consumesRepresentation)) {
                    representations.add(consumesRepresentation);
                }
            }
        }
        // default is HTTP GET
        if (representations.isEmpty()) {
            representations.add("text/html");
        }
        return new ArrayList<String>(representations);
    }

    private List<String> listOfStringsFrom(Object[] array) {
        final List<String> result = new ArrayList<String>(array.length);
        for (Object o : array) {
            result.add(o.toString());
        }
        return result;
    }
}