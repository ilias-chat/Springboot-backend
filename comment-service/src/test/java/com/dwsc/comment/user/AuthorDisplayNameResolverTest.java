package com.dwsc.comment.user;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthorDisplayNameResolverTest {

    @Test
    void resolveFromProfile_prefersName() {
        assertEquals(
                "Ilias",
                AuthorDisplayNameResolver.resolveFromProfile(
                        new UserProfileResponse("Ilias", "i@test.com", "uid")));
    }

    @Test
    void resolveFromProfile_usesEmailLocalPartWhenNameMissing() {
        assertEquals(
                "ilias",
                AuthorDisplayNameResolver.resolveFromProfile(
                        new UserProfileResponse(null, "ilias@example.com", "uid")));
    }
}
