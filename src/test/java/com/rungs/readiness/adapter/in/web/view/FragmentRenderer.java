package com.rungs.readiness.adapter.in.web.view;

import java.util.Map;
import java.util.Set;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.thymeleaf.context.Context;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.linkbuilder.StandardLinkBuilder;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Renders Thymeleaf markup that calls fragments from {@code templates/}, without a Spring context, and parses the
 * result with Jsoup. Tests call fragments the way screens do, for example
 * {@code <div th:replace="~{fragments/button :: button(label=${label})}"></div>}.
 */
final class FragmentRenderer {

    private static final SpringTemplateEngine ENGINE = createEngine();

    private FragmentRenderer() {
    }

    /** Renders {@code markup} with {@code variables} and returns the parsed body fragment. */
    static Document render(String markup, Map<String, ?> variables) {
        Context context = new Context();
        variables.forEach(context::setVariable);
        return Jsoup.parseBodyFragment(ENGINE.process(markup, context));
    }

    private static SpringTemplateEngine createEngine() {
        ClassLoaderTemplateResolver classpath = new ClassLoaderTemplateResolver();
        classpath.setPrefix("templates/");
        classpath.setSuffix(".html");
        classpath.setTemplateMode(TemplateMode.HTML);
        classpath.setCharacterEncoding("UTF-8");
        classpath.setCheckExistence(true);
        classpath.setCacheable(false);
        classpath.setOrder(1);

        StringTemplateResolver inline = new StringTemplateResolver();
        inline.setTemplateMode(TemplateMode.HTML);
        inline.setOrder(2);

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolvers(Set.of(classpath, inline));
        engine.setLinkBuilder(new StandardLinkBuilder() {
            @Override
            protected String computeContextPath(IExpressionContext context, String base, Map<String, Object> parameters) {
                // Outside a web request there is no servlet context; render context-relative links from the root.
                return "";
            }
        });
        return engine;
    }
}
