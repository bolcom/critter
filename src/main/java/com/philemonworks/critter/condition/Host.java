package com.philemonworks.critter.condition;

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import com.philemonworks.critter.ProxyFilter;
import com.philemonworks.critter.action.Action;
import com.philemonworks.critter.action.RuleIngredient;
import com.philemonworks.critter.rule.RuleContext;
import com.sun.jersey.spi.container.ContainerRequest;

public class Host extends RuleIngredient implements Condition, Action {

    private static final Logger LOG = Logger.getLogger(Host.class);

	public String matches;
    public String value;
    
	@Override
	public boolean test(RuleContext ctx) {
	    String host = ctx.forwardURI.getHost();
        return matches.matches(host);
	}
	@Override
	public String explain() {
		return matches != null 
		        ? "host name matches ["+matches+"]"
		        : "change the host to ["+this.value+"]";
	}
    @Override
    public void perform(RuleContext context) {
        ContainerRequest containerRequest = (ContainerRequest) context.httpContext.getRequest();
        URI forwardUri = (URI)containerRequest.getProperties().get(ProxyFilter.UNPROXIED_URI);
        try {
            context.forwardURI = new URI(
                    forwardUri.getScheme(), 
                    forwardUri.getUserInfo(), 
                    this.value, 
                    forwardUri.getPort(),
                    forwardUri.getPath(), 
                    forwardUri.getQuery(), 
                    forwardUri.getFragment());
            if (LOG.isTraceEnabled()) {
                LOG.trace(context.forwardURI.toString());
            }
        } catch (URISyntaxException e) {
            LOG.error("perform failed",e);
        }
    }
}
