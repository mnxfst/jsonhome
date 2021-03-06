package de.otto.jsonhome.generator;

import com.sun.jersey.api.uri.UriBuilderImpl;

import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URLDecoder;

/**
 * @author Sebastian Schroeder
 * @since 09.12.2012
 */
public final class JerseyResourceLinkGenerator extends ResourceLinkGenerator {

    private final URI applicationBaseUri;

    public JerseyResourceLinkGenerator(final URI applicationBaseUri,
                                       final URI relationTypeBaseUri) {
        super(applicationBaseUri,
                relationTypeBaseUri,
                null,
                new JerseyHintsGenerator(relationTypeBaseUri),
                new JerseyHrefVarsGenerator(relationTypeBaseUri));
        this.applicationBaseUri = applicationBaseUri;
    }

    /**
     * Returns true if the method is a candidate for further processing, false otherwise.
     *
     * @param method the method to check
     * @return boolean
     */
    @Override
    protected boolean isCandidateForAnalysis(final Method method) {
        for (final Annotation annotation : method.getDeclaredAnnotations()) {
            if (HttpMethods.isHttpMethod(annotation)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the resource paths for the given method.
     * <p/>
     * The resource paths are the paths of the URIs the given method is responsible for.
     *
     * @param method the method of the controller, possibly handling one or more REST resources.
     * @return resource path
     */
    @Override
    protected String resourcePathFor(final Method method) {
        if (isCandidateForAnalysis(method)) {
            final UriBuilder uriBuilder = new UriBuilderImpl()
                    .uri(applicationBaseUri)
                    .path(method.getDeclaringClass());
            if (method.getAnnotation(Path.class) != null) {
                uriBuilder.path(method);
            }
            try {
                return URLDecoder.decode(uriBuilder.build().toString(), "UTF-8") + queryTemplateFrom(method);
            } catch (UnsupportedEncodingException e) {
                //UTF-8 should be known
            }
        }
        return null;
    }

}
