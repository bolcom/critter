package com.philemonworks.critter.ui;

import com.philemonworks.critter.TrafficManager;
import com.philemonworks.critter.action.Delay;
import com.philemonworks.critter.action.Forward;
import com.philemonworks.critter.action.Respond;
import com.philemonworks.critter.condition.Host;
import com.philemonworks.critter.rule.Rule;
import com.philemonworks.critter.rule.RuleConverter;
import com.philemonworks.critter.ui.fixed.EditFixedResponsePage;
import com.philemonworks.critter.ui.fixed.FixedResponseBuilder;
import org.apache.commons.lang3.StringUtils;
import org.rendershark.http.HttpServer;
import org.rendersnake.HtmlCanvas;
import org.rendersnake.StringResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Properties;

@Path("/ui")
public class AdminUIResource {
    private static final Logger LOG = LoggerFactory.getLogger(AdminUIResource.class);

	@Inject TrafficManager trafficManager;
	@Inject @Named("Proxy") HttpServer proxyServer;

	@GET
	@Path("/newrule")
	@Produces("text/html")
	public Response newRule() throws IOException {
		HtmlCanvas html = new HtmlCanvas();
		html.getPageContext()
		    .withString("rulexml", StringResource.get(("/newrule.xml")))
		    .withBoolean("proxy.started", this.proxyServer.isStarted());
		html.render(new SiteLayout(new NewRulePage()));
		return Response.ok().entity(html.toHtml()).build();
	}

