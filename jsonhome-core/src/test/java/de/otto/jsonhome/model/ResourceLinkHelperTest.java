package de.otto.jsonhome.model;

import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;

import static de.otto.jsonhome.fixtures.LinkFixtures.*;
import static de.otto.jsonhome.model.ResourceLinkHelper.mergeResources;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;

/**
 * @author Guido Steinacker
 * @since 27.12.12
 */
public class ResourceLinkHelperTest {

    @Test
    public void mergingIdenticalLinksShouldResultInSingleLink() throws Exception {
        // when
        final List<? extends ResourceLink> resourceLinks = mergeResources(
                asList(STOREFRONT_LINK),
                STOREFRONT_LINK
        );
        // then
        assertEquals(resourceLinks, asList(STOREFRONT_LINK));
    }

    @Test
    public void mergingDifferentLinksShouldResultInMergedLinks() throws Exception {
        // when
        final List<? extends ResourceLink> resourceLinks = mergeResources(
                asList(STOREFRONT_LINK),
                ABOUTPAGE_LINK
        );
        // then
        assertEquals(resourceLinks, asList(STOREFRONT_LINK, ABOUTPAGE_LINK));
    }

    @Test
    public void mergingListWithIdenticalLinksShouldResultInSingleLink() throws Exception {
        // when
        final List<? extends ResourceLink> resourceLinks = mergeResources(
                asList(STOREFRONT_LINK),
                asList(STOREFRONT_LINK)
        );
        // then
        assertEquals(resourceLinks, asList(STOREFRONT_LINK));
    }

    @Test
    public void mergingListsWithDifferentLinksShouldResultInMergedLinks() throws Exception {
        // when
        final List<? extends ResourceLink> resourceLinks = mergeResources(
                asList(STOREFRONT_LINK, ABOUTPAGE_LINK),
                asList(SHOPPAGES_LINK)
        );
        // then
        assertEquals(resourceLinks, asList(STOREFRONT_LINK, ABOUTPAGE_LINK, SHOPPAGES_LINK));
    }

    @Test
    public void mergingEmptyListWithLinksShouldResultInLinks() throws Exception {
        // when
        final List<? extends ResourceLink> resourceLinks = mergeResources(
                asList(STOREFRONT_LINK),
                Collections.<ResourceLink>emptyList()
        );
        // then
        assertEquals(resourceLinks, asList(STOREFRONT_LINK));
    }
}
