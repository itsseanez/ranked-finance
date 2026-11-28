package com.rankedfinance.user;

import java.util.List;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Lets every controller take a {@link CurrentUser} parameter. */
@Configuration
class CurrentUserWebConfig implements WebMvcConfigurer {

    static {
        // Filled from the session, not the request, so it isn't a parameter in the OpenAPI spec.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(CurrentUser.class);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