    @GET
    @Path("/newdelay")
    @Produces("text/html")
    public Response newDelay() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.getPageContext()
            .withObject("rule", new Rule())
            .withBoolean("proxy.started", this.proxyServer.isStarted());
        html.render(new SiteLayout(new EditDelayPage()));
        return Response.ok().entity(html.toHtml()).build();
    }

    @GET
    @Path("/newresponse")
    @Produces("text/html")
    public Response newFixedResponse() throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.getPageContext()
            .withObject("rule", new Rule())
            .withBoolean("proxy.started", this.proxyServer.isStarted());
        html.render(new SiteLayout(new EditFixedResponsePage()));
        return Response.ok().entity(html.toHtml()).build();
    }

	@POST
	@Path("/toggleproxy")
	public Response toggleProxyActivation() throws Exception {
	    if (this.proxyServer.isStarted()) {
	        this.proxyServer.shutDown();
	    } else {
	        this.proxyServer.startUp();
	    }
	    return Response.seeOther(new URI("/")).build();
	}

	@POST
	@Path("/newrule")
	@Produces("text/html")
	public Response saveRule(InputStream input) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		String decoded = URLDecoder.decode(reader.readLine(),"utf8"); // Despite the name, this utility class is for HTML form decoding
		int eq = decoded.indexOf('=');
		String rulexml = null;
		try {
			rulexml = decoded.substring(eq+1);
			Rule rule = (Rule)RuleConverter.fromXml(rulexml, true);
            saveRuleIfItDoesNotExist(rule);
        } catch (Exception ex) {
            LOG.error("new rule contains errors:", ex);
			HtmlCanvas html = new HtmlCanvas();
			html.getPageContext().withString("alert","This definition is not valid, please correct:<br>" + ex.getMessage());
			html.getPageContext().withString("rulexml",rulexml);
			html.render(new SiteLayout(new NewRulePage()));
			return Response.ok().entity(html.toHtml()).build();
		}
		return Response.seeOther(new URI("/")).build();
	}

    private void saveRuleIfItDoesNotExist(Rule rule) {
        rule.ensureId();
        if (this.trafficManager.getRule(rule.id) == null) {
            this.trafficManager.addOrReplaceRule(rule);
        } else {
            throw new IllegalArgumentException(String.format("A rule with ID %s already exists.", rule.id));
        }
    }

    @POST
	@Path("/newresponse")
	@Produces("text/html")
	public Response saveFixedResponse(InputStream input) throws Exception {
	    BufferedReader reader = new BufferedReader(new InputStreamReader(input));
	    String decoded = URLDecoder.decode(reader.readLine(),"utf8"); // Despite the name, this utility class is for HTML form decoding
	    try {
	        FixedResponseBuilder formDecoder = new FixedResponseBuilder();
            Rule rule = formDecoder.buildRuleFrom(EditFixedResponsePage.toInput(EditFixedResponsePage.decode(decoded)));
            saveRuleIfItDoesNotExist(rule);
        } catch (Exception ex) {
            LOG.error("save fixed response failed", ex);
            HtmlCanvas html = new HtmlCanvas();
            html.getPageContext().withString("alert", "This definition is not valid, please correct. " + ex.getMessage());
            html.render(new SiteLayout(new EditFixedResponsePage()));
            return Response.ok().entity(html.toHtml()).build();
        }
	    return Response.seeOther(new URI("/")).build();
	}

    @POST
    @Path("/newdelay")
    @Produces("text/html")
    public Response saveDelay(InputStream input) throws Exception {
        try {
            // TODO put this in util
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String decoded = URLDecoder.decode(reader.readLine(),"utf8"); // Despite the name, this utility class is for HTML form decoding
            Properties props = new Properties();
            for (String keyvalue : decoded.split("&")) {
                String[] pair = keyvalue.split("=");
                props.put(pair[0], pair[1]);
            }
            Rule rule = new Rule();
            rule.id = props.getProperty("critter_id");

            URL url = new URL(props.getProperty("critter_url"));

            Host host = new Host();
            host.matches = url.getHost();
            rule.getConditions().add(host);

            if (!StringUtils.isEmpty(url.getPath()) && !"/".matches(url.getPath())) {
                com.philemonworks.critter.condition.Path path = new com.philemonworks.critter.condition.Path();
                path.matches = url.getPath();
                rule.getConditions().add(path);
            }

            Delay delay = new Delay();
            delay.milliSeconds = Long.parseLong(props.getProperty("critter_delay"));
            rule.getActions().add(delay);

            rule.getActions().add(new Forward());
            rule.getActions().add(new Respond());

            saveRuleIfItDoesNotExist(rule);
        } catch (Exception ex) {
            LOG.error("save new delay failed", ex);
            HtmlCanvas html = new HtmlCanvas();
            html.getPageContext().withString("alert","This definition is not valid, please correct.");
            html.render(new SiteLayout(new EditDelayPage()));
            return Response.ok().entity(html.toHtml()).build();
        }
        return Response.seeOther(new URI("/")).build();
    }

    @GET
    @Path("/traffic.css")
    @Produces("text/css")
    public Response trafficCss() {
        return Response.ok().entity(this.getClass().getResourceAsStream("/traffic.css")).build();
    }

    @GET
    @Path("/traffic.js")
    @Produces("text/plain")
    public Response trafficJs() {
        return Response.ok().entity(this.getClass().getResourceAsStream("/traffic.js")).build();
    }

    @GET
    @Path("/rules/{id}")
    @Produces("text/html")
    public Response showRule(@PathParam("id") String id) throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        html.getPageContext()
            .withObject("rule", trafficManager.getRule(id))
            .withBoolean("proxy.started", this.proxyServer.isStarted());
        html.render(new SiteLayout(new RulePage()));
        return Response.ok().entity(html.toHtml()).build();
    }

    @GET
    @Path("/rules/{id}/edit")
    @Produces("text/html")
    public Response editRule(@PathParam("id") String id) throws IOException {
        HtmlCanvas html = new HtmlCanvas();
        Rule rule = trafficManager.getRule(id);
        html.getPageContext()
            .withObject("rule", rule)
            .withObject("id", id)
            .withString("rulexml", RuleConverter.toXml(rule))
            .withBoolean("proxy.started", this.proxyServer.isStarted());
        html.render(new SiteLayout(new EditRulePage()));
        return Response.ok().entity(html.toHtml()).build();
    }

    @POST
    @Path("/rules/{id}/edit")
    @Produces("text/html")
    public Response saveRuleAfterEdit(@PathParam("id") String id, InputStream input) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String decoded = URLDecoder.decode(reader.readLine(), "utf8"); // Despite the name, this utility class is for HTML form decoding
        int eq = decoded.indexOf('=');
        String rulexml = null;
        try {
            rulexml = decoded.substring(eq+1);
            Rule rule = (Rule)RuleConverter.fromXml(new ByteArrayInputStream(rulexml.getBytes()));
            this.trafficManager.addOrReplaceRule(rule);
        } catch (Exception ex) {
            HtmlCanvas html = new HtmlCanvas();
            html.getPageContext().withString("alert","This definition is not valid, please correct.");
            html.getPageContext().withString("rulexml",rulexml);
            html.getPageContext().withString("id", id);
            html.render(new SiteLayout(new EditRulePage()));
            return Response.ok().entity(html.toHtml()).build();
        }
        return Response.seeOther(new URI("/")).build();
    }
}
