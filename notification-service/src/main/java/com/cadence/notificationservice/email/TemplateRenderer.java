package com.cadence.notificationservice.email;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Renders DB-stored templates (authored with Figma-style {{merge_tag}}
 * notation, see V2 seed data) through the real Thymeleaf engine --
 * {{var}} is translated to Thymeleaf's inline expression syntax
 * [[${var}]] before processing, so templates stay editable in the
 * friendly notation the Figma's template modal shows while genuinely
 * using Thymeleaf to do the substitution (a plain String.replace would
 * not satisfy "Use Thymeleaf for HTML Email Templates"). Uses
 * SpringTemplateEngine (SpEL-backed SpringStandardDialect) rather than
 * the bare core TemplateEngine -- the latter defaults to StandardDialect,
 * which requires OGNL on the classpath and isn't pulled in by
 * spring-boot-starter-thymeleaf alone.
 */
@Component
public class TemplateRenderer {

    private static final Pattern MERGE_TAG = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_]+)\\s*\\}\\}");

    private final SpringTemplateEngine templateEngine;

    public TemplateRenderer() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        this.templateEngine = new SpringTemplateEngine();
        this.templateEngine.setTemplateResolver(resolver);
    }

    public String render(String templateBody, Map<String, String> variables) {
        String thymeleafSource = MERGE_TAG.matcher(templateBody).replaceAll("[[\\${$1}]]");
        Context context = new Context();
        variables.forEach(context::setVariable);
        return templateEngine.process(thymeleafSource, context);
    }
}
